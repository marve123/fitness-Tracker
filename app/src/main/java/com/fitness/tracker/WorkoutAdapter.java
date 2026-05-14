package com.fitness.tracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * WorkoutAdapter binds a list of Workout objects to RecyclerView cards
 * on the Main Dashboard. Exposes a delete callback for swipe/button removal.
 */
public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    private final Context context;
    private final List<Workout> workouts;
    private final OnDeleteListener listener;

    // ── Delete callback ───────────────────────────────────────────────────
    public interface OnDeleteListener {
        void onDelete(int workoutId, int position);
    }

    // ── Constructor ───────────────────────────────────────────────────────
    public WorkoutAdapter(Context context, List<Workout> workouts,
                          OnDeleteListener listener) {
        this.context   = context;
        this.workouts  = workouts;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Workout w = workouts.get(position);

        h.tvType.setText(w.getWorkoutType());
        h.tvDuration.setText("⏱ " + w.getDuration() + " min");
        h.tvCalories.setText("🔥 " + String.format("%.1f", w.getCaloriesBurned()) + " kcal");
        h.tvDate.setText(w.getDate());
        h.tvIntensity.setText(w.getIntensity());

        // Colour-code the intensity badge
        int colour;
        switch (w.getIntensity()) {
            case "High":   colour = context.getResources().getColor(R.color.intensity_high);   break;
            case "Medium": colour = context.getResources().getColor(R.color.intensity_medium); break;
            default:       colour = context.getResources().getColor(R.color.intensity_low);    break;
        }
        h.tvIntensity.setTextColor(colour);

        // Delete button — shows confirmation dialog via callback
        h.btnDelete.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && listener != null) {
                listener.onDelete(w.getId(), pos);
            }
        });
    }

    @Override
    public int getItemCount() { return workouts.size(); }

    /**
     * Removes item from the local list and triggers animated removal.
     * Call this after the DB delete is confirmed.
     */
    public void removeItem(int position) {
        workouts.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, workouts.size());
    }

    // ── ViewHolder ────────────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvType, tvDuration, tvCalories, tvIntensity, tvDate;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType      = itemView.findViewById(R.id.tv_workout_type);
            tvDuration  = itemView.findViewById(R.id.tv_duration);
            tvCalories  = itemView.findViewById(R.id.tv_calories);
            tvIntensity = itemView.findViewById(R.id.tv_intensity);
            tvDate      = itemView.findViewById(R.id.tv_date);
            btnDelete   = itemView.findViewById(R.id.btn_delete);
        }
    }
}
