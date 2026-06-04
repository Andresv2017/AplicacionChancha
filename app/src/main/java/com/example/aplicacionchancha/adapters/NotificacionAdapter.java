package com.example.aplicacionchancha.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.R;
import com.google.gson.JsonObject;

import java.util.List;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.ViewHolder> {

    private final List<JsonObject> notificaciones;

    public NotificacionAdapter(List<JsonObject> notificaciones) {
        this.notificaciones = notificaciones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notificacion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject n = notificaciones.get(position);

        String mensaje = n.get("mensaje").getAsString();
        String fecha   = n.has("fecha") && !n.get("fecha").isJsonNull()
                          ? n.get("fecha").getAsString() : "";
        boolean leida  = n.get("leida").getAsInt() == 1;

        holder.tvMensaje.setText(mensaje);
        holder.tvFecha.setText(fecha.length() > 10 ? fecha.substring(0, 16) : fecha);

        // El dot se muestra solo para no leídas
        holder.viewDot.setVisibility(leida ? View.INVISIBLE : View.VISIBLE);

        // Fondo levemente diferente para no leídas
        holder.itemView.setAlpha(leida ? 0.75f : 1.0f);
    }

    @Override
    public int getItemCount() { return notificaciones.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvFecha;
        View     viewDot;

        ViewHolder(View v) {
            super(v);
            tvMensaje = v.findViewById(R.id.tvMensaje);
            tvFecha   = v.findViewById(R.id.tvFecha);
            viewDot   = v.findViewById(R.id.viewDot);
        }
    }
}
