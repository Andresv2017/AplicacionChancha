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

public class ColectaAdapter extends RecyclerView.Adapter<ColectaAdapter.ViewHolder> {

    public interface OnClickListener {
        void onClick(JsonObject colecta);
    }

    private final List<JsonObject> colectas;
    private final OnClickListener  clickListener;

    public ColectaAdapter(List<JsonObject> colectas, OnClickListener clickListener) {
        this.colectas      = colectas;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_colecta, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject c   = colectas.get(position);
        Context    ctx = holder.itemView.getContext();

        String descripcion     = c.get("descripcion").getAsString();
        double montoPorPersona = c.get("monto_por_persona").getAsDouble();
        String estado          = c.get("estado").getAsString();
        int    totalMiembros   = c.has("total_miembros") && !c.get("total_miembros").isJsonNull()
                                  ? c.get("total_miembros").getAsInt() : 0;
        int    pagados         = c.has("pagados") && !c.get("pagados").isJsonNull()
                                  ? c.get("pagados").getAsInt() : 0;
        String miEstado        = c.has("mi_estado") && !c.get("mi_estado").isJsonNull()
                                  ? c.get("mi_estado").getAsString() : "pendiente";

        holder.tvIcono.setText(String.valueOf(descripcion.charAt(0)).toUpperCase());
        holder.tvDescripcion.setText(descripcion);
        holder.tvMonto.setText("S/. " + String.format("%.2f", montoPorPersona) + " / persona");
        holder.tvProgreso.setText(pagados + " de " + totalMiembros + " pagaron");

        // Estado de la colecta
        if ("activa".equals(estado)) {
            holder.tvEstado.setText("Activa");
            holder.tvEstado.setTextColor(ctx.getColor(R.color.success_green));
        } else {
            holder.tvEstado.setText("Cerrada");
            holder.tvEstado.setTextColor(ctx.getColor(R.color.gray_text));
        }

        // Mi estado personal
        switch (miEstado) {
            case "pagado":
                holder.tvMiEstado.setText("Pagado ✓");
                holder.tvMiEstado.setTextColor(ctx.getColor(R.color.colorPrimary));
                break;
            case "confirmado":
                holder.tvMiEstado.setText("Confirmado ✓");
                holder.tvMiEstado.setTextColor(ctx.getColor(R.color.success_green));
                break;
            default: // pendiente
                holder.tvMiEstado.setText("Pendiente");
                holder.tvMiEstado.setTextColor(ctx.getColor(R.color.error_red));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(c);
        });
    }

    @Override
    public int getItemCount() { return colectas.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcono, tvDescripcion, tvMonto, tvProgreso, tvEstado, tvMiEstado;

        ViewHolder(View v) {
            super(v);
            tvIcono       = v.findViewById(R.id.tvIconoColecta);
            tvDescripcion = v.findViewById(R.id.tvDescripcionColecta);
            tvMonto       = v.findViewById(R.id.tvMontoColecta);
            tvProgreso    = v.findViewById(R.id.tvProgresoColecta);
            tvEstado      = v.findViewById(R.id.tvEstadoColecta);
            tvMiEstado    = v.findViewById(R.id.tvMiEstadoColecta);
        }
    }
}
