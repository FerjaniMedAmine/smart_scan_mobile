package com.example.smartscan;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormat;
import java.util.Date;

public class NoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "extra_note_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainNoteDetail), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvDetailTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDetailDate = findViewById(R.id.tvDetailDate);
        TextView tvDetailKeywords = findViewById(R.id.tvDetailKeywords);
        TextView tvDetailSummary = findViewById(R.id.tvDetailSummary);
        TextView tvDetailRawText = findViewById(R.id.tvDetailRawText);
        Button btnSaveNewNote = findViewById(R.id.btnSaveNewNote);

        NotesRepository repository = new NotesRepository(this);

        boolean isNewScan = getIntent().getBooleanExtra("IS_NEW_SCAN", false);
        if (isNewScan) {
            String rawText = getIntent().getStringExtra("EXTRA_RAW_TEXT");
            String summary = getIntent().getStringExtra("EXTRA_SUMMARY");
            String keywords = getIntent().getStringExtra("EXTRA_KEYWORDS");

            String title = "New Scan " + DateFormat.getDateTimeInstance().format(new Date());
            if (rawText != null && !rawText.isEmpty()) {
                title = rawText.split("\\n")[0].trim();
                if (title.length() > 40) title = title.substring(0, 40) + "...";
            }

            tvDetailTitle.setText(title);
            tvDetailDate.setText(DateFormat.getDateTimeInstance().format(new Date()));
            tvDetailKeywords.setText("Keywords: " + (keywords != null ? keywords : ""));
            tvDetailSummary.setText(summary);
            tvDetailRawText.setText(rawText);

            btnSaveNewNote.setVisibility(View.VISIBLE);
            btnSaveNewNote.setOnClickListener(v -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    repository.insertNote(user.getUid(), tvDetailTitle.getText().toString(), rawText, summary, keywords);
                    Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            long noteId = getIntent().getLongExtra(EXTRA_NOTE_ID, -1);
            if (noteId == -1) {
                finish();
                return;
            }

            Note note = repository.getNoteById(noteId);
            if (note == null) {
                finish();
                return;
            }

            tvDetailTitle.setText(note.getTitle());
            tvDetailDate.setText(DateFormat.getDateTimeInstance().format(new Date(note.getCreatedAt())));
            tvDetailKeywords.setText("Keywords: " + note.getKeywords());
            tvDetailSummary.setText(note.getSummary());
            tvDetailRawText.setText(note.getRawText());
            btnSaveNewNote.setVisibility(View.GONE);
        }
    }
}
