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
    String walletId;

    // Predefined wallet addresses and private keys
    private static final String[][] WALLET_PAIRS = {
            {"0xD8A423bc11E4F2A48d388A7CdF27279D7852c7f3", "0xa05edfb0b323b3a0f44e898f0c1b74f073e5bff6918b366145769de1cd44acac"},
            {"0x279bD993B47bb9adb6056804124be104420581F0", "0xf0a136da74a3507c7780cfac49c10eb21fa616e57c0935994c55918cc8cca755"}
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

        // Assign one of the 2 predefined wallet pairs
        int userIndex = getUserIndex();
        String walletId = WALLET_PAIRS[userIndex][0];
        String privateKey = WALLET_PAIRS[userIndex][1];

        // Create a HashMap to store the wallet data
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

        // Generate and upload QR code for the wallet
        generateAndUploadQRCode(walletId, privateKey, userId);
    }

    private int getUserIndex() {
        // Logic to assign one of the predefined wallet pairs to each user
        return (int) (Math.random() * WALLET_PAIRS.length); // Get a random wallet pair from available ones
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
                // Run on UI thread since UI updates must happen on the main thread
                runOnUiThread(() -> walletBalanceTextView.setText("Balance: " + balance + " ETH"));

                Toast.makeText(MainActivity.this, "Balance received: " + balance + " ETH", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                // Handle errors
                runOnUiThread(() -> {
                    // Show the error message in a Toast
                    Toast.makeText(MainActivity.this, "Error fetching balance: " + error, Toast.LENGTH_SHORT).show();

                    // Set the full error message to the wallet balance TextView
                    walletBalanceTextView.setText("Error: " + error);
                });
            }
        });
    }

    private void displayQRCode() {
        String userId = auth.getCurrentUser().getUid();
        StorageReference qrCodeRef = FirebaseStorage.getInstance().getReference().child("WalletQRs/" + userId + ".png");

        qrCodeRef.getBytes(1024 * 1024) // Adjust size as needed
                .addOnSuccessListener(bytes -> {
                    Bitmap qrBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    // Show QR code in a dialog
                    Dialog qrDialog = new Dialog(MainActivity.this);
                    qrDialog.setContentView(R.layout.qr_code_image_view);

                    ImageView qrImageView = qrDialog.findViewById(R.id.qr_code_image_view);
                    qrImageView.setImageBitmap(qrBitmap);

                    qrDialog.show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to fetch QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}