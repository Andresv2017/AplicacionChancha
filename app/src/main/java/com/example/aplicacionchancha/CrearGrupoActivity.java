package com.example.aplicacionchancha;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrearGrupoActivity extends AppCompatActivity {

    private TextInputLayout tilNombre, tilCodigo;
    private TextInputEditText etNombre, etCodigo;
    private ProgressBar progressBar;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_grupo);

        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilNombre   = findViewById(R.id.tilNombreGrupo);
        tilCodigo   = findViewById(R.id.tilCodigo);
        etNombre    = findViewById(R.id.etNombreGrupo);
        etCodigo    = findViewById(R.id.etCodigo);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnCrearGrupo).setOnClickListener(v -> crearGrupo());
        findViewById(R.id.btnUnirse).setOnClickListener(v -> unirseGrupo());
    }

    private void crearGrupo() {
        tilNombre.setError(null);
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) {
            tilNombre.setError("Ingresa un nombre para el grupo");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("nombre", nombre);

        ApiClient.getService().crearGrupo(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            String codigo = response.body().get("codigo_invitacion").getAsString();
                            Toast.makeText(CrearGrupoActivity.this,
                                    "Grupo creado. Código: " + codigo, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(CrearGrupoActivity.this,
                                    "Error al crear el grupo", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CrearGrupoActivity.this,
                                "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unirseGrupo() {
        tilCodigo.setError(null);
        String codigo = etCodigo.getText() != null ? etCodigo.getText().toString().trim() : "";

        if (TextUtils.isEmpty(codigo)) {
            tilCodigo.setError("Ingresa el código de invitación");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("codigo_invitacion", codigo);

        ApiClient.getService().unirseGrupo(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(CrearGrupoActivity.this,
                                    "¡Te uniste al grupo!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String msg = "Código inválido o ya eres miembro";
                            try {
                                JsonObject err = new com.google.gson.JsonParser()
                                        .parse(response.errorBody().string()).getAsJsonObject();
                                if (err.has("mensaje")) msg = err.get("mensaje").getAsString();
                            } catch (Exception ignored) {}
                            tilCodigo.setError(msg);
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CrearGrupoActivity.this,
                                "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
