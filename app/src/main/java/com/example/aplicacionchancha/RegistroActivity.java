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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etCorreo, etContrasena, etConfirmar;
    private Button            btnRegistrarse;
    private ProgressBar       progressBar;
    private FirebaseAuth      mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        mAuth          = FirebaseAuth.getInstance();
        etNombre       = findViewById(R.id.etNombre);
        etCorreo       = findViewById(R.id.etCorreo);
        etContrasena   = findViewById(R.id.etContrasena);
        etConfirmar    = findViewById(R.id.etConfirmar);
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

        if (!pass.equals(confirmar)) {
            Toast.makeText(this, getString(R.string.error_contrasenas), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // 1. Crear cuenta en Firebase Auth
        mAuth.createUserWithEmailAndPassword(correo, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        // 2. Obtener token y registrar en MySQL
                        task.getResult().getUser().getIdToken(false)
                                .addOnSuccessListener(r -> registrarEnBackend(r.getToken(), nombre));
                    } else {
                        setLoading(false);
                        String msg = "Error al registrar";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            msg = "El correo ya está registrado";
                        } else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                            msg = "La contraseña debe tener al menos 6 caracteres";
                        } else if (task.getException() != null) {
                            msg = task.getException().getMessage();
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registrarEnBackend(String firebaseToken, String nombre) {
        JsonObject body = new JsonObject();
        body.addProperty("firebase_token", firebaseToken);
        body.addProperty("nombre", nombre);

        ApiClient.getService().registroFirebase(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(RegistroActivity.this,
                            "Cuenta creada. Inicia sesión.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
                    finish();
                } else {
                    // Si falla MySQL, eliminar cuenta de Firebase para no dejar huérfanos
                    mAuth.getCurrentUser().delete();
                    String msg = "Error al registrar";
                    try {
                        JsonObject err = new com.google.gson.JsonParser()
                                .parse(response.errorBody().string()).getAsJsonObject();
                        msg = err.get("error").getAsString();
                    } catch (Exception ignored) {}
                    Toast.makeText(RegistroActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                mAuth.getCurrentUser().delete();
                Toast.makeText(RegistroActivity.this,
                        "Error de conexión. Verifica que el servidor esté activo.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegistrarse.setEnabled(!loading);
    }
}
