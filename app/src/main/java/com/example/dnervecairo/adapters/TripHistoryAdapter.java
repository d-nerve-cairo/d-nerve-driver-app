package com.example.dnervecairo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.api.responses.TripResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
        this.trips = trips != null ? trips : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addTrips(List<TripResponse> newTrips) {
        if (newTrips == null || newTrips.isEmpty()) return;
        int startPos = trips.size();
        trips.addAll(newTrips);
        notifyItemRangeInserted(startPos, newTrips.size());
    }

    public void clearTrips() {
        trips.clear();
        notifyDataSetChanged();
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
        // Route badge
        private final LinearLayout layoutRouteBadge;
        private final TextView tvRouteName;
        private final TextView tvManualBadge;
        
        // Stats
        private final TextView tvDate;
        private final TextView tvDuration;
        private final TextView tvDistance;
        private final TextView tvPoints;
        private final TextView tvQuality;

        TripViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Route badge views
            layoutRouteBadge = itemView.findViewById(R.id.layout_route_badge);
            tvRouteName = itemView.findViewById(R.id.tv_route_name);
            tvManualBadge = itemView.findViewById(R.id.tv_manual_badge);
            
            // Stats views
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvPoints = itemView.findViewById(R.id.tv_points);
            tvQuality = itemView.findViewById(R.id.tv_quality);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    // Add click animation
                    v.animate()
                        .scaleX(0.98f).scaleY(0.98f)
                        .setDuration(50)
                        .withEndAction(() -> {
                            v.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(50)
                                .start();
                            listener.onTripClick(trips.get(getAdapterPosition()));
                        }).start();
                }
            });
        }

        void bind(TripResponse trip) {
            // Route name badge
            String routeName = trip.getRouteName();
            if (routeName != null && !routeName.isEmpty()) {
                if (layoutRouteBadge != null) {
                    layoutRouteBadge.setVisibility(View.VISIBLE);
                }
                if (tvRouteName != null) {
                    tvRouteName.setText(routeName);
                }
                if (tvManualBadge != null) {
                    tvManualBadge.setVisibility(View.GONE);
                }
            } else {
                if (layoutRouteBadge != null) {
                    layoutRouteBadge.setVisibility(View.GONE);
                }
                if (tvManualBadge != null) {
                    tvManualBadge.setVisibility(View.VISIBLE);
                }
            }

            // Format date - show smart date (Today, Yesterday, or time)
            String dateDisplay = formatSmartDate(trip.getStartTime());
            if (tvDate != null) {
                tvDate.setText(dateDisplay);
            }

            // Duration
            if (tvDuration != null) {
                tvDuration.setText(String.format(Locale.getDefault(), "%d min", trip.getDurationMinutes()));
            }

            // Distance
            if (tvDistance != null) {
                double distance = trip.getDistanceKm();
                if (distance < 1) {
                    tvDistance.setText(String.format(Locale.getDefault(), "%.0f m", distance * 1000));
                } else {
                    tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", distance));
                }
            }

            // Points
            if (tvPoints != null) {
                int points = trip.getPointsEarned();
                tvPoints.setText(String.format(Locale.getDefault(), "+%d", points));
            }

            // Quality score with color coding
            if (tvQuality != null) {
                double qualityRaw = trip.getQualityScore();
                int quality;
                
                // Handle both 0-1 and 0-100 formats
                if (qualityRaw <= 1.0) {
                    quality = (int) (qualityRaw * 100);
                } else {
                    quality = (int) qualityRaw;
                }
                
                tvQuality.setText(String.format(Locale.getDefault(), "%d%%", quality));

                // Set color based on quality
                int colorRes;
                if (quality >= 90) {
                    colorRes = R.color.success;
                } else if (quality >= 70) {
                    colorRes = R.color.success;
                } else if (quality >= 50) {
                    colorRes = R.color.warning;
                } else {
                    colorRes = R.color.error;
                }
                tvQuality.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
            }
        }

        /**
         * Format date smartly - shows "10:30 AM" for today, 
         * "Yesterday, 10:30 AM" for yesterday, 
         * or "Jan 15, 10:30 AM" for older dates
         */
        private String formatSmartDate(String isoDate) {
            if (isoDate == null) return "";
            
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date tripDate = inputFormat.parse(isoDate.split("\\.")[0]); // Remove milliseconds
                if (tripDate == null) return isoDate;

                Calendar tripCal = Calendar.getInstance();
                tripCal.setTime(tripDate);

                Calendar today = Calendar.getInstance();
                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DAY_OF_YEAR, -1);

                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                String timeStr = timeFormat.format(tripDate);

                // Check if today
                if (isSameDay(tripCal, today)) {
                    return timeStr;
                }
                
                // Check if yesterday
                if (isSameDay(tripCal, yesterday)) {
                    return "Yesterday, " + timeStr;
                }

                // Check if this year
                if (tripCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
                    return dateFormat.format(tripDate) + ", " + timeStr;
                }

                // Different year
                SimpleDateFormat fullFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                return fullFormat.format(tripDate);

            } catch (ParseException e) {
                return isoDate;
            }
        }

        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }
    }
}
