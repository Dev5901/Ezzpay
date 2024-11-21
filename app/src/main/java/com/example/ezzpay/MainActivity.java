package com.example.ezzpay;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private Button addWalletButton, sendButton, receiveButton;
    private View cardView;
    private TextView usernameTextView, walletIdTextView, walletBalanceTextView;

    // Predefined wallet addresses and private keys
    private static final String[][] WALLET_PAIRS = {
            {"0xEC3b13C00Af3d785f314e722db02b1d31C4DbD4c", "0x14c52e5f82204048bddefed194465cc751bd94b5e9219376001c5f7c127ac724"},
            {"0x7881633923e5d009FD62CcEA0E9357760629e6C2", "0x4d99090dd6e6d04458c6c666eeed4fb03b64ba7972f3fba6f7b9f65e48e67a39"},
            {"0x14786531B91AC30a6317A55ff9D7b3Fe80940ab4", "0xe63863b5eac76e7ac10e073111ab1898ad3b0526da87b06d3ba2947c4435630b"},
            {"0x70877847006e697D37C0d1fdAeD574D2D23DF0da", "0x5a94790632e1e256613a94bd2cb3211d5388a8cd2cb1662d6f71b55abf9778a6"},
            {"0xF78C6639C72CBfa407695C3F9eaFA5DDC13CC61b", "0xa618c61e15d686c018924be503af835a425d1a5c47334949702be4391d34925a"},
            {"0x86C0515FcD1b051370DBA23e2F5b00cf037d75e8", "0x3845da94875c553b51a93c80f12274b2b726a5924f9f36fa9dbb8a0815880140"},
            {"0xF10a0A90B594223956489E7eE0CE7e1231F4c5e5", "0x7fc620773dd16fbcdea5247517c8739f6bfc9a2351846b98fbd3156fe66ccb1e"},
            {"0x47c81Fa6C7975AD007cfF3d377E5d396D4F47580", "0x822e491e58321cac436725fcfea5ea8d799dde31429b882508afa80375894ab2"},
            {"0xB0FE44a31c15EaCFc3155840C8f358975D58aAC7", "0xd71d97abb1eaadc88b51b3071d228895eb1b10995891c0ae45f0a4148cef9dec"},
            {"0xB0FE44a31c15EaCFc3155840C8f358975D58aAC7", "0xd71d97abb1eaadc88b51b3071d228895eb1b10995891c0ae45f0a4148cef9dec"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.appBg));
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        addWalletButton = findViewById(R.id.add_wallet_button);
        cardView = findViewById(R.id.card_view);
        usernameTextView = findViewById(R.id.username);
        walletIdTextView = findViewById(R.id.wallet_id);
        walletBalanceTextView = findViewById(R.id.wallet_balance);
        sendButton = findViewById(R.id.send_button);
        receiveButton = findViewById(R.id.receive_button);
        checkWalletStatus();

        // Handle Add Wallet Button Click
        addWalletButton.setOnClickListener(view -> createWalletForUser());

        receiveButton.setOnClickListener(view -> displayQRCode());
    }

    private void checkWalletStatus() {
        String userId = auth.getCurrentUser().getUid(); // Get current user ID
        DocumentReference userRef = firestore.collection("Users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String walletId = document.getString("walletId");
                    String walletBalance = document.getString("walletBalance");

                    if (walletId != null && !walletId.isEmpty()) {
                        // Wallet exists - show CardView
                        cardView.setVisibility(View.VISIBLE);
                        addWalletButton.setVisibility(View.GONE);

                        // Mask wallet ID
                        String maskedWalletId = maskWalletId(walletId);
                        usernameTextView.setText(document.getString("fullName"));
                        walletIdTextView.setText("Wallet ID: " + maskedWalletId);

                        if (walletBalance != null && !walletBalance.isEmpty()) {
                            walletBalanceTextView.setText("Balance: " + walletBalance);
                        } else {
                            walletBalanceTextView.setText("Balance: Fetching...");
                            updateWalletBalance(walletId); // Fetch the balance from Ethereum if not set
                        }
                    } else {
                        // No wallet - show Add Wallet button
                        cardView.setVisibility(View.GONE);
                        addWalletButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e("MainActivity", "No document found for user.");
                }
            } else {
                Log.e("MainActivity", "Error fetching wallet info: " + task.getException().getMessage());
                Toast.makeText(this, "Error fetching wallet info: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private String maskWalletId(String walletId) {
        if (walletId.length() > 4) {
            String lastFourDigits = walletId.substring(walletId.length() - 4);
            return "XXXX " + lastFourDigits;
        }
        return "XXXX";
    }

    private void createWalletForUser() {
        String userId = auth.getCurrentUser().getUid();
        DocumentReference userRef = firestore.collection("Users").document(userId);

        // Assign one of the 10 predefined wallet pairs
        int userIndex = getUserIndex();
        String walletId = WALLET_PAIRS[userIndex][0];
        String privateKey = WALLET_PAIRS[userIndex][1];

        // Create a HashMap to store the wallet data without a default balance
        Map<String, Object> walletData = new HashMap<>();
        walletData.put("walletId", walletId);
        walletData.put("privateKey", privateKey); // Store private key in hexadecimal format

        // Log the generated wallet ID and private key
        Log.d("GeneratedWalletId", walletId);
        Log.d("GeneratedPrivateKey", privateKey);

        // Use 'set' to ensure that the wallet data is written properly
        userRef.set(walletData, SetOptions.merge())  // Merge if document exists
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Wallet created successfully!", Toast.LENGTH_SHORT).show();
                    checkWalletStatus(); // Refresh UI
                    updateWalletBalance(walletId); // Fetch actual balance after wallet creation
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create wallet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        generateAndUploadQRCode(walletId, privateKey, userId);
    }

    private int getUserIndex() {
        // Logic to assign one of the predefined 10 wallet pairs to each user
        int userCount = 10;  // Total available wallets
        int randomIndex = (int) (Math.random() * userCount);
        return randomIndex;
    }

    private void generateAndUploadQRCode(String walletId, String privateKey, String userId) {
        // Generate QR code from wallet data
        String qrData = walletId + "," + privateKey;
        Bitmap qrCode = generateQRCodeBitmap(qrData);

        // Convert Bitmap to ByteArrayOutputStream to upload to Firebase
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qrCode.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] qrCodeData = baos.toByteArray();

        // Upload QR code to Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("QR_Codes/" + userId + "_qr.png");
        UploadTask uploadTask = storageRef.putBytes(qrCodeData);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Store the QR code URL in Firestore
                DocumentReference userRef = firestore.collection("Users").document(userId);
                userRef.update("qrCodeUrl", uri.toString())
                        .addOnSuccessListener(aVoid -> Log.d("QR Code", "Uploaded and URL saved"));
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Error uploading QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private Bitmap generateQRCodeBitmap(String data) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 500, 500);

            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.createBitmap(bitMatrix);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateWalletBalance(String walletId) {
        EthereumService ethereumService = new EthereumService();
        ethereumService.checkBalance(walletId, new EthereumService.BalanceCallback() {
            @Override
            public void onBalanceFetched(String balance) {
                // Update the UI with the fetched balance
                walletBalanceTextView.setText("Balance: " + balance);
            }

            @Override
            public void onError(String error) {
                // Handle error case
                walletBalanceTextView.setText("Balance: Error fetching");
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayQRCode() {
        String userId = auth.getCurrentUser().getUid();
        DocumentReference userRef = firestore.collection("Users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String qrCodeUrl = document.getString("qrCodeUrl");
                    if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
                        // Load QR code image using an image loading library like Picasso
                        ImageView qrCodeImageView = findViewById(R.id.qr_code_image_view);
                        Picasso.get().load(qrCodeUrl).into(qrCodeImageView);
                    }
                }
            } else {
                Toast.makeText(MainActivity.this, "Error fetching QR code", Toast.LENGTH_SHORT).show();
            }
        });
    }

}