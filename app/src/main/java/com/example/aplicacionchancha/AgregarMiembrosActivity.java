package com.example.aplicacionchancha;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgregarMiembrosActivity extends AppCompatActivity {

    private TextInputLayout tilCorreo;
    private TextInputEditText etCorreo;
    private ProgressBar progressBar;
    private MaterialCardView cardUsuario;
    private TextView tvAvatar, tvNombre, tvCorreo, tvMensaje;
    private SessionManager session;
    private int grupoId;
    private int usuarioEncontradoId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_miembros);

        session = new SessionManager(this);
        grupoId = getIntent().getIntExtra("grupo_id", 0);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilCorreo   = findViewById(R.id.tilCorreoBuscar);
        etCorreo    = findViewById(R.id.etCorreoBuscar);
        progressBar = findViewById(R.id.progressBar);
        cardUsuario = findViewById(R.id.cardUsuarioEncontrado);
        tvAvatar    = findViewById(R.id.tvAvatarEncontrado);
        tvNombre    = findViewById(R.id.tvNombreEncontrado);
        tvCorreo    = findViewById(R.id.tvCorreoEncontrado);
        tvMensaje   = findViewById(R.id.tvMensaje);

        findViewById(R.id.btnBuscar).setOnClickListener(v -> buscarUsuario());
        findViewById(R.id.btnAgregar).setOnClickListener(v -> agregarMiembro());
    }

    private void buscarUsuario() {
        tilCorreo.setError(null);
        cardUsuario.setVisibility(View.GONE);
        tvMensaje.setVisibility(View.GONE);
        usuarioEncontradoId = -1;

        String correo = etCorreo.getText() != null ? etCorreo.getText().toString().trim() : "";
        if (TextUtils.isEmpty(correo)) {
            tilCorreo.setError("Ingresa un correo");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getService().buscarUsuario(session.getBearerToken(), correo, grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject u = response.body().getAsJsonObject("usuario");
                            usuarioEncontradoId = u.get("id").getAsInt();
                            String nombre = u.get("nombre").getAsString();
                            tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
                            tvNombre.setText(nombre);
                            tvCorreo.setText(u.get("correo").getAsString());
                            cardUsuario.setVisibility(View.VISIBLE);
                        } else {
                            String msg = "Usuario no encontrado";
                            try {
                                JsonObject err = new com.google.gson.JsonParser()
                                        .parse(response.errorBody().string()).getAsJsonObject();
                                if (err.has("error")) msg = err.get("error").getAsString();
                            } catch (Exception ignored) {}
                            tvMensaje.setText(msg);
                            tvMensaje.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        tvMensaje.setText("Sin conexión");
                        tvMensaje.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void agregarMiembro() {
        if (usuarioEncontradoId == -1) return;

        progressBar.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("grupo_id", grupoId);
        body.addProperty("nuevo_usuario_id", usuarioEncontradoId);

        ApiClient.getService().agregarMiembro(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(AgregarMiembrosActivity.this,
                                    "Miembro agregado correctamente", Toast.LENGTH_SHORT).show();
                            cardUsuario.setVisibility(View.GONE);
                            etCorreo.setText("");
                            usuarioEncontradoId = -1;
                        } else {
                            Toast.makeText(AgregarMiembrosActivity.this,
                                    "No se pudo agregar al miembro", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AgregarMiembrosActivity.this,
                                "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
