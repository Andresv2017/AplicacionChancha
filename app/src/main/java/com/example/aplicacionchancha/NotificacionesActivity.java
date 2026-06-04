package com.example.aplicacionchancha;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.adapters.NotificacionAdapter;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView rvNotificaciones;
    private TextView     tvSinNotificaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvNotificaciones    = findViewById(R.id.rvNotificaciones);
        tvSinNotificaciones = findViewById(R.id.tvSinNotificaciones);
        rvNotificaciones.setLayoutManager(new LinearLayoutManager(this));

        cargarNotificaciones();
    }

    private void cargarNotificaciones() {
        SessionManager session = new SessionManager(this);

        ApiClient.getService().listarNotificaciones(session.getBearerToken())
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonArray arr = response.body().getAsJsonArray("notificaciones");
                            List<JsonObject> lista = new ArrayList<>();
                            for (int i = 0; i < arr.size(); i++) {
                                lista.add(arr.get(i).getAsJsonObject());
                            }
                            if (lista.isEmpty()) {
                                rvNotificaciones.setVisibility(View.GONE);
                                tvSinNotificaciones.setVisibility(View.VISIBLE);
                            } else {
                                tvSinNotificaciones.setVisibility(View.GONE);
                                rvNotificaciones.setVisibility(View.VISIBLE);
                                rvNotificaciones.setAdapter(new NotificacionAdapter(lista));
                            }
                            // Marcar todas como leídas
                            marcarLeidas(session);
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void marcarLeidas(SessionManager session) {
        ApiClient.getService()
                .marcarNotificacionesLeidas(session.getBearerToken(), new JsonObject())
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }
}
