package com.example.ezzpay;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.MenuItem;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.web3j.crypto.Credentials;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private Button addWalletButton, sendButton, receiveButton, historyButton;
    private View cardView;
    private TextView usernameTextView, walletIdTextView, walletBalanceTextView;
    String walletId;

    // Predefined wallet addresses and private keys
    private static final String[][] WALLET_PAIRS = {
            {"0xD8A423bc11E4F2A48d388A7CdF27279D7852c7f3", "0xa05edfb0b323b3a0f44e898f0c1b74f073e5bff6918b366145769de1cd44acac"},
            {"0x279bD993B47bb9adb6056804124be104420581F0", "0xf0a136da74a3507c7780cfac49c10eb21fa616e57c0935994c55918cc8cca755"},
            {"0x332646E38b210ecaF069140e873CcDb7f8d72b4d","0xcb0fbee26eb814f7e122f39f8d89808e362666b09208e8ed9cfaeeae095caec8"},
            {"0x76590844C4678F4baF661aA8DdCC849F2FA65D15","0x71551944392f6b4908a3a24fe2db746729d6e2879821ea205d3ca6960b9a0715"},
            {"0xa0315015732dC28528071ffd7D2f19EA85C38570","0x4f4d5924b96669913ce56d06f7e3de9983f8d73122e1d3bafc50bdc9ee4a585b"},
            {"0x9E6Aa77347c8C96Ac8E683c8B73d670d332D19Fd","0x4e153ab88b12a38fc7231173ac255c66b561680f4bdf9ca3682fab18644f76b6"},
            {"0x336273DD20432f79f8f858244a74C49F5b4280Ce","0x7a1b7b0b38bd30a141a76bb83ee5ef95fd1ff87e5aee299fb7fb9bf83ebd37b3"},
            {"0x36CC2F8fCba614022C4d18279060bdc3d5817c6B","0x1227d522b99f1a41ddf8e93fec87e00015a50f77b58c69c94ee0d5d7f09a88c6"},
            {"0xE63Ffe3540885C630639548bBE0D3299A524fe40","0xc3babbef0a433ad34281259f6ac13f69b47fd067d92d7b85c9f8e238da10e892"},
            {"0xd053876aF32A41aC41A360086c9a6d38a3a44a9C","0x6e7edf4ccfc8e1e3921e95cf82b80d415c7bc492f896f1d5d47f7f107a0ac75e"}
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

//        Button profileButton = findViewById(R.id.action_profile);
//        Button historyButton = findViewById(R.id.action_history);
//        Button signOutButton = findViewById(R.id.action_sign_out);
//
//        profileButton.setOnClickListener(view -> showProfile());
//        historyButton.setOnClickListener(view -> setupHistoryButton());
//        signOutButton.setOnClickListener(view -> signOutUser());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.action_profile) {
                    showProfile();
                    return true;
                } else if (itemId == R.id.action_history) {
                    setupHistoryButton();
                    return true;
                } else if (itemId == R.id.action_sign_out) {
                    signOutUser();
                    return true;
                } else {
                    return false;
                }
            }
        });

        // Setup send button functionality
        setupSendButton();
        //setupHistoryButton();
    }

    private void signOutUser() {
        FirebaseAuth.getInstance().signOut();
        // Navigate to the login or home screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    private void setupHistoryButton() {
            // Navigate to HistoryActivity
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
    }

    private void showProfile() {
        // Get the current user's ID
        String userId = auth.getCurrentUser().getUid();
        DocumentReference userRef = firestore.collection("Users").document(userId);

        // Fetch the user data from Firestore
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot document = task.getResult();

                // Extract user information from Firestore document
                String fullName = document.getString("fullName");
                String email = document.getString("email");
                String phone = document.getString("contactNumber");
                String walletId = document.getString("walletId");


                // Prepare user information to display
                String profileInfo = "Name: " + fullName + "\n"
                        + "Email: " + email + "\n"
                        + "Phone: " + phone + "\n"
                        + "Wallet ID: " + walletId + "\n";

                // Create and display the dialog
                Dialog profileDialog = new Dialog(this);
                profileDialog.setContentView(R.layout.dialog_profile_info); // Create a layout file for the dialog
                profileDialog.setCancelable(true);

                TextView profileTextView = profileDialog.findViewById(R.id.profile_info_text_view);
                profileTextView.setText(profileInfo);

                Button closeButton = profileDialog.findViewById(R.id.close_button);
                closeButton.setOnClickListener(view -> profileDialog.dismiss());

                profileDialog.show();
            } else {
                Toast.makeText(this, "Failed to fetch profile information.", Toast.LENGTH_SHORT).show();
                Log.e("showProfile", "Error fetching profile info: " + task.getException().getMessage());
            }
        });
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


