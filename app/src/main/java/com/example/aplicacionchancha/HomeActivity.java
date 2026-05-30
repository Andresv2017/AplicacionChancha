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
    private TextView tvSinGrupos, tvTotalDebo, tvTotalMeDeben;
    private SessionManager session;

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
                startActivity(new Intent(this, CreateGroupActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarGrupos();
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
                                Intent intent = new Intent(HomeActivity.this, GroupDetailActivity.class);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            cerrarSesion();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
