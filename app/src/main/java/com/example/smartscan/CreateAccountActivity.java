package com.example.smartscan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText etCreateEmail;
    private EditText etCreatePassword;
    private EditText etConfirmPassword;
    private Button btnCreateAccountSubmit;
    private TextView tvBackToLogin;
    private FirebaseAuth firebaseAuth;
    private boolean isFirebaseReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainCreateAccount), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeFirebaseAuth();

        etCreateEmail = findViewById(R.id.etCreateEmail);
        etCreatePassword = findViewById(R.id.etCreatePassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreateAccountSubmit = findViewById(R.id.btnCreateAccountSubmit);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnCreateAccountSubmit.setOnClickListener(v -> createAccount());
        tvBackToLogin.setOnClickListener(v -> finish());

        if (!isFirebaseReady) {
            btnCreateAccountSubmit.setEnabled(false);
        }
    }

    private void initializeFirebaseAuth() {
        try {
            firebaseAuth = FirebaseAuth.getInstance();
            isFirebaseReady = true;
        } catch (IllegalStateException e) {
            isFirebaseReady = false;
            Toast.makeText(
                    this,
                    "Firebase not configured. Add google-services.json in app/ and sync.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void createAccount() {
        if (!isFirebaseReady) {
            Toast.makeText(this, "Firebase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = etCreateEmail.getText().toString().trim();
        String password = etCreatePassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etCreateEmail.setError("Email required");
            etCreateEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etCreatePassword.setError("Password must be at least 6 characters");
            etCreatePassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnCreateAccountSubmit.setEnabled(false);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnCreateAccountSubmit.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, NotesListActivity.class));
                        finish();
                    } else {
                        Toast.makeText(
                                this,
                                task.getException() != null ? task.getException().getMessage() : "Account creation failed",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}

