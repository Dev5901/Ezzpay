package com.example.ezzpay;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView transactionListView;
    private Web3j web3j;
    private TransactionAdapter adapter;
    private List<String> transactions;

    // Firebase Firestore instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        transactionListView = findViewById(R.id.transaction_list);
        transactions = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactions);
        transactionListView.setAdapter(adapter);

        // Connect to Ganache (Running on Local Machine)
        web3j = Web3j.build(new HttpService("http://10.0.2.2:7545")); // Ganache URL for Android Emulator
        fetchUserWalletAddress();
    }

    private void fetchUserWalletAddress() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid(); // Get logged-in user ID

        // Fetch walletId from Firestore
        DocumentReference userDocRef = db.collection("Users").document(userId);
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String walletAddress = task.getResult().getString("walletId");

                if (walletAddress != null) {
                    // Fetch transaction history after getting wallet address
                    fetchTransactionHistory(walletAddress);
                } else {
                    Log.e("HistoryActivity", "Wallet address is null.");
                    Toast.makeText(HistoryActivity.this, "Wallet address not found!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("HistoryActivity", "Error fetching wallet address", task.getException());
                Toast.makeText(HistoryActivity.this, "Error fetching wallet address", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTransactionHistory(String userWalletAddress) {
        try {
            web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, true)
                    .sendAsync()
                    .thenAccept(block -> {
                        // Ensure the block and its transactions are not null
                        if (block.getBlock() != null && block.getBlock().getTransactions() != null) {
                            List<EthBlock.TransactionResult> txs = block.getBlock().getTransactions(); // List of transactions

                            // Iterate over the transactions
                            for (EthBlock.TransactionResult txResult : txs) {
                                // Check if the transaction is a Transaction object
                                if (txResult.get() instanceof Transaction) {
                                    Transaction tx = (Transaction) txResult.get(); // Cast to Transaction

                                    // Check if the wallet address is involved in this transaction
                                    if (tx.getFrom().equalsIgnoreCase(userWalletAddress) ||
                                            tx.getTo().equalsIgnoreCase(userWalletAddress)) {

                                        // Fetch full transaction details using the hash
                                        String transactionHash = tx.getHash();
                                        fetchTransactionDetails(transactionHash, userWalletAddress);
                                    }
                                }
                            }
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "No transactions found", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e("HistoryActivity", "Error fetching transactions", e);
            runOnUiThread(() -> Toast.makeText(this, "Error fetching transactions", Toast.LENGTH_SHORT).show());
        }
    }

    private void fetchTransactionDetails(String transactionHash, String userWalletAddress) {
        web3j.ethGetTransactionByHash(transactionHash).sendAsync().thenAccept(txResponse -> {
            // Get the full transaction details
            if (txResponse.getResult() != null) {
                Transaction tx = txResponse.getResult();
                String from = tx.getFrom();
                String to = tx.getTo();
                String value = tx.getValue().toString(); // Value transferred in Wei

                // Convert Wei to Ether (optional, for better readability)
                BigDecimal etherValue = Convert.fromWei(value, Convert.Unit.ETHER);

                // Log the full details of the transaction
                Log.d("TransactionDetails", "Hash: " + transactionHash);
                Log.d("TransactionDetails", "From: " + from);
                Log.d("TransactionDetails", "To: " + to);
                Log.d("TransactionDetails", "Value: " + etherValue.toString() + " ETH");

                // Add the transaction details to your list (for displaying in the UI)
                String transactionDetails = "From: " + from + "\nTo: " + to + "\nValue: " + etherValue.toString() + " ETH";
                transactions.add(transactionDetails);

                // Notify the adapter to update the UI with the fetched transactions
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                });
            }
        }).exceptionally(ex -> {
            Log.e("TransactionDetails", "Error fetching transaction details", ex);
            return null;
        });
    }





}
