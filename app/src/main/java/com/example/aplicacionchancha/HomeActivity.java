package com.example.aplicacionchancha;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.adapters.GroupAdapter;
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

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvGrupos;
    private TextView     tvSinGrupos, tvTotalDebo, tvTotalMeDeben;
    private SessionManager session;

    // Badge de notificaciones
    private TextView tvBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Hola, " + session.getNombre().split(" ")[0]);
        }

        rvGrupos       = findViewById(R.id.rvGrupos);
        tvSinGrupos    = findViewById(R.id.tvSinGrupos);
        tvTotalDebo    = findViewById(R.id.tvTotalDebo);
        tvTotalMeDeben = findViewById(R.id.tvTotalMeDeben);

        rvGrupos.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.fabNuevoGrupo).setOnClickListener(v ->
                startActivity(new Intent(this, CrearGrupoActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar saludo por si el nombre cambió
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Hola, " + session.getNombre().split(" ")[0]);
        }
        cargarGrupos();
        verificarNotificaciones();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);

        // Obtener la action view de la campana para el badge
        MenuItem bellItem = menu.findItem(R.id.action_notifications);
        if (bellItem != null && bellItem.getActionView() != null) {
            tvBadge = bellItem.getActionView().findViewById(R.id.tvBadge);
            bellItem.getActionView().setOnClickListener(v -> {
                startActivity(new Intent(this, NotificacionesActivity.class));
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, PerfilActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            cerrarSesion();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void verificarNotificaciones() {
        ApiClient.getService().listarNotificaciones(session.getBearerToken())
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null && tvBadge != null) {
                            int noLeidas = response.body().get("no_leidas").getAsInt();
                            if (noLeidas > 0) {
                                tvBadge.setText(noLeidas > 9 ? "9+" : String.valueOf(noLeidas));
                                tvBadge.setVisibility(View.VISIBLE);
                            } else {
                                tvBadge.setVisibility(View.GONE);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void cargarGrupos() {
        ApiClient.getService().listarGrupos(session.getBearerToken())
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) return;

                        JsonArray arr = response.body().getAsJsonArray("grupos");
                        List<JsonObject> lista = new ArrayList<>();
                        double totalDebo = 0, totalMeDeben = 0;

                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject g = arr.get(i).getAsJsonObject();
                            lista.add(g);
                            totalDebo    += g.has("total_debo")     ? g.get("total_debo").getAsDouble()     : 0;
                            totalMeDeben += g.has("total_me_deben") ? g.get("total_me_deben").getAsDouble() : 0;
                        }

                        tvTotalDebo.setText("S/. " + String.format("%.2f", totalDebo));
                        tvTotalMeDeben.setText("S/. " + String.format("%.2f", totalMeDeben));

                        if (lista.isEmpty()) {
                            rvGrupos.setVisibility(View.GONE);
                            tvSinGrupos.setVisibility(View.VISIBLE);
                        } else {
                            tvSinGrupos.setVisibility(View.GONE);
                            rvGrupos.setVisibility(View.VISIBLE);
                            rvGrupos.setAdapter(new GroupAdapter(lista, grupo -> {
                                Intent intent = new Intent(HomeActivity.this, DetalleGrupoActivity.class);
                                intent.putExtra("grupo_id", grupo.get("id").getAsInt());
                                intent.putExtra("grupo_nombre", grupo.get("nombre").getAsString());
                                intent.putExtra("codigo_invitacion", grupo.get("codigo_invitacion").getAsString());
                                startActivity(intent);
                            }));
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void cerrarSesion() {
        ApiClient.getService().logout(session.getBearerToken())
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
        session.cerrarSesion();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
