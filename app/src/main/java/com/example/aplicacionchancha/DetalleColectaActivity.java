package com.example.aplicacionchancha;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleColectaActivity extends AppCompatActivity {

    private TextView       tvTitulo, tvMonto, tvFechaLimite, tvEstado, tvMiEstado;
    private ImageView      ivQrCode;
    private MaterialButton btnPagar;
    private RecyclerView   rvPagos;

    private int        colectaId  = 0;
    private String     codigoQr   = null;
    private boolean    esAdmin    = false;
    private String     miEstado   = "pendiente";
    private JsonObject colectaData = null;

    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_colecta);

        colectaId = getIntent().getIntExtra("colecta_id", 0);
        codigoQr  = getIntent().getStringExtra("codigo_qr");

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) subirComprobante(uri); }
        );

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitulo      = findViewById(R.id.tvTituloColecta);
        tvMonto       = findViewById(R.id.tvMontoDetalle);
        tvFechaLimite = findViewById(R.id.tvFechaLimiteDetalle);
        tvEstado      = findViewById(R.id.tvEstadoDetalle);
        tvMiEstado    = findViewById(R.id.tvMiEstado);
        ivQrCode      = findViewById(R.id.ivQrCode);
        btnPagar      = findViewById(R.id.btnPagar);
        rvPagos       = findViewById(R.id.rvPagos);

        rvPagos.setLayoutManager(new LinearLayoutManager(this));

        btnPagar.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        cargarDetalle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (colectaData != null) cargarDetalle();
    }

    // -----------------------------------------------------------------------
    // Carga de datos
    // -----------------------------------------------------------------------

    private void cargarDetalle() {
        SessionManager session = new SessionManager(this);

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
                    JsonArray partesArr = response.body().getAsJsonArray("partes");
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

    // -----------------------------------------------------------------------
    // Mostrar datos
    // -----------------------------------------------------------------------

    private void mostrarColecta(JsonObject colecta, JsonArray partesArr, int userId) {
        if (colectaId == 0) colectaId = colecta.get("id").getAsInt();

        String descripcion     = colecta.get("descripcion").getAsString();
        double montoPorPersona = colecta.get("monto_por_persona").getAsDouble();
        String estadoColecta   = colecta.get("estado").getAsString();
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

        tvEstado.setText("activa".equals(estadoColecta) ? "● Activa" : "● Cerrada");
        tvEstado.setTextColor(getColor("activa".equals(estadoColecta)
                ? R.color.success_green : R.color.gray_text));

        generarQr(colecta.get("codigo_qr").getAsString());

        // Buscar mi estado
        miEstado = "pendiente";
        List<JsonObject> partes = new ArrayList<>();
        for (int i = 0; i < partesArr.size(); i++) {
            JsonObject parte = partesArr.get(i).getAsJsonObject();
            partes.add(parte);
            if (parte.get("usuario_id").getAsInt() == userId) {
                miEstado = parte.get("estado_pago").getAsString();
            }
        }

        // Botón y mensaje según mi estado
        boolean colectaActiva = "activa".equals(estadoColecta);
        switch (miEstado) {
            case "pendiente":
                if (colectaActiva) {
                    btnPagar.setVisibility(View.VISIBLE);
                    btnPagar.setText("Subir comprobante de pago");
                    btnPagar.setEnabled(true);
                } else {
                    btnPagar.setVisibility(View.GONE);
                }
                tvMiEstado.setVisibility(View.GONE);
                break;

            case "en_revision":
                btnPagar.setVisibility(View.GONE);
                tvMiEstado.setVisibility(View.VISIBLE);
                tvMiEstado.setText("Comprobante enviado · Esperando confirmación del admin");
                tvMiEstado.setTextColor(0xFFE65100);
                break;

            case "confirmado":
                btnPagar.setVisibility(View.GONE);
                tvMiEstado.setVisibility(View.VISIBLE);
                tvMiEstado.setText("Tu pago fue confirmado ✓");
                tvMiEstado.setTextColor(getColor(R.color.success_green));
                break;

            default:
                btnPagar.setVisibility(View.GONE);
                tvMiEstado.setVisibility(View.GONE);
                break;
        }

        // Lista de miembros con su estado
        rvPagos.setAdapter(new PagoColectaAdapter(partes, esAdmin,
                (uid, comprobante) -> mostrarDialogRevisar(uid, comprobante)));
    }

    private void generarQr(String contenido) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            android.graphics.Bitmap bitmap =
                    encoder.encodeBitmap(contenido, BarcodeFormat.QR_CODE, 500, 500);
            ivQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            ivQrCode.setVisibility(View.GONE);
        }
    }

    // -----------------------------------------------------------------------
    // Subir comprobante (miembro)
    // -----------------------------------------------------------------------

    private void subirComprobante(Uri uri) {
        btnPagar.setEnabled(false);
        btnPagar.setText("Subiendo...");

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                is.close();
                byte[] bytes = baos.toByteArray();

                String mime = getContentResolver().getType(uri);
                if (mime == null) mime = "image/jpeg";
                String ext = mime.contains("png") ? "png" : mime.contains("webp") ? "webp" : "jpg";

                RequestBody reqFile = RequestBody.create(MediaType.parse(mime), bytes);
                MultipartBody.Part part = MultipartBody.Part.createFormData(
                        "comprobante", "comprobante." + ext, reqFile);
                RequestBody idBody = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(colectaId));

                SessionManager session = new SessionManager(this);

                runOnUiThread(() ->
                        ApiClient.getService()
                                .pagarColecta(session.getBearerToken(), idBody, part)
                                .enqueue(new Callback<JsonObject>() {
                                    @Override
                                    public void onResponse(Call<JsonObject> call, Response<JsonObject> r) {
                                        if (r.isSuccessful()) {
                                            Toast.makeText(DetalleColectaActivity.this,
                                                    "Comprobante enviado. El admin lo revisará.",
                                                    Toast.LENGTH_LONG).show();
                                            cargarDetalle();
                                        } else {
                                            btnPagar.setEnabled(true);
                                            btnPagar.setText("Subir comprobante de pago");
                                            String err = "Error al subir";
                                            try {
                                                JsonObject e = new com.google.gson.JsonParser()
                                                        .parse(r.errorBody().string()).getAsJsonObject();
                                                if (e.has("error")) err = e.get("error").getAsString();
                                            } catch (Exception ignored) {}
                                            Toast.makeText(DetalleColectaActivity.this,
                                                    err, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                    @Override
                                    public void onFailure(Call<JsonObject> call, Throwable t) {
                                        btnPagar.setEnabled(true);
                                        btnPagar.setText("Subir comprobante de pago");
                                        Toast.makeText(DetalleColectaActivity.this,
                                                "Sin conexión", Toast.LENGTH_SHORT).show();
                                    }
                                })
                );
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnPagar.setEnabled(true);
                    btnPagar.setText("Subir comprobante de pago");
                    Toast.makeText(this, "Error al leer la imagen", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // -----------------------------------------------------------------------
    // Revisar comprobante (admin)
    // -----------------------------------------------------------------------

    private void mostrarDialogRevisar(int pagadorId, String comprobante) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        TextView tvCargando = new TextView(this);
        tvCargando.setText("Cargando imagen...");
        tvCargando.setTextSize(13f);
        tvCargando.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvCargando);

        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(320 * getResources().getDisplayMetrics().density));
        imgLp.topMargin = (int)(8 * getResources().getDisplayMetrics().density);
        imageView.setLayoutParams(imgLp);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setVisibility(View.GONE);
        layout.addView(imageView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Comprobante de pago")
                .setView(layout)
                .setPositiveButton("Confirmar pago", (d, w) -> procesarRevision(pagadorId, "confirmar"))
                .setNegativeButton("Rechazar", (d, w) -> procesarRevision(pagadorId, "rechazar"))
                .setNeutralButton("Cerrar", null)
                .create();
        dialog.show();

        String imageUrl = ApiClient.BASE_URL + comprobante;
        new Thread(() -> {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(new URL(imageUrl).openStream());
                runOnUiThread(() -> {
                    if (dialog.isShowing()) {
                        tvCargando.setVisibility(View.GONE);
                        imageView.setImageBitmap(bmp);
                        imageView.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> tvCargando.setText("No se pudo cargar la imagen"));
            }
        }).start();
    }

    private void procesarRevision(int pagadorId, String accion) {
        SessionManager session = new SessionManager(this);
        JsonObject body = new JsonObject();
        body.addProperty("colecta_id", colectaId);
        body.addProperty("usuario_id", pagadorId);
        body.addProperty("accion",     accion);

        ApiClient.getService().confirmarPago(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> r) {
                        String msg = "confirmar".equals(accion)
                                ? "Pago confirmado" : "Comprobante rechazado";
                        Toast.makeText(DetalleColectaActivity.this, msg, Toast.LENGTH_SHORT).show();
                        cargarDetalle();
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(DetalleColectaActivity.this,
                                "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
