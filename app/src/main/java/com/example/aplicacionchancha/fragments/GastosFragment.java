package com.example.aplicacionchancha.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.EditarGastoActivity;
import com.example.aplicacionchancha.R;
import com.example.aplicacionchancha.adapters.GastoAdapter;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GastosFragment extends Fragment {

    private static final String ARG_GRUPO_ID = "grupo_id";
    private static final String ARG_ES_ADMIN = "es_admin";

    private RecyclerView rvGastos;
    private TextView     tvSinGastos;
    private int          grupoId;
    private boolean      esAdmin;

    public static GastosFragment newInstance(int grupoId, boolean esAdmin) {
        GastosFragment f = new GastosFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_GRUPO_ID, grupoId);
        args.putBoolean(ARG_ES_ADMIN, esAdmin);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gastos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvGastos    = view.findViewById(R.id.rvGastos);
        tvSinGastos = view.findViewById(R.id.tvSinGastos);
        rvGastos.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (getArguments() != null) {
            grupoId = getArguments().getInt(ARG_GRUPO_ID);
            esAdmin = getArguments().getBoolean(ARG_ES_ADMIN, false);
        }
        cargarGastos();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarGastos();
    }

    private void cargarGastos() {
        if (!isAdded()) return;
        SessionManager session = new SessionManager(requireContext());

        ApiClient.getService().listarGastos(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            JsonArray arr = response.body().getAsJsonArray("gastos");
                            List<JsonObject> lista = new ArrayList<>();
                            for (int i = 0; i < arr.size(); i++) {
                                lista.add(arr.get(i).getAsJsonObject());
                            }
                            if (lista.isEmpty()) {
                                rvGastos.setVisibility(View.GONE);
                                tvSinGastos.setVisibility(View.VISIBLE);
                            } else {
                                tvSinGastos.setVisibility(View.GONE);
                                rvGastos.setVisibility(View.VISIBLE);
                                rvGastos.setAdapter(new GastoAdapter(
                                        lista,
                                        session.getUserId(),
                                        esAdmin,
                                        gasto -> {
                                            Intent intent = new Intent(requireContext(),
                                                    EditarGastoActivity.class);
                                            intent.putExtra("gasto_id",
                                                    gasto.get("id").getAsInt());
                                            intent.putExtra("grupo_id", grupoId);
                                            startActivity(intent);
                                        }
                                ));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }
}
