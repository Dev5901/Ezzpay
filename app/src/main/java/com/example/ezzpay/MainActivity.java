package com.example.ezzpay;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.StorageReference;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private Button addWalletButton;
    private View cardView;
    private TextView usernameTextView, walletIdTextView, walletBalanceTextView;

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
        // Check Wallet Status
        checkWalletStatus();

        // Handle Add Wallet Button Click
        addWalletButton.setOnClickListener(view -> createWalletForUser());
    }

    private void checkWalletStatus() {
        String userId = auth.getCurrentUser().getUid(); // Get current user ID
        DocumentReference userRef = firestore.collection("Users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String walletId = document.getString("walletId");
                    String walletBalance = document.getString("walletBalance");

                    if (walletId != null && !walletId.isEmpty()) {
                        // Wallet exists - show CardView
                        cardView.setVisibility(View.VISIBLE);
                        addWalletButton.setVisibility(View.GONE);

                        // Mask wallet ID
                        String maskedWalletId = maskWalletId(walletId);

                        // Populate card details
                        usernameTextView.setText(document.getString("fullName"));
                        walletIdTextView.setText("Wallet ID: " + maskedWalletId);
                        walletBalanceTextView.setText("Balance: " + (walletBalance != null ? walletBalance : "$0"));
                    } else {
                        // No wallet - show Add Wallet button
                        cardView.setVisibility(View.GONE);
                        addWalletButton.setVisibility(View.VISIBLE);
                    }
                }
            } else {
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

        // Generate Wallet ID and Private Key
        String walletId = generateWalletId();
        String privateKey = generatePrivateKey();

        // Create a HashMap to store the wallet data
        Map<String, Object> walletData = new HashMap<>();
        walletData.put("walletId", walletId);
        walletData.put("privateKey", privateKey);
        walletData.put("walletBalance", "$0"); // Default balance

        // Log the generated wallet ID and private key
        Log.d("GeneratedWalletId", walletId);
        Log.d("GeneratedPrivateKey", privateKey);

        // Use 'set' to ensure that the wallet data is written properly
        userRef.set(walletData, SetOptions.merge())  // Merge if document exists
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Wallet created successfully!", Toast.LENGTH_SHORT).show();
                    checkWalletStatus(); // Refresh UI
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create wallet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String generateWalletId() {
        SecureRandom random = new SecureRandom();
        byte[] addressBytes = new byte[20];
        random.nextBytes(addressBytes);

        // Convert bytes to hex string and add "0x" prefix
        StringBuilder walletId = new StringBuilder("0x");
        for (byte b : addressBytes) {
            walletId.append(String.format("%02x", b));
        }
        return walletId.toString();
    }

    private String generatePrivateKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);

        // Convert bytes to hex string
        StringBuilder privateKey = new StringBuilder();
        for (byte b : keyBytes) {
            privateKey.append(String.format("%02x", b));
        }
        return privateKey.toString();
    }
}

