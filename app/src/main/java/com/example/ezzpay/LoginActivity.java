package com.example.ezzpay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText loginEmail, loginPassword;
    private TextView signupRedirectText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and UI elements
        auth = FirebaseAuth.getInstance();
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);

        // Handle login button click
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleLogin();
            }
        });

        // Redirect to the SignUpActivity
        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectToSignUp();
            }
        });
    }

    private void handleLogin() {
        String email = loginEmail.getText().toString().trim();
        String pass = loginPassword.getText().toString().trim();

        if (isValidInput(email, pass)) {
            auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Toast.makeText(LoginActivity.this, "Login Success", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoginActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private boolean isValidInput(String email, String pass) {
        if (email.isEmpty()) {
            loginEmail.setError("Email cannot be empty");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError("Please enter a valid email");
            return false;
        }
        if (pass.isEmpty()) {
            loginPassword.setError("Password cannot be empty");
            return false;
        }
        return true;
    }

    private void redirectToSignUp() {
        startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
    }
}
