package com.example.aplicacionchancha;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aplicacionchancha.adapters.GroupPagerAdapter;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupDetailActivity extends AppCompatActivity {

    private int     grupoId;
    private String  grupoNombre;
    private String  codigoInvitacion;
    private boolean esAdmin = false;

    private ViewPager2 viewPager;
    private TabLayout  tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

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

        findViewById(R.id.fabAgregarGasto).setOnClickListener(v -> {
            Intent intent = new Intent(this, AgregarGastoActivity.class);
            intent.putExtra("grupo_id", grupoId);
            startActivity(intent);
        });

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
                                    ? grupo.get("creado_por_id").getAsInt()
                                    : 0;
                            esAdmin = (creadoPorId == session.getUserId());
                        }
                        montarPager();
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        montarPager(); // montar igual sin privilegios admin
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
            }
        }).attach();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_member) {
            Intent intent = new Intent(this, AddMembersActivity.class);
            intent.putExtra("grupo_id", grupoId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
