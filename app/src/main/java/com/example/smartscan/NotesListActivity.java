package com.example.smartscan;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.util.List;

public class NotesListActivity extends AppCompatActivity {

    private static final String STATE_QUERY = "state_query";

    private EditText etSearch;
    private TextView tvNotesEmpty;
    private NotesAdapter notesAdapter;
    private NotesRepository notesRepository;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainNotesList), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        notesRepository = new NotesRepository(this);

        etSearch = findViewById(R.id.etSearchNotes);
        Button btnNewScan = findViewById(R.id.btnNewScan);
        Button btnLogout = findViewById(R.id.btnLogout);
        tvNotesEmpty = findViewById(R.id.tvNotesEmpty);

        // RecyclerView setup
        RecyclerView recyclerView = findViewById(R.id.rvNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        notesAdapter = new NotesAdapter(
            note -> {
                Intent intent = new Intent(NotesListActivity.this, NoteDetailActivity.class);
                intent.putExtra(NoteDetailActivity.EXTRA_NOTE_ID, note.getId());
                startActivity(intent);
            },
            this::showDeleteDialog
        );
        
        recyclerView.setAdapter(notesAdapter);

        // Real-time search listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnNewScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, CaptureActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            firebaseAuth.signOut();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());
            googleSignInClient.revokeAccess();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        if (savedInstanceState != null) {
            etSearch.setText(savedInstanceState.getString(STATE_QUERY, ""));
        }
    }

    private void showDeleteDialog(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    notesRepository.deleteNote(note.getId());
                    loadNotes();
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        loadNotes();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, etSearch.getText().toString());
    }

    private void loadNotes() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }
        String query = etSearch.getText().toString().trim();
        List<Note> notes = notesRepository.getNotesForUser(user.getUid(), query);
        notesAdapter.setNotes(notes);

        if (notes.isEmpty()) {
            tvNotesEmpty.setVisibility(View.VISIBLE);
            if (query.isEmpty()) {
                tvNotesEmpty.setText("No notes yet. Tap New Scan to create your first one.");
            } else {
                tvNotesEmpty.setText("No notes found for this search.");
            }
        } else {
            tvNotesEmpty.setVisibility(View.GONE);
        }
    }
}
