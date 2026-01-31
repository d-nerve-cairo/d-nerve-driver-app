package com.example.dnervecairo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.api.responses.TripResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.TripViewHolder> {

    private List<TripResponse> trips = new ArrayList<>();
    private OnTripClickListener listener;

    public interface OnTripClickListener {
        void onTripClick(TripResponse trip);
    }

    public void setOnTripClickListener(OnTripClickListener listener) {
        this.listener = listener;
    }

    public void setTrips(List<TripResponse> trips) {
        this.trips = trips;
        notifyDataSetChanged();
    }

    public void addTrips(List<TripResponse> newTrips) {
        int startPos = trips.size();
        trips.addAll(newTrips);
        notifyItemRangeInserted(startPos, newTrips.size());
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_history, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        TripResponse trip = trips.get(position);
        holder.bind(trip);
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    class TripViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate;
        private final TextView tvTime;
        private final TextView tvDistance;
        private final TextView tvDuration;
        private final TextView tvPoints;
        private final TextView tvQuality;

        TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_trip_date);
            tvTime = itemView.findViewById(R.id.tv_trip_time);
            tvDistance = itemView.findViewById(R.id.tv_trip_distance);
            tvDuration = itemView.findViewById(R.id.tv_trip_duration);
            tvPoints = itemView.findViewById(R.id.tv_trip_points);
            tvQuality = itemView.findViewById(R.id.tv_trip_quality);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onTripClick(trips.get(getAdapterPosition()));
                }
            });
        }

        void bind(TripResponse trip) {
            // Format date and time
            String dateStr = formatDate(trip.getStartTime());
            String timeStr = formatTime(trip.getStartTime());

            tvDate.setText(dateStr);
            tvTime.setText(timeStr);
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", trip.getDistanceKm()));
            tvDuration.setText(String.format(Locale.getDefault(), "%d min", trip.getDurationMinutes()));
            tvPoints.setText(String.format(Locale.getDefault(), "+%d", trip.getPointsEarned()));

            // Quality score with color
            int quality = (int) (trip.getQualityScore() * 100);
            tvQuality.setText(String.format(Locale.getDefault(), "%d%%", quality));

            if (quality >= 90) {
                tvQuality.setTextColor(itemView.getContext().getResources().getColor(R.color.quality_excellent, null));
            } else if (quality >= 70) {
                tvQuality.setTextColor(itemView.getContext().getResources().getColor(R.color.quality_good, null));
            } else if (quality >= 50) {
                tvQuality.setTextColor(itemView.getContext().getResources().getColor(R.color.quality_fair, null));
            } else {
                tvQuality.setTextColor(itemView.getContext().getResources().getColor(R.color.quality_poor, null));
            }
        }

        private String formatDate(String isoDate) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(isoDate);
                return outputFormat.format(date);
            } catch (ParseException e) {
                return isoDate;
            }
        }

        private String formatTime(String isoDate) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Date date = inputFormat.parse(isoDate);
                return outputFormat.format(date);
            } catch (ParseException e) {
                return "";
            }
        }
    }
}