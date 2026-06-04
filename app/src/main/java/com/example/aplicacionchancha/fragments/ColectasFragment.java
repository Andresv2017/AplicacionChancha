package com.example.aplicacionchancha.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.DetalleColectaActivity;
import com.example.aplicacionchancha.R;
import com.example.aplicacionchancha.adapters.ColectaAdapter;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ColectasFragment extends Fragment {

    private static final String ARG_GRUPO_ID = "grupo_id";
    private static final String ARG_ES_ADMIN = "es_admin";

    private RecyclerView rvColectas;
    private TextView     tvSinColectas;
    private int          grupoId;
    private boolean      esAdmin;

    // Lanzador del escáner QR (Activity Result API)
    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String codigoQr = result.getContents();
                    Intent intent = new Intent(requireContext(), DetalleColectaActivity.class);
                    intent.putExtra("codigo_qr", codigoQr);
                    startActivity(intent);
                }
            });

    public static ColectasFragment newInstance(int grupoId, boolean esAdmin) {
        ColectasFragment f = new ColectasFragment();
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
        return inflater.inflate(R.layout.fragment_colectas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvColectas    = view.findViewById(R.id.rvColectas);
        tvSinColectas = view.findViewById(R.id.tvSinColectas);
        Button btnScan = view.findViewById(R.id.btnEscanearQr);

        rvColectas.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (getArguments() != null) {
            grupoId = getArguments().getInt(ARG_GRUPO_ID);
            esAdmin = getArguments().getBoolean(ARG_ES_ADMIN, false);
        }

        btnScan.setOnClickListener(v -> iniciarEscaneo());
        cargarColectas();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarColectas();
    }

    private void iniciarEscaneo() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Apunta al QR de la colecta");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        scanLauncher.launch(options);
    }

    private void cargarColectas() {
        if (!isAdded()) return;
        SessionManager session = new SessionManager(requireContext());

        ApiClient.getService().listarColectas(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            JsonArray arr = response.body().getAsJsonArray("colectas");
                            List<JsonObject> lista = new ArrayList<>();
                            for (int i = 0; i < arr.size(); i++) {
                                lista.add(arr.get(i).getAsJsonObject());
                            }
                            if (lista.isEmpty()) {
                                rvColectas.setVisibility(View.GONE);
                                tvSinColectas.setVisibility(View.VISIBLE);
                            } else {
                                tvSinColectas.setVisibility(View.GONE);
                                rvColectas.setVisibility(View.VISIBLE);
                                rvColectas.setAdapter(new ColectaAdapter(lista, colecta -> {
                                    Intent intent = new Intent(requireContext(),
                                            DetalleColectaActivity.class);
                                    intent.putExtra("colecta_id", colecta.get("id").getAsInt());
                                    startActivity(intent);
                                }));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }
}
