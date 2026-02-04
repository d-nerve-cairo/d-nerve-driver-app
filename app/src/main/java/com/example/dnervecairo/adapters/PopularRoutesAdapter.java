package com.example.dnervecairo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.models.PopularRoute;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class PopularRoutesAdapter extends RecyclerView.Adapter<PopularRoutesAdapter.RouteViewHolder> {

    private final List<PopularRoute> routes;
    private final OnRouteClickListener listener;
    private int selectedPosition = -1;

    public interface OnRouteClickListener {
        void onRouteClick(PopularRoute route);
    }

    public PopularRoutesAdapter(List<PopularRoute> routes, OnRouteClickListener listener) {
        this.routes = routes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_popular_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        PopularRoute route = routes.get(position);
        holder.bind(route, position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            listener.onRouteClick(route);
        });
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvRouteName;
        private final TextView tvDuration;
        private final TextView tvPopularity;
        private final LinearProgressIndicator progressPopularity;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvRouteName = itemView.findViewById(R.id.tv_route_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvPopularity = itemView.findViewById(R.id.tv_popularity);
            progressPopularity = itemView.findViewById(R.id.progress_popularity);
        }

        void bind(PopularRoute route, boolean isSelected) {
            tvRouteName.setText(route.getDisplayName());
            tvDuration.setText("~" + route.getEstimatedMinutes() + " min");
            tvPopularity.setText(route.getPopularity() + "% popular");
            progressPopularity.setProgress(route.getPopularity());

            if (isSelected) {
                cardView.setStrokeWidth(4);
                cardView.setStrokeColor(itemView.getContext().getResources()
                        .getColor(R.color.primary, null));
            } else {
                cardView.setStrokeWidth(0);
            }
        }
    }
}