package com.example.aplicacionchancha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicacionchancha.network.ApiClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etCorreo, etContrasena, etConfirmar;
    private Button btnRegistrarse;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre     = findViewById(R.id.etNombre);
        etCorreo     = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        etConfirmar  = findViewById(R.id.etConfirmar);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);
        progressBar    = findViewById(R.id.progressBar);

        btnRegistrarse.setOnClickListener(v -> intentarRegistro());

        TextView tvYaTengo = findViewById(R.id.tvYaTengo);
        tvYaTengo.setOnClickListener(v -> finish());
    }

    private void intentarRegistro() {
        String nombre    = etNombre.getText().toString().trim();
        String correo    = etCorreo.getText().toString().trim();
        String pass      = etContrasena.getText().toString().trim();
        String confirmar = etConfirmar.getText().toString().trim();

        if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_campos_vacios), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!correo.toLowerCase().endsWith(".edu.pe")) {
            Toast.makeText(this, getString(R.string.error_correo_invalido), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmar)) {
            Toast.makeText(this, getString(R.string.error_contrasenas), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        JsonObject body = new JsonObject();
        body.addProperty("nombre",      nombre);
        body.addProperty("correo",      correo);
        body.addProperty("contrasena",  pass);
        body.addProperty("contrasena2", confirmar);

        ApiClient.getService().register(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this,
                            "Cuenta creada. Inicia sesión.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    String msg = "Error al registrar";
                    try {
                        JsonObject err = new com.google.gson.JsonParser()
                                .parse(response.errorBody().string()).getAsJsonObject();
                        msg = err.get("error").getAsString();
                    } catch (Exception ignored) {}
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this,
                        "Error de conexión. Verifica que el servidor esté activo.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegistrarse.setEnabled(!loading);
    }
}
