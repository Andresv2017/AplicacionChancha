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

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    private final List<JsonObject> miembros;
    private final int usuarioActualId;

    public MemberAdapter(List<JsonObject> miembros, int usuarioActualId) {
        this.miembros        = miembros;
        this.usuarioActualId = usuarioActualId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_miembro, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject m = miembros.get(position);

        String nombre = m.get("nombre").getAsString();
        int    id     = m.get("id").getAsInt();
        boolean esCreador = m.has("es_creador") && m.get("es_creador").getAsInt() == 1;

        holder.tvNombre.setText(nombre + (id == usuarioActualId ? " (tú)" : ""));
        holder.tvCorreo.setText(m.get("correo").getAsString());
        holder.tvRol.setText(esCreador ? "Admin" : "Miembro");
        holder.tvRol.setVisibility(esCreador ? View.VISIBLE : View.GONE);

        holder.tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
    }

    @Override
    public int getItemCount() { return miembros.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvNombre, tvCorreo, tvRol;

        ViewHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvAvatar);
            tvNombre = v.findViewById(R.id.tvNombreMiembro);
            tvCorreo = v.findViewById(R.id.tvCorreoMiembro);
            tvRol    = v.findViewById(R.id.tvRol);
        }
    }
}
