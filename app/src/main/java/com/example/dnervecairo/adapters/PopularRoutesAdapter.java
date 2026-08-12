package com.example.dnervecairo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.models.PopularRoute;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying routes with section headers and smart matching
 */
public class PopularRoutesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ROUTE = 1;
    private static final int VIEW_TYPE_EMPTY = 2;

    private final List<Object> items = new ArrayList<>();
    private final OnRouteClickListener listener;
    private int selectedPosition = -1;

    public interface OnRouteClickListener {
        void onRouteClick(PopularRoute route);
    }

    public PopularRoutesAdapter(List<PopularRoute> routes, OnRouteClickListener listener) {
        this.listener = listener;
        updateItems(routes);
    }
    
    /**
     * Update the adapter with new routes (without sections)
     */
    public void updateItems(List<PopularRoute> routes) {
        items.clear();
        for (PopularRoute route : routes) {
            items.add(route);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Update with sectioned routes
     */
    public void updateWithSections(List<PopularRoute> exactMatches, 
                                    List<PopularRoute> nearbyRoutes,
                                    String driverLocationName) {
        items.clear();
        
        // Section 1: From Your Location
        if (!exactMatches.isEmpty()) {
            items.add(new SectionHeader(
                "🎯 From " + (driverLocationName != null ? driverLocationName : "Your Location"),
                exactMatches.size() + " routes starting here"
            ));
            items.addAll(exactMatches);
        }
        
        // Section 2: Nearby Routes
        if (!nearbyRoutes.isEmpty()) {
            items.add(new SectionHeader(
                "📍 Nearby Routes",
                nearbyRoutes.size() + " routes within 5 km"
            ));
            items.addAll(nearbyRoutes);
        }
        
        // Empty state if no routes at all
        if (exactMatches.isEmpty() && nearbyRoutes.isEmpty()) {
            items.add(new EmptyState());
        }
        
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof SectionHeader) {
            return VIEW_TYPE_HEADER;
        } else if (item instanceof EmptyState) {
            return VIEW_TYPE_EMPTY;
        }
        return VIEW_TYPE_ROUTE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_route_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_EMPTY) {
            View view = inflater.inflate(R.layout.item_route_empty_state, parent, false);
            return new EmptyViewHolder(view);
        }
        
        View view = inflater.inflate(R.layout.item_popular_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((SectionHeader) item);
        } else if (holder instanceof EmptyViewHolder) {
            // Empty state - no binding needed
        } else if (holder instanceof RouteViewHolder) {
            PopularRoute route = (PopularRoute) item;
            ((RouteViewHolder) holder).bind(route, position == selectedPosition);

            holder.itemView.setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                if (previousSelected != -1) {
                    notifyItemChanged(previousSelected);
                }
                notifyItemChanged(selectedPosition);

                // Animate click
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80));

                listener.onRouteClick(route);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    public void clearSelection() {
        int prev = selectedPosition;
        selectedPosition = -1;
        if (prev != -1) {
            notifyItemChanged(prev);
        }
    }

    // =========================================================================
    // VIEW HOLDERS
    // =========================================================================

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvRouteName;
        private final TextView tvDuration;
        private final TextView tvPopularity;
        private final TextView tvDistanceFromDriver;
        private final TextView tvMatchBadge;
        private final ImageView ivRouteIcon;
        private final LinearProgressIndicator progressPopularity;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvRouteName = itemView.findViewById(R.id.tv_route_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvPopularity = itemView.findViewById(R.id.tv_popularity);
            tvDistanceFromDriver = itemView.findViewById(R.id.tv_distance_from_driver);
            tvMatchBadge = itemView.findViewById(R.id.tv_match_badge);
            ivRouteIcon = itemView.findViewById(R.id.iv_route_icon);
            progressPopularity = itemView.findViewById(R.id.progress_popularity);
        }

        void bind(PopularRoute route, boolean isSelected) {
            tvRouteName.setText(route.getDisplayName());
            tvDuration.setText("~" + route.getEstimatedMinutes() + " min");
            
            // Show popularity
            if (route.getPopularity() > 50) {
                tvPopularity.setText("🔥 Popular");
            } else {
                tvPopularity.setText(route.getPopularity() + " trips");
            }
            
            if (progressPopularity != null) {
                progressPopularity.setProgress(Math.min(route.getPopularity(), 100));
            }
            
            // Show distance to pickup
            if (tvDistanceFromDriver != null) {
                String distText = route.getFormattedDistanceToPickup();
                if (!distText.isEmpty()) {
                    tvDistanceFromDriver.setText(distText);
                    tvDistanceFromDriver.setVisibility(View.VISIBLE);
                    
                    // Color based on match type
                    int color;
                    if (route.isExactMatch()) {
                        color = ContextCompat.getColor(itemView.getContext(), R.color.success);
                    } else {
                        color = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
                    }
                    tvDistanceFromDriver.setTextColor(color);
                } else {
                    tvDistanceFromDriver.setVisibility(View.GONE);
                }
            }
            
            // Show match badge for exact matches
            if (tvMatchBadge != null) {
                if (route.isExactMatch()) {
                    tvMatchBadge.setVisibility(View.VISIBLE);
                    tvMatchBadge.setText("✓ Best Match");
                } else {
                    tvMatchBadge.setVisibility(View.GONE);
                }
            }
            
            // Route icon color for exact match
            if (ivRouteIcon != null) {
                int iconColor = route.isExactMatch() 
                    ? ContextCompat.getColor(itemView.getContext(), R.color.success)
                    : ContextCompat.getColor(itemView.getContext(), R.color.primary);
                ivRouteIcon.setColorFilter(iconColor);
            }

            // Selection state
            if (isSelected) {
                cardView.setStrokeWidth(4);
                cardView.setStrokeColor(ContextCompat.getColor(
                    itemView.getContext(), R.color.primary));
                cardView.setCardBackgroundColor(ContextCompat.getColor(
                    itemView.getContext(), R.color.primary_light));
            } else {
                // Highlight exact matches subtly
                if (route.isExactMatch()) {
                    cardView.setStrokeWidth(2);
                    cardView.setStrokeColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.success));
                    cardView.setCardBackgroundColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.surface));
                } else {
                    cardView.setStrokeWidth(0);
                    cardView.setCardBackgroundColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.surface));
                }
            }
        }
    }
    
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvSubtitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_section_title);
            tvSubtitle = itemView.findViewById(R.id.tv_section_subtitle);
        }

        void bind(SectionHeader header) {
            tvTitle.setText(header.title);
            if (tvSubtitle != null && header.subtitle != null) {
                tvSubtitle.setText(header.subtitle);
                tvSubtitle.setVisibility(View.VISIBLE);
            } else if (tvSubtitle != null) {
                tvSubtitle.setVisibility(View.GONE);
            }
        }
    }
    
    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // =========================================================================
    // DATA CLASSES
    // =========================================================================
    
    public static class SectionHeader {
        public final String title;
        public final String subtitle;

        public SectionHeader(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }
    
    public static class EmptyState {
        // Marker class for empty state
    }
}
