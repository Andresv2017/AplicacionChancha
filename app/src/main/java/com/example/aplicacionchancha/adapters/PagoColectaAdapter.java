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

    public interface OnAccionListener {
        void onVerComprobante(int usuarioId, String comprobante);
    }

    private final List<JsonObject> partes;
    private final boolean          esAdmin;
    private final OnAccionListener accionListener;

    public PagoColectaAdapter(List<JsonObject> partes, boolean esAdmin,
                               OnAccionListener accionListener) {
        this.partes         = partes;
        this.esAdmin        = esAdmin;
        this.accionListener = accionListener;
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

        int    usuarioId   = p.get("usuario_id").getAsInt();
        String nombre      = p.get("nombre").getAsString();
        String estadoPago  = p.get("estado_pago").getAsString();
        String comprobante = (p.has("comprobante") && !p.get("comprobante").isJsonNull())
                ? p.get("comprobante").getAsString() : null;

        holder.tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        holder.tvNombre.setText(nombre);
        holder.tvMetodo.setVisibility(View.GONE);
        holder.btnConfirmar.setVisibility(View.GONE);

        switch (estadoPago) {
            case "en_revision":
                holder.tvEstado.setText("Comprobante enviado");
                holder.tvEstado.setTextColor(ctx.getColor(R.color.colorPrimary));
                if (esAdmin && comprobante != null) {
                    holder.btnConfirmar.setVisibility(View.VISIBLE);
                    holder.btnConfirmar.setText("Ver comprobante");
                    holder.btnConfirmar.setOnClickListener(v -> {
                        if (accionListener != null)
                            accionListener.onVerComprobante(usuarioId, comprobante);
                    });
                }
                break;

            case "confirmado":
                holder.tvEstado.setText("Confirmado ✓");
                holder.tvEstado.setTextColor(ctx.getColor(R.color.success_green));
                break;

            default: // pendiente o pagado legado
                holder.tvEstado.setText("Pendiente");
                holder.tvEstado.setTextColor(ctx.getColor(R.color.error_red));
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
            tvAvatar     = v.findViewById(R.id.tvAvatarPago);
            tvNombre     = v.findViewById(R.id.tvNombrePago);
            tvMetodo     = v.findViewById(R.id.tvMetodoPago);
            tvEstado     = v.findViewById(R.id.tvEstadoPago);
            btnConfirmar = v.findViewById(R.id.btnConfirmar);
        }
    }
}
