package com.example.aplicacionchancha;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditarPerfilActivity extends AppCompatActivity {

    private TextInputEditText etNombre;
    private MaterialButton    btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etNombre   = findViewById(R.id.etNombre);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Pre-cargar nombre actual
        SessionManager session = new SessionManager(this);
        etNombre.setText(session.getNombre());
        etNombre.setSelection(etNombre.getText() != null ? etNombre.getText().length() : 0);

        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void guardarCambios() {
        String nombre = etNombre.getText() != null
                ? etNombre.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Ingresa tu nombre");
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        SessionManager session = new SessionManager(this);
        JsonObject body = new JsonObject();
        body.addProperty("nombre", nombre);

        ApiClient.getService().editarPerfil(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("Guardar cambios");
                        if (response.isSuccessful() && response.body() != null
                                && response.body().has("mensaje")) {
                            // Actualizar nombre en sesión local
                            session.guardarNombre(nombre);
                            Toast.makeText(EditarPerfilActivity.this,
                                    "Nombre actualizado", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String err = "Error al guardar";
                            if (response.body() != null && response.body().has("error")) {
                                err = response.body().get("error").getAsString();
                            }
                            Toast.makeText(EditarPerfilActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("Guardar cambios");
                        Toast.makeText(EditarPerfilActivity.this,
                                "Sin conexión al servidor", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
