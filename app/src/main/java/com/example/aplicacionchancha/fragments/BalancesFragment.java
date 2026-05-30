package com.example.aplicacionchancha.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.aplicacionchancha.R;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BalancesFragment extends Fragment {

    private static final String ARG_GRUPO_ID = "grupo_id";
    private LinearLayout contenedorBalances;
    private TextView tvSinDeudas;

    public static BalancesFragment newInstance(int grupoId) {
        BalancesFragment f = new BalancesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_GRUPO_ID, grupoId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_balances, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contenedorBalances = view.findViewById(R.id.contenedorBalances);
        tvSinDeudas        = view.findViewById(R.id.tvSinDeudas);

        int grupoId = getArguments() != null ? getArguments().getInt(ARG_GRUPO_ID) : 0;
        cargarBalances(grupoId);
    }

    private void cargarBalances(int grupoId) {
        SessionManager session = new SessionManager(requireContext());

        ApiClient.getService().balancesGrupo(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            mostrarBalances(response.body().getAsJsonArray("balances"));
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void mostrarBalances(JsonArray balances) {
        contenedorBalances.removeAllViews();

        if (balances.size() == 0) {
            tvSinDeudas.setVisibility(View.VISIBLE);
            return;
        }
        tvSinDeudas.setVisibility(View.GONE);

        int px8  = dp(8);
        int px12 = dp(12);
        int px16 = dp(16);

        for (int i = 0; i < balances.size(); i++) {
            JsonObject b       = balances.get(i).getAsJsonObject();
            String deudor      = b.get("deudor").getAsString();
            String acreedor    = b.get("acreedor").getAsString();
            double monto       = b.get("monto").getAsDouble();
            boolean esMiDeuda  = b.get("es_mi_deuda").getAsBoolean();
            boolean meDeben    = b.get("me_deben").getAsBoolean();

            // Card contenedor
            com.google.android.material.card.MaterialCardView card =
                    new com.google.android.material.card.MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, px12);
            card.setLayoutParams(cardLp);
            card.setRadius(dp(12));
            card.setCardElevation(dp(2));

            // Color de fondo según estado
            if (esMiDeuda) {
                card.setCardBackgroundColor(0xFFFFF3F3); // rojo muy suave
            } else if (meDeben) {
                card.setCardBackgroundColor(0xFFF1FAF1); // verde muy suave
            } else {
                card.setCardBackgroundColor(0xFFFFFFFF);
            }

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(px16, px12, px16, px12);

            // Línea 1: etiqueta de contexto
            TextView tvTipo = new TextView(requireContext());
            tvTipo.setTextSize(11f);
            tvTipo.setTypeface(null, Typeface.BOLD);
            tvTipo.setPadding(0, 0, 0, dp(4));
            if (esMiDeuda) {
                tvTipo.setText("DEBES PAGAR");
                tvTipo.setTextColor(requireContext().getColor(R.color.error_red));
            } else if (meDeben) {
                tvTipo.setText("TE DEBEN");
                tvTipo.setTextColor(requireContext().getColor(R.color.success_green));
            } else {
                tvTipo.setText("BALANCE PENDIENTE");
                tvTipo.setTextColor(requireContext().getColor(R.color.gray_text));
            }
            inner.addView(tvTipo);

            // Línea 2: quién le debe a quién (prominente)
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Avatar deudor
            TextView tvAvatarD = crearAvatar(deudor);
            fila.addView(tvAvatarD);

            // Nombre deudor
            TextView tvDeudor = new TextView(requireContext());
            tvDeudor.setText(esMiDeuda ? "Tú" : deudor.split(" ")[0]);
            tvDeudor.setTextSize(15f);
            tvDeudor.setTypeface(null, Typeface.BOLD);
            tvDeudor.setTextColor(requireContext().getColor(R.color.colorPrimaryDark));
            tvDeudor.setPadding(px8, 0, px8, 0);
            fila.addView(tvDeudor);

            // Flecha
            TextView tvFlecha = new TextView(requireContext());
            tvFlecha.setText("→");
            tvFlecha.setTextSize(18f);
            tvFlecha.setTextColor(requireContext().getColor(R.color.gray_text));
            tvFlecha.setPadding(0, 0, px8, 0);
            fila.addView(tvFlecha);

            // Avatar acreedor
            TextView tvAvatarA = crearAvatar(acreedor);
            fila.addView(tvAvatarA);

            // Nombre acreedor
            TextView tvAcreedor = new TextView(requireContext());
            tvAcreedor.setText(meDeben ? "Tí" : acreedor.split(" ")[0]);
            tvAcreedor.setTextSize(15f);
            tvAcreedor.setTypeface(null, Typeface.BOLD);
            tvAcreedor.setTextColor(requireContext().getColor(R.color.colorPrimaryDark));
            tvAcreedor.setPadding(px8, 0, 0, 0);
            fila.addView(tvAcreedor);

            inner.addView(fila);

            // Línea 3: nombre completo + monto
            LinearLayout filaDetalle = new LinearLayout(requireContext());
            filaDetalle.setOrientation(LinearLayout.HORIZONTAL);
            filaDetalle.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams detallelp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            detallelp.setMargins(0, dp(6), 0, 0);
            filaDetalle.setLayoutParams(detallelp);

            TextView tvNombres = new TextView(requireContext());
            tvNombres.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvNombres.setText(deudor + " → " + acreedor);
            tvNombres.setTextSize(12f);
            tvNombres.setTextColor(requireContext().getColor(R.color.gray_text));
            filaDetalle.addView(tvNombres);

            TextView tvMonto = new TextView(requireContext());
            tvMonto.setText("S/. " + String.format("%.2f", monto));
            tvMonto.setTextSize(18f);
            tvMonto.setTypeface(null, Typeface.BOLD);
            tvMonto.setTextColor(requireContext().getColor(
                    esMiDeuda ? R.color.error_red : meDeben ? R.color.success_green : R.color.colorPrimaryDark));
            filaDetalle.addView(tvMonto);

            inner.addView(filaDetalle);
            card.addView(inner);
            contenedorBalances.addView(card);
        }
    }

    private TextView crearAvatar(String nombre) {
        TextView tv = new TextView(requireContext());
        int size = dp(32);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        tv.setLayoutParams(lp);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(13f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(requireContext().getColor(R.color.white));
        tv.setBackground(requireContext().getDrawable(R.drawable.bg_avatar));
        tv.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        return tv;
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
