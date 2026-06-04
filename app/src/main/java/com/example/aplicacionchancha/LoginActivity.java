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
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etCorreo, etContrasena;
    private Button btnIngresar;
    private ProgressBar progressBar;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session     = new SessionManager(this);
        etCorreo    = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnIngresar  = findViewById(R.id.btnIngresar);
        progressBar  = findViewById(R.id.progressBar);

        btnIngresar.setOnClickListener(v -> intentarLogin());

        TextView tvRegistro = findViewById(R.id.tvRegistro);
        tvRegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));

        TextView tvOlvide = findViewById(R.id.tvOlvide);
        tvOlvide.setOnClickListener(v ->
                startActivity(new Intent(this, RecuperarPasswordActivity.class)));
    }

    private void intentarLogin() {
        String correo = etCorreo.getText().toString().trim();
        String pass   = etContrasena.getText().toString().trim();

        if (correo.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_campos_vacios), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        JsonObject body = new JsonObject();
        body.addProperty("correo",     correo);
        body.addProperty("contrasena", pass);

        ApiClient.getService().login(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    String token   = data.get("token").getAsString();
                    JsonObject user = data.getAsJsonObject("usuario");
                    int    userId  = user.get("id").getAsInt();
                    String nombre  = user.get("nombre").getAsString();
                    String correoU = user.get("correo").getAsString();

                    session.guardarSesion(token, userId, nombre, correoU);

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Error de conexión. Verifica que el servidor esté activo.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnIngresar.setEnabled(!loading);
    }
}
