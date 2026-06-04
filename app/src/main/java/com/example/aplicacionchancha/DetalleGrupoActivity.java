package com.example.aplicacionchancha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aplicacionchancha.adapters.GroupPagerAdapter;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.PdfGenerator;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleGrupoActivity extends AppCompatActivity {

    private int     grupoId;
    private String  grupoNombre;
    private String  codigoInvitacion;
    private boolean esAdmin = false;

    private ViewPager2           viewPager;
    private TabLayout            tabLayout;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_grupo);

        grupoId          = getIntent().getIntExtra("grupo_id", 0);
        grupoNombre      = getIntent().getStringExtra("grupo_nombre");
        codigoInvitacion = getIntent().getStringExtra("codigo_invitacion");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(grupoNombre);
        }

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fab       = findViewById(R.id.fabAgregarGasto);

        determinarAdmin();
    }

    private void determinarAdmin() {
        SessionManager session = new SessionManager(this);
        ApiClient.getService().detalleGrupo(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject grupo = response.body().getAsJsonObject("grupo");
                            int creadoPorId  = grupo.get("creado_por_id") != null
                                    ? grupo.get("creado_por_id").getAsInt() : 0;
                            esAdmin = (creadoPorId == session.getUserId());
                        }
                        montarPager();
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        montarPager();
                    }
                });
    }

    private void montarPager() {
        viewPager.setAdapter(new GroupPagerAdapter(this, grupoId, codigoInvitacion, esAdmin));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Gastos");   break;
                case 1: tab.setText("Balances"); break;
                case 2: tab.setText("Miembros"); break;
                case 3: tab.setText("Colectas"); break;
            }
        }).attach();

        actualizarFab(0);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                actualizarFab(position);
            }
        });
    }

    private void actualizarFab(int position) {
        if (position == 0) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(this, AgregarGastoActivity.class);
                intent.putExtra("grupo_id", grupoId);
                startActivity(intent);
            });
        } else if (position == 3 && esAdmin) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(this, CrearColectaActivity.class);
                intent.putExtra("grupo_id", grupoId);
                startActivity(intent);
            });
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_member) {
            Intent intent = new Intent(this, AgregarMiembrosActivity.class);
            intent.putExtra("grupo_id", grupoId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_export_pdf) {
            exportarPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportarPdf() {
        Toast.makeText(this, "Generando PDF…", Toast.LENGTH_SHORT).show();
        SessionManager session = new SessionManager(this);

        ApiClient.getService().listarGastos(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> respGastos) {
                        JsonArray gastos = new JsonArray();
                        if (respGastos.isSuccessful() && respGastos.body() != null) {
                            gastos = respGastos.body().getAsJsonArray("gastos");
                        }
                        final JsonArray gastosF = gastos;

                        ApiClient.getService().balancesGrupo(session.getBearerToken(), grupoId)
                                .enqueue(new Callback<JsonObject>() {
                                    @Override
                                    public void onResponse(Call<JsonObject> call2, Response<JsonObject> respBal) {
                                        JsonArray balances = new JsonArray();
                                        if (respBal.isSuccessful() && respBal.body() != null
                                                && respBal.body().has("balances")) {
                                            balances = respBal.body().getAsJsonArray("balances");
                                        }
                                        generarYCompartirPdf(gastosF, balances);
                                    }
                                    @Override
                                    public void onFailure(Call<JsonObject> call2, Throwable t) {
                                        generarYCompartirPdf(gastosF, new JsonArray());
                                    }
                                });
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(DetalleGrupoActivity.this,
                                "Error al obtener datos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void generarYCompartirPdf(JsonArray gastos, JsonArray balances) {
        try {
            File pdf = PdfGenerator.generarResumenGrupo(this, grupoNombre, gastos, balances);
            Uri uri  = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", pdf);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "Resumen: " + grupoNombre);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Compartir PDF"));

        } catch (Exception e) {
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_LONG).show();
        }
    }
}
