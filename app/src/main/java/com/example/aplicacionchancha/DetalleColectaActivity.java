package com.example.aplicacionchancha;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.adapters.PagoColectaAdapter;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleColectaActivity extends AppCompatActivity {

    private TextView       tvTitulo, tvMonto, tvFechaLimite, tvEstado;
    private ImageView      ivQrCode;
    private MaterialButton btnPagar;
    private RecyclerView   rvPagos;

    private int     colectaId = 0;
    private String  codigoQr  = null;
    private boolean esAdmin   = false;
    private JsonObject colectaData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_colecta);

        colectaId = getIntent().getIntExtra("colecta_id", 0);
        codigoQr  = getIntent().getStringExtra("codigo_qr");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitulo     = findViewById(R.id.tvTituloColecta);
        tvMonto      = findViewById(R.id.tvMontoDetalle);
        tvFechaLimite = findViewById(R.id.tvFechaLimiteDetalle);
        tvEstado     = findViewById(R.id.tvEstadoDetalle);
        ivQrCode     = findViewById(R.id.ivQrCode);
        btnPagar     = findViewById(R.id.btnPagar);
        rvPagos      = findViewById(R.id.rvPagos);

        rvPagos.setLayoutManager(new LinearLayoutManager(this));

        btnPagar.setOnClickListener(v -> mostrarDialogoPago());

        cargarDetalle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (colectaData != null) {
            cargarDetalle();
        }
    }

    private void cargarDetalle() {
        SessionManager session = new SessionManager(this);
        esAdmin = false; // se determinará al comparar creador_id con userId

        Call<JsonObject> call;
        if (colectaId > 0) {
            call = ApiClient.getService().detalleColecta(session.getBearerToken(), colectaId);
        } else if (codigoQr != null && !codigoQr.isEmpty()) {
            call = ApiClient.getService().detalleColectaPorQr(session.getBearerToken(), codigoQr);
        } else {
            Toast.makeText(this, "Colecta no especificada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    colectaData = response.body().getAsJsonObject("colecta");
                    JsonArray   partesArr = response.body().getAsJsonArray("partes");
                    mostrarColecta(colectaData, partesArr, session.getUserId());
                } else {
                    Toast.makeText(DetalleColectaActivity.this,
                            "No se pudo cargar la colecta", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) {
                Toast.makeText(DetalleColectaActivity.this,
                        "Sin conexión al servidor", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void mostrarColecta(JsonObject colecta, JsonArray partesArr, int userId) {
        // Guardar colecta_id en caso de que vinimos por QR
        if (colectaId == 0) {
            colectaId = colecta.get("id").getAsInt();
        }

        String descripcion     = colecta.get("descripcion").getAsString();
        double montoPorPersona = colecta.get("monto_por_persona").getAsDouble();
        String estado          = colecta.get("estado").getAsString();
        int    creadorId       = colecta.get("creador_id").getAsInt();
        String fechaLimite     = colecta.has("fecha_limite") && !colecta.get("fecha_limite").isJsonNull()
                                  ? colecta.get("fecha_limite").getAsString() : null;

        esAdmin = (creadorId == userId);

        tvTitulo.setText(descripcion);
        tvMonto.setText("S/. " + String.format("%.2f", montoPorPersona) + " por persona");

        if (fechaLimite != null) {
            tvFechaLimite.setVisibility(View.VISIBLE);
            tvFechaLimite.setText("Límite: " + fechaLimite);
        } else {
            tvFechaLimite.setVisibility(View.GONE);
        }

        if ("activa".equals(estado)) {
            tvEstado.setText("● Activa");
            tvEstado.setTextColor(getColor(R.color.success_green));
        } else {
            tvEstado.setText("● Cerrada");
            tvEstado.setTextColor(getColor(R.color.gray_text));
        }

        // Generar QR
        String qrContent = colecta.get("codigo_qr").getAsString();
        generarQr(qrContent);

        // Procesar partes
        List<JsonObject> partes = new ArrayList<>();
        String miEstado = "pendiente";
        for (int i = 0; i < partesArr.size(); i++) {
            JsonObject parte = partesArr.get(i).getAsJsonObject();
            partes.add(parte);
            if (parte.get("usuario_id").getAsInt() == userId) {
                miEstado = parte.get("estado_pago").getAsString();
            }
        }

        // Mostrar botón pagar solo si estado de colecta es activa y mi estado es pendiente
        if ("activa".equals(estado) && "pendiente".equals(miEstado)) {
            btnPagar.setVisibility(View.VISIBLE);
        } else {
            btnPagar.setVisibility(View.GONE);
        }

        // Lista de pagos
        rvPagos.setAdapter(new PagoColectaAdapter(partes, esAdmin, pagadorId ->
                confirmarPago(pagadorId)));
    }

    private void generarQr(String contenido) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(contenido, BarcodeFormat.QR_CODE, 500, 500);
            ivQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            ivQrCode.setVisibility(View.GONE);
        }
    }

    private void mostrarDialogoPago() {
        String[] metodos = {"Yape", "BCP", "Efectivo", "BBVA", "Interbank"};
        new AlertDialog.Builder(this)
                .setTitle("¿Cómo pagaste?")
                .setItems(metodos, (dialog, which) -> registrarPago(metodos[which]))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void registrarPago(String metodoPago) {
        btnPagar.setEnabled(false);
        btnPagar.setText("Registrando...");

        JsonObject body = new JsonObject();
        body.addProperty("colecta_id", colectaId);
        body.addProperty("metodo_pago", metodoPago);

        SessionManager session = new SessionManager(this);
        ApiClient.getService().pagarColecta(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().has("mensaje")) {
                            Toast.makeText(DetalleColectaActivity.this,
                                    "Pago registrado", Toast.LENGTH_SHORT).show();
                            btnPagar.setVisibility(View.GONE);
                            cargarDetalle();
                        } else {
                            btnPagar.setEnabled(true);
                            btnPagar.setText("Registrar mi pago");
                            String err = "Error al registrar pago";
                            if (response.body() != null && response.body().has("error")) {
                                err = response.body().get("error").getAsString();
                            }
                            Toast.makeText(DetalleColectaActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        btnPagar.setEnabled(true);
                        btnPagar.setText("Registrar mi pago");
                        Toast.makeText(DetalleColectaActivity.this,
                                "Sin conexión al servidor", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void confirmarPago(int pagadorId) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar pago")
                .setMessage("¿Confirmas que recibiste este pago?")
                .setPositiveButton("Sí, confirmar", (dialog, which) -> {
                    JsonObject body = new JsonObject();
                    body.addProperty("colecta_id", colectaId);
                    body.addProperty("usuario_id", pagadorId);

                    SessionManager session = new SessionManager(this);
                    ApiClient.getService().confirmarPago(session.getBearerToken(), body)
                            .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                    if (response.isSuccessful() && response.body() != null
                                            && response.body().has("mensaje")) {
                                        Toast.makeText(DetalleColectaActivity.this,
                                                "Pago confirmado", Toast.LENGTH_SHORT).show();
                                        cargarDetalle();
                                    } else {
                                        String err = "Error al confirmar";
                                        if (response.body() != null && response.body().has("error")) {
                                            err = response.body().get("error").getAsString();
                                        }
                                        Toast.makeText(DetalleColectaActivity.this, err, Toast.LENGTH_LONG).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<JsonObject> call, Throwable t) {
                                    Toast.makeText(DetalleColectaActivity.this,
                                            "Sin conexión al servidor", Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
