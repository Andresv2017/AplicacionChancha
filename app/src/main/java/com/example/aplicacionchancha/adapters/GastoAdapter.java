package com.example.aplicacionchancha.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.R;
import com.google.gson.JsonObject;

import java.util.List;

public class GastoAdapter extends RecyclerView.Adapter<GastoAdapter.ViewHolder> {

    public interface OnEditListener {
        void onEdit(JsonObject gasto);
    }

    private final List<JsonObject> gastos;
    private final int              usuarioActualId;
    private final boolean          esAdmin;
    private final OnEditListener   editListener;

    public GastoAdapter(List<JsonObject> gastos, int usuarioActualId,
                        boolean esAdmin, OnEditListener editListener) {
        this.gastos          = gastos;
        this.usuarioActualId = usuarioActualId;
        this.esAdmin         = esAdmin;
        this.editListener    = editListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gasto, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject g   = gastos.get(position);
        Context    ctx = holder.itemView.getContext();

        String descripcion = g.get("descripcion").getAsString();
        double montoTotal  = g.get("monto_total").getAsDouble();
        int    pagadoPorId = g.get("pagado_por_id").getAsInt();
        String pagadoPor   = g.get("pagado_por_nombre").getAsString();
        String fecha       = g.has("fecha") && !g.get("fecha").isJsonNull()
                             ? g.get("fecha").getAsString() : "";

        holder.tvIcono.setText(String.valueOf(descripcion.charAt(0)).toUpperCase());
        holder.tvDescripcion.setText(descripcion);
        holder.tvMontoTotal.setText("S/. " + String.format("%.2f", montoTotal));
        holder.tvFecha.setText(fecha);
        holder.tvPagadoPor.setText(pagadoPorId == usuarioActualId ? "Tú pagaste" : "Pagó: " + pagadoPor);

        // Mi parte
        if (g.has("mi_parte") && !g.get("mi_parte").isJsonNull()) {
            double miParte  = g.get("mi_parte").getAsDouble();
            String miEstado = g.get("mi_estado").getAsString();
            if (pagadoPorId == usuarioActualId) {
                holder.tvMiParte.setText("Pagaste");
                holder.tvMiParte.setTextColor(ctx.getColor(R.color.success_green));
            } else if ("pendiente".equals(miEstado)) {
                holder.tvMiParte.setText("Debes S/. " + String.format("%.2f", miParte));
                holder.tvMiParte.setTextColor(ctx.getColor(R.color.error_red));
            } else {
                holder.tvMiParte.setText("Pagado");
                holder.tvMiParte.setTextColor(ctx.getColor(R.color.success_green));
            }
        } else {
            holder.tvMiParte.setText("");
        }

        // Botón editar solo para admin
        if (esAdmin) {
            holder.tvEditar.setVisibility(View.VISIBLE);
            holder.tvEditar.setOnClickListener(v -> {
                if (editListener != null) editListener.onEdit(g);
            });
        } else {
            holder.tvEditar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return gastos.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcono, tvDescripcion, tvPagadoPor, tvFecha, tvMontoTotal, tvMiParte, tvEditar;

        ViewHolder(View v) {
            super(v);
            tvIcono       = v.findViewById(R.id.tvIconoGasto);
            tvDescripcion = v.findViewById(R.id.tvDescripcion);
            tvPagadoPor   = v.findViewById(R.id.tvPagadoPor);
            tvFecha       = v.findViewById(R.id.tvFecha);
            tvMontoTotal  = v.findViewById(R.id.tvMontoTotal);
            tvMiParte     = v.findViewById(R.id.tvMiParte);
            tvEditar      = v.findViewById(R.id.tvEditar);
        }
    }
}
