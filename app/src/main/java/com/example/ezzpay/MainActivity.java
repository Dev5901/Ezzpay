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
import java.math.BigDecimal;
import java.util.HashMap;
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

        // Setup send button functionality
        setupSendButton();
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
        String qrData = walletId;
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
        // Sender's private key (from Firebase or local storage)
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("Users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String privateKey = "0xf0a136da74a3507c7780cfac49c10eb21fa616e57c0935994c55918cc8cca755";
                //task.getResult().getString("privateKey");

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
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Transaction Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("SendEthereumError", e.getMessage(), e);
                        });
                    }
                }).start();
            } else {
                Toast.makeText(this, "Failed to fetch sender's wallet", Toast.LENGTH_SHORT).show();
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

