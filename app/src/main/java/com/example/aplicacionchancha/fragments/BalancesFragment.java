package com.example.aplicacionchancha.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.aplicacionchancha.R;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BalancesFragment extends Fragment {

    private static final String ARG_GRUPO_ID = "grupo_id";

    private LinearLayout             contenedorBalances;
    private TextView                 tvSinDeudas;
    private PieChart                 pieChart;
    private int                      grupoId;
    private int                      pendingGastoId;
    private ActivityResultLauncher<String> galleryLauncher;

    public static BalancesFragment newInstance(int grupoId) {
        BalancesFragment f = new BalancesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_GRUPO_ID, grupoId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) subirComprobante(pendingGastoId, uri); }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_balances, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contenedorBalances = view.findViewById(R.id.contenedorBalances);
        tvSinDeudas        = view.findViewById(R.id.tvSinDeudas);
        pieChart           = view.findViewById(R.id.pieChart);
        grupoId = getArguments() != null ? getArguments().getInt(ARG_GRUPO_ID) : 0;
        configurarPieChart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (grupoId != 0) cargarDatos(grupoId);
    }

    // -----------------------------------------------------------------------
    // Pie chart
    // -----------------------------------------------------------------------

    private void configurarPieChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(42f);
        pieChart.setTransparentCircleRadius(47f);
        pieChart.setCenterText("Gastos\npor miembro");
        pieChart.setCenterTextSize(12f);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextSize(11f);
        pieChart.setEntryLabelTextSize(10f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setNoDataText("Sin gastos aún");
        pieChart.setRotationEnabled(true);
    }

    // -----------------------------------------------------------------------
    // Carga de datos
    // -----------------------------------------------------------------------

    private void cargarDatos(int grupoId) {
        SessionManager session = new SessionManager(requireContext());

        ApiClient.getService().balancesGrupo(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null)
                            mostrarBalances(response.body().getAsJsonArray("balances"));
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });

        ApiClient.getService().listarGastos(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null)
                            mostrarGrafico(response.body().getAsJsonArray("gastos"));
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    // -----------------------------------------------------------------------
    // Gráfico
    // -----------------------------------------------------------------------

    private void mostrarGrafico(JsonArray gastos) {
        if (gastos == null || gastos.size() == 0) {
            pieChart.setVisibility(View.GONE);
            return;
        }

        Map<String, Double> totales = new LinkedHashMap<>();
        for (int i = 0; i < gastos.size(); i++) {
            JsonObject g     = gastos.get(i).getAsJsonObject();
            String pagadoPor = g.has("pagado_por_nombre")
                    ? g.get("pagado_por_nombre").getAsString() : "?";
            double monto     = g.has("monto_total") ? g.get("monto_total").getAsDouble() : 0;
            totales.merge(pagadoPor, monto, Double::sum);
        }

        List<PieEntry> entradas = new ArrayList<>();
        for (Map.Entry<String, Double> e : totales.entrySet()) {
            entradas.add(new PieEntry(e.getValue().floatValue(), e.getKey().split(" ")[0]));
        }

        PieDataSet dataSet = new PieDataSet(entradas, "");
        dataSet.setColors(
                ColorTemplate.MATERIAL_COLORS[0],
                ColorTemplate.MATERIAL_COLORS[1],
                ColorTemplate.MATERIAL_COLORS[2],
                ColorTemplate.MATERIAL_COLORS[3],
                ColorTemplate.COLORFUL_COLORS[0],
                ColorTemplate.COLORFUL_COLORS[1]
        );
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));
        pieChart.setUsePercentValues(true);
        pieChart.setData(data);
        pieChart.setVisibility(View.VISIBLE);
        pieChart.animateY(800);
    }

    // -----------------------------------------------------------------------
    // Balances
    // -----------------------------------------------------------------------

    private void mostrarBalances(JsonArray balances) {
        contenedorBalances.removeAllViews();

        if (balances == null || balances.size() == 0) {
            tvSinDeudas.setVisibility(View.VISIBLE);
            return;
        }
        tvSinDeudas.setVisibility(View.GONE);

        int px8  = dp(8);
        int px12 = dp(12);
        int px16 = dp(16);

        for (int i = 0; i < balances.size(); i++) {
            JsonObject b       = balances.get(i).getAsJsonObject();
            String deudor      = b.get("deudor").getAsString();
            String acreedor    = b.get("acreedor").getAsString();
            double monto       = b.get("monto").getAsDouble();
            boolean esMiDeuda  = b.get("es_mi_deuda").getAsBoolean();
            boolean meDeben    = b.get("me_deben").getAsBoolean();
            String estado      = b.has("estado_pago") ? b.get("estado_pago").getAsString() : "pendiente";
            String descripcion = b.has("descripcion") ? b.get("descripcion").getAsString() : "";
            int gastoId        = b.get("gasto_id").getAsInt();
            int deudorId       = b.get("deudor_id").getAsInt();
            String comprobante = (b.has("comprobante") && !b.get("comprobante").isJsonNull())
                    ? b.get("comprobante").getAsString() : null;

            // Color de fondo y etiqueta según rol + estado
            int cardColor;
            String labelTipo;
            int labelColor;

            if (esMiDeuda) {
                if ("en_revision".equals(estado)) {
                    cardColor  = 0xFFFFF8E1;
                    labelTipo  = "ENVIADO - EN REVISIÓN";
                    labelColor = 0xFFE65100;
                } else {
                    cardColor  = 0xFFFFF3F3;
                    labelTipo  = "DEBES PAGAR";
                    labelColor = requireContext().getColor(R.color.error_red);
                }
            } else {
                if ("en_revision".equals(estado)) {
                    cardColor  = 0xFFE8F0FE;
                    labelTipo  = "PAGO POR CONFIRMAR";
                    labelColor = 0xFF1A73E8;
                } else {
                    cardColor  = 0xFFF1FAF1;
                    labelTipo  = "TE DEBEN";
                    labelColor = requireContext().getColor(R.color.success_green);
                }
            }

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, px12);
            card.setLayoutParams(cardLp);
            card.setRadius(dp(12));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(cardColor);

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(px16, px12, px16, px12);

            // Etiqueta de tipo
            TextView tvTipo = new TextView(requireContext());
            tvTipo.setTextSize(11f);
            tvTipo.setTypeface(null, Typeface.BOLD);
            tvTipo.setPadding(0, 0, 0, dp(4));
            tvTipo.setText(labelTipo);
            tvTipo.setTextColor(labelColor);
            inner.addView(tvTipo);

            // Descripción del gasto
            if (!descripcion.isEmpty()) {
                TextView tvDesc = new TextView(requireContext());
                tvDesc.setText(descripcion);
                tvDesc.setTextSize(12f);
                tvDesc.setTextColor(requireContext().getColor(R.color.gray_text));
                tvDesc.setPadding(0, 0, 0, dp(6));
                inner.addView(tvDesc);
            }

            // Fila avatares deudor → acreedor
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

            fila.addView(crearAvatar(deudor));

            TextView tvDeudor = new TextView(requireContext());
            tvDeudor.setText(esMiDeuda ? "Tú" : deudor.split(" ")[0]);
            tvDeudor.setTextSize(15f);
            tvDeudor.setTypeface(null, Typeface.BOLD);
            tvDeudor.setTextColor(requireContext().getColor(R.color.colorPrimaryDark));
            tvDeudor.setPadding(px8, 0, px8, 0);
            fila.addView(tvDeudor);

            TextView tvFlecha = new TextView(requireContext());
            tvFlecha.setText("→");
            tvFlecha.setTextSize(18f);
            tvFlecha.setTextColor(requireContext().getColor(R.color.gray_text));
            tvFlecha.setPadding(0, 0, px8, 0);
            fila.addView(tvFlecha);

            fila.addView(crearAvatar(acreedor));

            TextView tvAcreedor = new TextView(requireContext());
            tvAcreedor.setText(meDeben ? "Ti" : acreedor.split(" ")[0]);
            tvAcreedor.setTextSize(15f);
            tvAcreedor.setTypeface(null, Typeface.BOLD);
            tvAcreedor.setTextColor(requireContext().getColor(R.color.colorPrimaryDark));
            tvAcreedor.setPadding(px8, 0, 0, 0);
            fila.addView(tvAcreedor);

            inner.addView(fila);

            // Fila nombres completos + monto
            LinearLayout filaDetalle = new LinearLayout(requireContext());
            filaDetalle.setOrientation(LinearLayout.HORIZONTAL);
            filaDetalle.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams detalleLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            detalleLp.setMargins(0, dp(6), 0, 0);
            filaDetalle.setLayoutParams(detalleLp);

            TextView tvNombres = new TextView(requireContext());
            tvNombres.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvNombres.setText(deudor + " → " + acreedor);
            tvNombres.setTextSize(12f);
            tvNombres.setTextColor(requireContext().getColor(R.color.gray_text));
            filaDetalle.addView(tvNombres);

            TextView tvMonto = new TextView(requireContext());
            tvMonto.setText("S/. " + String.format("%.2f", monto));
            tvMonto.setTextSize(18f);
            tvMonto.setTypeface(null, Typeface.BOLD);
            tvMonto.setTextColor(esMiDeuda ? requireContext().getColor(R.color.error_red)
                    : requireContext().getColor(R.color.success_green));
            filaDetalle.addView(tvMonto);

            inner.addView(filaDetalle);

            // Botón o texto según rol + estado
            if (esMiDeuda) {
                if ("pendiente".equals(estado)) {
                    Button btnSubir = new Button(requireContext());
                    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    btnLp.setMargins(0, dp(10), 0, dp(2));
                    btnSubir.setLayoutParams(btnLp);
                    btnSubir.setText("Subir comprobante de pago");
                    btnSubir.setTextSize(13f);
                    btnSubir.setOnClickListener(v -> {
                        pendingGastoId = gastoId;
                        galleryLauncher.launch("image/*");
                    });
                    inner.addView(btnSubir);
                } else {
                    // en_revision
                    TextView tvEspera = new TextView(requireContext());
                    LinearLayout.LayoutParams espLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    espLp.setMargins(0, dp(10), 0, dp(2));
                    tvEspera.setLayoutParams(espLp);
                    tvEspera.setText("Comprobante enviado · Esperando confirmación del acreedor");
                    tvEspera.setTextSize(12f);
                    tvEspera.setTextColor(0xFFE65100);
                    tvEspera.setTypeface(null, Typeface.ITALIC);
                    inner.addView(tvEspera);
                }
            } else if (meDeben && "en_revision".equals(estado) && comprobante != null) {
                Button btnRevisar = new Button(requireContext());
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                btnLp.setMargins(0, dp(10), 0, dp(2));
                btnRevisar.setLayoutParams(btnLp);
                btnRevisar.setText("Ver comprobante y confirmar");
                btnRevisar.setTextSize(13f);
                final int fDeudorId   = deudorId;
                final String fComprob = comprobante;
                btnRevisar.setOnClickListener(v ->
                        mostrarDialogRevisar(gastoId, fDeudorId, fComprob));
                inner.addView(btnRevisar);
            }

            card.addView(inner);
            contenedorBalances.addView(card);
        }
    }

    // -----------------------------------------------------------------------
    // Subir comprobante (deudor)
    // -----------------------------------------------------------------------

    private void subirComprobante(int gastoId, Uri uri) {
        Toast.makeText(requireContext(), "Subiendo comprobante...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                is.close();
                byte[] bytes = baos.toByteArray();

                String mime = requireContext().getContentResolver().getType(uri);
                if (mime == null) mime = "image/jpeg";
                String ext = mime.contains("png") ? "png" : mime.contains("webp") ? "webp" : "jpg";

                RequestBody reqFile  = RequestBody.create(MediaType.parse(mime), bytes);
                MultipartBody.Part part = MultipartBody.Part.createFormData(
                        "comprobante", "comprobante." + ext, reqFile);
                RequestBody idBody   = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(gastoId));

                SessionManager session = new SessionManager(requireContext());

                requireActivity().runOnUiThread(() ->
                        ApiClient.getService()
                                .subirComprobanteGasto(session.getBearerToken(), idBody, part)
                                .enqueue(new Callback<JsonObject>() {
                                    @Override
                                    public void onResponse(Call<JsonObject> call, Response<JsonObject> r) {
                                        if (!isAdded()) return;
                                        if (r.isSuccessful()) {
                                            Toast.makeText(requireContext(),
                                                    "Comprobante enviado. El acreedor debe confirmarlo.",
                                                    Toast.LENGTH_LONG).show();
                                            cargarDatos(grupoId);
                                        } else {
                                            Toast.makeText(requireContext(),
                                                    "Error al subir el comprobante", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    @Override
                                    public void onFailure(Call<JsonObject> call, Throwable t) {
                                        if (!isAdded()) return;
                                        Toast.makeText(requireContext(),
                                                "Sin conexión", Toast.LENGTH_SHORT).show();
                                    }
                                })
                );
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Error al leer la imagen", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    // -----------------------------------------------------------------------
    // Revisar comprobante (acreedor)
    // -----------------------------------------------------------------------

    private void mostrarDialogRevisar(int gastoId, int deudorId, String comprobante) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(12), dp(16), dp(8));

        TextView tvCargando = new TextView(requireContext());
        tvCargando.setText("Cargando imagen...");
        tvCargando.setTextSize(13f);
        tvCargando.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvCargando);

        ImageView imageView = new ImageView(requireContext());
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(320));
        imgLp.setMargins(0, dp(8), 0, 0);
        imageView.setLayoutParams(imgLp);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setVisibility(View.GONE);
        layout.addView(imageView);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Comprobante de pago")
                .setView(layout)
                .setPositiveButton("Confirmar pago", (d, w) -> revisarPago(gastoId, deudorId, "confirmar"))
                .setNegativeButton("Rechazar", (d, w) -> revisarPago(gastoId, deudorId, "rechazar"))
                .setNeutralButton("Cerrar", null)
                .create();
        dialog.show();

        // Cargar imagen en background
        String imageUrl = ApiClient.BASE_URL + comprobante;
        new Thread(() -> {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(new URL(imageUrl).openStream());
                if (isAdded() && dialog.isShowing()) {
                    requireActivity().runOnUiThread(() -> {
                        tvCargando.setVisibility(View.GONE);
                        imageView.setImageBitmap(bmp);
                        imageView.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            tvCargando.setText("No se pudo cargar la imagen"));
                }
            }
        }).start();
    }

    private void revisarPago(int gastoId, int deudorId, String accion) {
        SessionManager session = new SessionManager(requireContext());
        JsonObject body = new JsonObject();
        body.addProperty("gasto_id",  gastoId);
        body.addProperty("deudor_id", deudorId);
        body.addProperty("accion",    accion);

        ApiClient.getService().revisarPagoGasto(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> r) {
                        if (!isAdded()) return;
                        String msg = "confirmar".equals(accion)
                                ? "Pago confirmado" : "Pago rechazado — el deudor deberá reenviar comprobante";
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        cargarDatos(grupoId);
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private TextView crearAvatar(String nombre) {
        TextView tv = new TextView(requireContext());
        int size = dp(32);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        tv.setLayoutParams(lp);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(13f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(requireContext().getColor(R.color.white));
        tv.setBackground(requireContext().getDrawable(R.drawable.bg_avatar));
        tv.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        return tv;
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
