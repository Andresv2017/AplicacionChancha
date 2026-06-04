package com.example.aplicacionchancha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilActivity extends AppCompatActivity {

    private TextView tvAvatar, tvNombre, tvCorreo, tvFechaRegistro;
    private TextView tvGruposActivos, tvDeudasPendientes, tvMontoDebo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvAvatar           = findViewById(R.id.tvAvatar);
        tvNombre           = findViewById(R.id.tvNombre);
        tvCorreo           = findViewById(R.id.tvCorreo);
        tvFechaRegistro    = findViewById(R.id.tvFechaRegistro);
        tvGruposActivos    = findViewById(R.id.tvGruposActivos);
        tvDeudasPendientes = findViewById(R.id.tvDeudasPendientes);
        tvMontoDebo        = findViewById(R.id.tvMontoDebo);

        MaterialButton btnEditar   = findViewById(R.id.btnEditarPerfil);
        MaterialButton btnPassword = findViewById(R.id.btnCambiarPassword);

        btnEditar.setOnClickListener(v ->
                startActivity(new Intent(this, EditarPerfilActivity.class)));
        btnPassword.setOnClickListener(v ->
                startActivity(new Intent(this, CambiarPasswordActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPerfil();
    }

    private void cargarPerfil() {
        SessionManager session = new SessionManager(this);

        ApiClient.getService().obtenerPerfil(session.getBearerToken())
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject perfil = response.body().getAsJsonObject("perfil");
                            mostrarPerfil(perfil);
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void mostrarPerfil(JsonObject p) {
        String nombre  = p.get("nombre").getAsString();
        String correo  = p.get("correo").getAsString();
        String fecha   = p.has("fecha_registro") && !p.get("fecha_registro").isJsonNull()
                          ? p.get("fecha_registro").getAsString().substring(0, 10) : "";
        int    grupos  = p.has("grupos_activos") ? p.get("grupos_activos").getAsInt() : 0;
        int    deudas  = p.has("deudas_pendientes") ? p.get("deudas_pendientes").getAsInt() : 0;
        double monto   = p.has("monto_total_debo") ? p.get("monto_total_debo").getAsDouble() : 0;

        tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        tvNombre.setText(nombre);
        tvCorreo.setText(correo);
        tvFechaRegistro.setText("Miembro desde " + fecha);
        tvGruposActivos.setText(String.valueOf(grupos));
        tvDeudasPendientes.setText(String.valueOf(deudas));
        tvMontoDebo.setText("S/." + String.format("%.0f", monto));
    }
}
