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

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    public interface OnGroupClickListener {
        void onClick(JsonObject grupo);
    }

    private final List<JsonObject> grupos;
    private final OnGroupClickListener listener;

    public GroupAdapter(List<JsonObject> grupos, OnGroupClickListener listener) {
        this.grupos   = grupos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grupo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject grupo = grupos.get(position);

        String nombre = grupo.get("nombre").getAsString();
        holder.tvNombre.setText(nombre);
        holder.tvInicial.setText(String.valueOf(nombre.charAt(0)).toUpperCase());

        int miembros = grupo.has("num_miembros") ? grupo.get("num_miembros").getAsInt() : 0;
        holder.tvMiembros.setText(miembros + " miembros");

        double debo    = grupo.has("total_debo")     ? grupo.get("total_debo").getAsDouble()     : 0;
        double meDeben = grupo.has("total_me_deben") ? grupo.get("total_me_deben").getAsDouble() : 0;

        Context ctx = holder.itemView.getContext();
        if (debo > 0.01) {
            holder.tvBalance.setText("Debes S/. " + String.format("%.2f", debo));
            holder.tvBalance.setTextColor(ctx.getColor(R.color.error_red));
        } else if (meDeben > 0.01) {
            holder.tvBalance.setText("Te deben S/. " + String.format("%.2f", meDeben));
            holder.tvBalance.setTextColor(ctx.getColor(R.color.success_green));
        } else {
            holder.tvBalance.setText("Sin deudas");
            holder.tvBalance.setTextColor(ctx.getColor(R.color.gray_text));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(grupo));
    }

    @Override
    public int getItemCount() { return grupos.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInicial, tvNombre, tvMiembros, tvBalance;

        ViewHolder(View v) {
            super(v);
            tvInicial  = v.findViewById(R.id.tvInicialGrupo);
            tvNombre   = v.findViewById(R.id.tvNombreGrupo);
            tvMiembros = v.findViewById(R.id.tvMiembros);
            tvBalance  = v.findViewById(R.id.tvBalance);
        }
    }
}
