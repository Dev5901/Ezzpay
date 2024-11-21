package com.example.ezzpay;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

public class EthereumService {

    private Web3j web3j;

    public interface BalanceCallback {
        void onBalanceFetched(String balance);
        void onError(String error);
    }

    public EthereumService() {
        String infuraUrl = "https://holesky.infura.io/v3/b7e4406c0c524520bc43c460a03bf791";
        web3j = Web3j.build(new HttpService(infuraUrl));
    }

    public void checkBalance(String walletId, final BalanceCallback callback) {
        // Ensure this runs asynchronously in a background thread
        new Thread(() -> {
            try {
                EthGetBalance balance = web3j.ethGetBalance(walletId, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
                String balanceInEther = Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER).toString();
                callback.onBalanceFetched(balanceInEther);
            } catch (Exception e) {
                callback.onError("Error fetching balance: " + e.getMessage());
            }
        }).start();
    }
}

