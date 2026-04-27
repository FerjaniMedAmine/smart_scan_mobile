package com.example.smartscan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(Note note);
    }

    private final List<Note> notes = new ArrayList<>();
    private final OnNoteClickListener clickListener;
    private final OnNoteLongClickListener longClickListener;

    public NotesAdapter(OnNoteClickListener clickListener, OnNoteLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setNotes(List<Note> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvItemTitle.setText(note.getTitle());
        holder.tvItemSummary.setText(note.getSummary());
        holder.tvItemDate.setText(DateFormat.getDateTimeInstance().format(new Date(note.getCreatedAt())));
        
        holder.itemView.setOnClickListener(v -> clickListener.onNoteClick(note));
        
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onNoteLongClick(note);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemTitle;
        TextView tvItemSummary;
        TextView tvItemDate;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
            tvItemSummary = itemView.findViewById(R.id.tvItemSummary);
            tvItemDate = itemView.findViewById(R.id.tvItemDate);
        }
    }
}
