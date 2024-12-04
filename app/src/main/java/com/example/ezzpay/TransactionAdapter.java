package com.example.ezzpay;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class TransactionAdapter extends ArrayAdapter<String> {

    public TransactionAdapter(Context context, List<String> transactions) {
        super(context, 0, transactions);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.transaction_item, parent, false);
        }
        TextView transactionText = convertView.findViewById(R.id.transaction_text);
        transactionText.setText(getItem(position));
        return convertView;
    }
}