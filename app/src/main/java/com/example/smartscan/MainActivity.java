package com.example.smartscan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {
    private TextView tvCreateAccount;
    private Button btnSignIn;
    private Button btnGoogleSignIn;
    private EditText etEmail;
    private EditText etPassword;

    private FirebaseAuth firebaseAuth;
    private boolean isFirebaseReady = false;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_authentification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeFirebaseAuth();
        initializeGoogleSignInLauncher();

        tvCreateAccount = findViewById(R.id.tvCreateAccount);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });

        btnSignIn.setOnClickListener(v -> signInWithEmailPassword());

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        if (!isFirebaseReady) {
            btnSignIn.setEnabled(false);
            btnGoogleSignIn.setEnabled(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFirebaseReady && firebaseAuth.getCurrentUser() != null) {
            openNotesList();
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

        if (isFirebaseReady) {
            configureGoogleSignIn();
        }
    }

    private void initializeGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        handleGoogleSignInResult(result.getData());
                    } else {
                        Toast.makeText(this, "Google sign-in canceled", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void configureGoogleSignIn() {
        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        if (!isFirebaseReady) {
            Toast.makeText(this, "Firebase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        if (googleSignInClient == null) {
            Toast.makeText(this, "Google sign-in not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void handleGoogleSignInResult(Intent data) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException.class);

            if (account == null || account.getIdToken() == null) {
                Toast.makeText(this, "Google account error", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        openNotesList();
                    } else {
                        Toast.makeText(
                                this,
                                task.getException() != null ? task.getException().getMessage() : "Google auth failed",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void signInWithEmailPassword() {
        if (!isFirebaseReady) {
            Toast.makeText(this, "Firebase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password required");
            etPassword.requestFocus();
            return;
        }

        btnSignIn.setEnabled(false);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        openNotesList();
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                task.getException() != null ? task.getException().getMessage() : "Login failed",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openNotesList() {
        startActivity(new Intent(MainActivity.this, NotesListActivity.class));
        finish();
    }
}