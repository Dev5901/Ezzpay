package com.example.ezzpay;

//import android.util.Log;
//
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.core.DefaultBlockParameterName;
//import org.web3j.protocol.http.HttpService;
//import org.web3j.protocol.core.methods.response.EthGetBalance;
//import org.web3j.utils.Convert;
//
//import java.math.BigDecimal;
//import java.math.BigInteger;
//
//public class EthereumService {
//
//    private Web3j web3j;
//
//    private static final String INFURA_URL = "https://mainnet.infura.io/v3/YOUR_INFURA_PROJECT_ID";
//    private static final Web3j web3 = Web3j.build(new HttpService(INFURA_URL));
//
//    public interface BalanceCallback {
//        void onBalanceFetched(String balance);
//        void onError(String error);
//    }
//
//    public static void checkBalance(String walletId, BalanceCallback callback) {
//        new Thread(() -> {
//            try {
//                Log.d("EthereumService", "Fetching balance for wallet ID: " + walletId);
//                EthGetBalance ethGetBalance = web3.ethGetBalance(walletId, DefaultBlockParameterName.LATEST).send();
//                BigInteger weiBalance = ethGetBalance.getBalance();
//                BigDecimal ethBalance = new BigDecimal(weiBalance).divide(new BigDecimal("1000000000000000000")); // Convert Wei to ETH
//                callback.onBalanceFetched(ethBalance.toPlainString());
//            } catch (Exception e) {
//                Log.e("EthereumService", "Error fetching balance", e);
//                callback.onError("Failed to fetch balance: " + e.getMessage());
//            }
//        }).start();
//    }
//}


import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

import android.os.Handler;
import android.os.Looper;

import java.math.BigInteger;

public class EthereumService {

    private Web3j web3j;

    // Constructor to initialize Web3j and connect to Infura
    public EthereumService() {
        //web3j = Web3j.build(new HttpService("http://10.0.2.2:7545"));
        web3j = Web3j.build(new HttpService("http://10.0.2.2:7545"));
    }

    // Method to get the current balance of the wallet address
    public void checkBalance(String walletAddress, BalanceCallback callback) {
        // Start a new thread for network operations
        new Thread(() -> {
            try {
                // Fetch the balance in Wei (smallest unit of Ethereum)
                EthGetBalance balanceResponse = web3j.ethGetBalance(walletAddress, DefaultBlockParameterName.LATEST).send();
                BigInteger balanceInWei = balanceResponse.getBalance();

                // Convert the balance from Wei to ETH
                String balanceInEth = Convert.fromWei(balanceInWei.toString(), Convert.Unit.ETHER).toPlainString();

                // Call the callback method to return the balance to the UI thread
                new Handler(Looper.getMainLooper()).post(() -> callback.onBalanceFetched(balanceInEth));
            } catch (Exception e) {
                // In case of an error, invoke the error callback
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error fetching balance: " + e.getMessage()));
            }
        }).start(); // Start the thread
    }

    // Callback interface to handle balance response or error
    public interface BalanceCallback {
        void onBalanceFetched(String balance);
        void onError(String error);
    }
}


