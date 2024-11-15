package com.example.ezzpay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ezzpay.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EditText firstName, lastName, email, phoneNumber, otpField, walletAddress;
    private Spinner ethereumWalletDropdown;
    private Button signupButton, generateWalletButton, sendOtpButton;
    private TextView loginRedirectText;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize fields
        firstName = findViewById(R.id.signup_first_name);
        lastName = findViewById(R.id.signup_last_name);
        email = findViewById(R.id.signup_email);
        phoneNumber = findViewById(R.id.signup_phone_number);
        otpField = findViewById(R.id.signup_otp);
        ethereumWalletDropdown = findViewById(R.id.ethereum_wallet_dropdown);
        walletAddress = findViewById(R.id.wallet_address);
        signupButton = findViewById(R.id.signup_button);
        generateWalletButton = findViewById(R.id.generate_wallet_button);
        sendOtpButton = findViewById(R.id.send_otp_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        // Handle Ethereum Wallet Dropdown
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ethereum_wallet_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ethereumWalletDropdown.setAdapter(adapter);

        ethereumWalletDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = parent.getItemAtPosition(position).toString();
                if ("Yes".equals(selection)) {
                    walletAddress.setVisibility(View.VISIBLE);
                    generateWalletButton.setVisibility(View.GONE);
                } else {
                    walletAddress.setVisibility(View.GONE);
                    generateWalletButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Send OTP
        sendOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = phoneNumber.getText().toString().trim();
                if (phone.isEmpty() || phone.length() != 10) {
                    phoneNumber.setError("Enter a valid phone number");
                    return;
                }

                String fullPhoneNumber = "+1" + phone; // Assuming CANADA (+1)

                PhoneAuthOptions options =
                        PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber(fullPhoneNumber)
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(SignUpActivity.this)
                                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    @Override
                                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                                        Toast.makeText(SignUpActivity.this, "Verification Completed", Toast.LENGTH_SHORT).show();
                                        // Auto-retrieval or instant verification
                                        signInWithPhoneAuthCredential(credential);
                                    }

                                    @Override
                                    public void onVerificationFailed(FirebaseException e) {
                                        Toast.makeText(SignUpActivity.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                                        SignUpActivity.this.verificationId = verificationId;
                                        resendToken = token;
                                        Toast.makeText(SignUpActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .build();
                PhoneAuthProvider.verifyPhoneNumber(options);
            }
        });

        // Sign Up Button
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String otp = otpField.getText().toString().trim();
                if (otp.isEmpty()) {
                    otpField.setError("Enter the OTP");
                    return;
                }

                verifyOtp(otp);
            }
        });

        // Login Redirect
        loginRedirectText.setOnClickListener(view -> startActivity(new Intent(SignUpActivity.this, MainActivity.class)));
    }

    private void verifyOtp(String otp) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserDataToFirestore();
                    } else {
                        Toast.makeText(SignUpActivity.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDataToFirestore() {
        String userId = auth.getCurrentUser().getUid();

        com.example.ezzpay.User user = new User(
                userId,
                firstName.getText().toString().trim(),
                lastName.getText().toString().trim(),
                email.getText().toString().trim(),
                phoneNumber.getText().toString().trim(),
                ethereumWalletDropdown.getSelectedItem().toString(),
                walletAddress.getText().toString().trim()
        );

        firestore.collection("Users").document(userId).set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "User Registered", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                    } else {
                        Toast.makeText(SignUpActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