//    private int getUserIndex() {
//        // Logic to assign one of the predefined wallet pairs to each user
//        return (int) (Math.random() * WALLET_PAIRS.length); // Get a random wallet pair from available ones
//    }
private List<Integer> assignedWalletIndexes = new ArrayList<>();

    private int getUserIndex() {
        // Get the list of unassigned wallet indexes
        List<Integer> availableIndexes = new ArrayList<>();
        for (int i = 0; i < WALLET_PAIRS.length; i++) {
            if (!assignedWalletIndexes.contains(i)) {
                availableIndexes.add(i);
            }
        }

        // If no wallets are available (all have been assigned), reset the assigned list
        if (availableIndexes.isEmpty()) {
            assignedWalletIndexes.clear();
            availableIndexes = new ArrayList<>();
            for (int i = 0; i < WALLET_PAIRS.length; i++) {
                availableIndexes.add(i);
            }
        }

        // Select a random index from the available ones
        int randomIndex = (int) (Math.random() * availableIndexes.size());
        int assignedIndex = availableIndexes.get(randomIndex);

        // Mark this wallet pair as assigned
        assignedWalletIndexes.add(assignedIndex);

        return assignedIndex;
    }


    private void generateAndUploadQRCode(String walletId, String privateKey, String userId) {
        // Generate QR code from wallet data
        String qrData = walletId;
        Bitmap qrCode = generateQRCodeBitmap(qrData, MainActivity.this);

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

//    private Bitmap generateQRCodeBitmap(String data) {
//        try {
//            MultiFormatWriter writer = new MultiFormatWriter();
//            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 500, 500);
//
//            BarcodeEncoder encoder = new BarcodeEncoder();
//            return encoder.createBitmap(bitMatrix);
//        } catch (Exception e) {
//            e.printStackTrace();
//
//            return null;
//        }
//    }

    private Bitmap generateQRCodeBitmap(String data, Context context) {
        try {
            // Define QR code size
            int qrCodeSize = 500;

            // Create a BitMatrix for the QR code
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize);

            // Retrieve colors from resources
            int foregroundColor = ContextCompat.getColor(context, R.color.offWhite);
            int backgroundColor = ContextCompat.getColor(context, R.color.appBg);

            // Create a Bitmap with custom colors
            Bitmap bitmap = Bitmap.createBitmap(qrCodeSize, qrCodeSize, Bitmap.Config.ARGB_8888);

            // Fill the Bitmap based on the BitMatrix
            for (int x = 0; x < qrCodeSize; x++) {
                for (int y = 0; y < qrCodeSize; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? foregroundColor : backgroundColor);
                }
            }

            return bitmap;
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

    private void setupSendButton() {
        sendButton.setOnClickListener(view -> {
            // Start QR code scanner to fetch the receiver's wallet address
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan Receiver's Wallet QR Code");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.initiateScan();
        });
    }


    // Handle QR Code Result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                // QR code content (receiver's wallet address)
                String receiverAddress = result.getContents();
                showSendDialog(receiverAddress);
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Show Dialog to Enter Amount and Send Ethereum
    private void showSendDialog(String receiverAddress) {
        Dialog sendDialog = new Dialog(this);
        sendDialog.setContentView(R.layout.dialog_send_eth); // Create a custom layout file: dialog_send_eth.xml

        TextView receiverTextView = sendDialog.findViewById(R.id.receiver_address);
        receiverTextView.setText("Receiver: " + receiverAddress);

        TextView amountInput = sendDialog.findViewById(R.id.eth_amount_input);
        Button sendEthButton = sendDialog.findViewById(R.id.send_eth_button);

        sendEthButton.setOnClickListener(view -> {
            String amountText = amountInput.getText().toString();
            if (amountText.isEmpty()) {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }

            BigDecimal amount = new BigDecimal(amountText);
            sendEthereum(receiverAddress, amount);
            sendDialog.dismiss();
        });

        sendDialog.show();
    }

    // Send Ethereum
    private void sendEthereum(String receiverAddress, BigDecimal amount) {
        // Fetch the current user's private key from Firestore
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("Users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String privateKey = task.getResult().getString("privateKey"); // Fetch privateKey from Firestore

                if (privateKey == null || privateKey.isEmpty()) {
                    Toast.makeText(this, "Private key not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Start a new thread for the transaction
                new Thread(() -> {
                    try {
                        // Initialize Web3j
                        Web3j web3j = Web3j.build(new HttpService("http://10.0.2.2:7545")); // Ganache RPC URL

                        // Load sender's credentials
                        Credentials credentials = Credentials.create(privateKey);

                        // Send transaction
                        TransactionReceipt receipt = Transfer.sendFunds(
                                web3j,
                                credentials,
                                receiverAddress,
                                amount,
                                Convert.Unit.ETHER
                        ).send();

                        runOnUiThread(() -> {
                            // Display transaction confirmation
                            String confirmationMsg = "Transaction Successful!\nHash: " + receipt.getTransactionHash();
                            Toast.makeText(this, confirmationMsg, Toast.LENGTH_LONG).show();
                            Log.d("TransactionReceipt", receipt.toString());

                            // Update the balance on the Android screen
                            updateWalletBalance(credentials.getAddress());
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Transaction Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("SendEthereumError", e.getMessage(), e);
                        });
                    }
                }).start();
            } else {
                Toast.makeText(this, "Failed to fetch sender's wallet information!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void displayQRCode() {
        String userId = auth.getCurrentUser().getUid();
        DocumentReference userRef = firestore.collection("Users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String qrCodeUrl = document.getString("qrCodeUrl");
                    if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
                        // Open the dialog to show QR code
                        showQRCodeDialog(qrCodeUrl);
                    }
                }
            }
        });
    }

    private void showQRCodeDialog(String qrCodeUrl) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.qr_code_image_view);

        ImageView qrImageView = dialog.findViewById(R.id.qr_code_image_view);

        // Load and display the QR code from URL
        Picasso.get().load(qrCodeUrl).into(qrImageView);

        dialog.show();
    }
}

