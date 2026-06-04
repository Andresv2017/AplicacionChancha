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

public class CambiarPasswordActivity extends AppCompatActivity {

    private TextInputEditText etActual, etNueva, etConfirmar;
    private MaterialButton    btnCambiar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_password);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etActual    = findViewById(R.id.etPasswordActual);
        etNueva     = findViewById(R.id.etPasswordNueva);
        etConfirmar = findViewById(R.id.etPasswordConfirmar);
        btnCambiar  = findViewById(R.id.btnCambiar);

        btnCambiar.setOnClickListener(v -> cambiarPassword());
    }

    private void cambiarPassword() {
        String actual    = etActual.getText()    != null ? etActual.getText().toString()    : "";
        String nueva     = etNueva.getText()     != null ? etNueva.getText().toString()     : "";
        String confirmar = etConfirmar.getText() != null ? etConfirmar.getText().toString() : "";

        if (TextUtils.isEmpty(actual)) {
            etActual.setError("Ingresa tu contraseña actual");
            return;
        }
        if (nueva.length() < 6) {
            etNueva.setError("Mínimo 6 caracteres");
            return;
        }
        if (!nueva.equals(confirmar)) {
            etConfirmar.setError("Las contraseñas no coinciden");
            return;
        }

        btnCambiar.setEnabled(false);
        btnCambiar.setText("Cambiando...");

        SessionManager session = new SessionManager(this);
        JsonObject body = new JsonObject();
        body.addProperty("password_actual", actual);
        body.addProperty("password_nueva",  nueva);

        ApiClient.getService().cambiarPassword(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        btnCambiar.setEnabled(true);
                        btnCambiar.setText("Cambiar contraseña");
                        if (response.isSuccessful() && response.body() != null
                                && response.body().has("mensaje")) {
                            Toast.makeText(CambiarPasswordActivity.this,
                                    "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String err = "Error al cambiar contraseña";
                            if (response.body() != null && response.body().has("error")) {
                                err = response.body().get("error").getAsString();
                            }
                            Toast.makeText(CambiarPasswordActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        btnCambiar.setEnabled(true);
                        btnCambiar.setText("Cambiar contraseña");
                        Toast.makeText(CambiarPasswordActivity.this,
                                "Sin conexión al servidor", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
