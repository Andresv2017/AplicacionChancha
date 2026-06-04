package com.example.aplicacionchancha;

import android.app.DatePickerDialog;
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

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrearColectaActivity extends AppCompatActivity {

    private TextInputEditText etDescripcion, etMonto, etFechaLimite;
    private MaterialButton    btnCrear;
    private int               grupoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_colecta);

        grupoId = getIntent().getIntExtra("grupo_id", 0);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etDescripcion  = findViewById(R.id.etDescripcion);
        etMonto        = findViewById(R.id.etMonto);
        etFechaLimite  = findViewById(R.id.etFechaLimite);
        btnCrear       = findViewById(R.id.btnCrearColecta);

        // Selector de fecha
        etFechaLimite.setOnClickListener(v -> mostrarDatePicker());

        btnCrear.setOnClickListener(v -> crearColecta());
    }

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String fecha = String.format("%04d-%02d-%02d", year, month + 1, day);
            etFechaLimite.setText(fecha);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void crearColecta() {
        String descripcion = etDescripcion.getText() != null
                ? etDescripcion.getText().toString().trim() : "";
        String montoStr = etMonto.getText() != null
                ? etMonto.getText().toString().replace(",", ".").trim() : "";
        String fechaLimite = etFechaLimite.getText() != null
                ? etFechaLimite.getText().toString().trim() : "";

        if (TextUtils.isEmpty(descripcion)) {
            etDescripcion.setError("Ingresa una descripción");
            return;
        }
        if (TextUtils.isEmpty(montoStr)) {
            etMonto.setError("Ingresa el monto por persona");
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoStr);
        } catch (NumberFormatException e) {
            etMonto.setError("Monto inválido");
            return;
        }

        if (monto <= 0) {
            etMonto.setError("El monto debe ser mayor a 0");
            return;
        }

        btnCrear.setEnabled(false);
        btnCrear.setText("Creando...");

        JsonObject body = new JsonObject();
        body.addProperty("grupo_id", grupoId);
        body.addProperty("descripcion", descripcion);
        body.addProperty("monto_por_persona", monto);
        if (!TextUtils.isEmpty(fechaLimite)) {
            body.addProperty("fecha_limite", fechaLimite);
        }

        SessionManager session = new SessionManager(this);
        ApiClient.getService().crearColecta(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        btnCrear.setEnabled(true);
                        btnCrear.setText("Crear colecta");
                        if (response.isSuccessful() && response.body() != null
                                && response.body().has("colecta_id")) {
                            Toast.makeText(CrearColectaActivity.this,
                                    "Colecta creada", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String err = "Error al crear colecta";
                            if (response.body() != null && response.body().has("error")) {
                                err = response.body().get("error").getAsString();
                            }
                            Toast.makeText(CrearColectaActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        btnCrear.setEnabled(true);
                        btnCrear.setText("Crear colecta");
                        Toast.makeText(CrearColectaActivity.this,
                                "Sin conexión al servidor", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
