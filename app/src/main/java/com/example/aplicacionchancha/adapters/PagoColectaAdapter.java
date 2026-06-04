package com.example.aplicacionchancha.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.R;
import com.google.gson.JsonObject;

import java.util.List;

public class PagoColectaAdapter extends RecyclerView.Adapter<PagoColectaAdapter.ViewHolder> {

    public interface OnConfirmarListener {
        void onConfirmar(int usuarioId);
    }

    private final List<JsonObject>    partes;
    private final boolean             esAdmin;
    private final OnConfirmarListener confirmarListener;

    public PagoColectaAdapter(List<JsonObject> partes, boolean esAdmin,
                               OnConfirmarListener confirmarListener) {
        this.partes            = partes;
        this.esAdmin           = esAdmin;
        this.confirmarListener = confirmarListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pago_colecta, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject p   = partes.get(position);
        Context    ctx = holder.itemView.getContext();

        int    usuarioId  = p.get("usuario_id").getAsInt();
        String nombre     = p.get("nombre").getAsString();
        String estadoPago = p.get("estado_pago").getAsString();
        String metodo     = p.has("metodo_pago") && !p.get("metodo_pago").isJsonNull()
                             ? p.get("metodo_pago").getAsString() : null;

        holder.tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        holder.tvNombre.setText(nombre);

        // Mostrar método de pago si está disponible
        if (metodo != null && !metodo.isEmpty()) {
            holder.tvMetodo.setVisibility(View.VISIBLE);
            holder.tvMetodo.setText("Vía: " + metodo);
        } else {
            holder.tvMetodo.setVisibility(View.GONE);
        }

        // Estado con color
        switch (estadoPago) {
            case "pagado":
                holder.tvEstado.setText("Pagado");
                holder.tvEstado.setTextColor(ctx.getColor(R.color.colorPrimary));
                // Admin puede confirmar pagos con estado "pagado"
                if (esAdmin) {
                    holder.btnConfirmar.setVisibility(View.VISIBLE);
                    holder.btnConfirmar.setOnClickListener(v -> {
                        if (confirmarListener != null) confirmarListener.onConfirmar(usuarioId);
                    });
                } else {
                    holder.btnConfirmar.setVisibility(View.GONE);
                }
                break;
            case "confirmado":
                holder.tvEstado.setText("Confirmado ✓");
                holder.tvEstado.setTextColor(ctx.getColor(R.color.success_green));
                holder.btnConfirmar.setVisibility(View.GONE);
                break;
            default: // pendiente
                holder.tvEstado.setText("Pendiente");
                holder.tvEstado.setTextColor(ctx.getColor(R.color.error_red));
                holder.btnConfirmar.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() { return partes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvNombre, tvMetodo, tvEstado;
        Button   btnConfirmar;

        ViewHolder(View v) {
            super(v);
            tvAvatar      = v.findViewById(R.id.tvAvatarPago);
            tvNombre      = v.findViewById(R.id.tvNombrePago);
            tvMetodo      = v.findViewById(R.id.tvMetodoPago);
            tvEstado      = v.findViewById(R.id.tvEstadoPago);
            btnConfirmar  = v.findViewById(R.id.btnConfirmar);
        }
    }
}
