package com.example.aplicacionchancha;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgregarGastoActivity extends AppCompatActivity {

    private TextInputLayout   tilDescripcion, tilMonto;
    private TextInputEditText etDescripcion, etMonto;
    private LinearLayout      llPagadores, llPartes;
    private RadioGroup        rgDivision;
    private RadioGroup        rgPagadores;
    private ProgressBar       progressBar;
    private SessionManager    session;
    private int               grupoId;
    private boolean           isAdjusting  = false; // evita recursión en auto-ajuste

    private final List<JsonObject>        miembros     = new ArrayList<>();
    private final List<TextInputEditText> camposPartes = new ArrayList<>();
    private final List<TextInputLayout>   tilesPartes  = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_gasto);

        session = new SessionManager(this);
        grupoId = getIntent().getIntExtra("grupo_id", 0);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilDescripcion = findViewById(R.id.tilDescripcion);
        tilMonto       = findViewById(R.id.tilMonto);
        etDescripcion  = findViewById(R.id.etDescripcion);
        etMonto        = findViewById(R.id.etMonto);
        llPagadores    = findViewById(R.id.llPagadores);
        llPartes       = findViewById(R.id.llPartes);
        rgDivision     = findViewById(R.id.rgDivision);
        progressBar    = findViewById(R.id.progressBar);

        // Recalcular al cambiar el total según el modo activo
        etMonto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {
                int checkedId = rgDivision.getCheckedRadioButtonId();
                if (checkedId == R.id.rbEquitativa) {
                    recalcularEquitativa();
                } else if (checkedId == R.id.rbPersonalizada) {
                    autoAjustarUltimoCampo(); // total cambió → ajustar último campo
                }
            }
        });

        rgDivision.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbEquitativa) {
                configurarModoPartes(false);
                recalcularEquitativa();
                bloquearCamposPartes(true);
            } else if (checkedId == R.id.rbPorcentaje) {
                configurarModoPartes(true);
                bloquearCamposPartes(false);
                rellenarPorcentajeIgual();
            } else {                              // personalizada
                configurarModoPartes(false);
                bloquearCamposPartes(false);
            }
        });

        findViewById(R.id.btnRegistrar).setOnClickListener(v -> registrarGasto());
        cargarMiembros();
    }

    // -------------------------------------------------------------------------
    // Carga de miembros
    // -------------------------------------------------------------------------

    private void cargarMiembros() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getService().miembrosGrupo(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            JsonArray arr = response.body().getAsJsonArray("miembros");
                            miembros.clear();
                            for (int i = 0; i < arr.size(); i++)
                                miembros.add(arr.get(i).getAsJsonObject());
                            construirFormulario();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AgregarGastoActivity.this, "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Construcción del formulario
    // -------------------------------------------------------------------------

    private void construirFormulario() {
        llPagadores.removeAllViews();
        llPartes.removeAllViews();
        camposPartes.clear();
        tilesPartes.clear();

        rgPagadores = new RadioGroup(this);
        rgPagadores.setOrientation(RadioGroup.VERTICAL);
        int defaultCheckId = View.NO_ID;

        for (JsonObject m : miembros) {
            int    uid    = m.get("id").getAsInt();
            String nombre = m.get("nombre").getAsString();

            // RadioButton del pagador
            int rbId = View.generateViewId();
            RadioButton rb = new RadioButton(this);
            rb.setId(rbId);
            rb.setTag(uid);
            rb.setText(nombre + (uid == session.getUserId() ? " (tú)" : ""));
            rb.setTextSize(14f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 4, 0, 4);
            rb.setLayoutParams(lp);
            rgPagadores.addView(rb);
            if (uid == session.getUserId()) defaultCheckId = rbId;

            // Campo de monto/porcentaje por miembro
            TextInputLayout til = new TextInputLayout(this, null,
                    com.google.android.material.R.style
                            .Widget_Material3_TextInputLayout_OutlinedBox);
            til.setHint(nombre);
            LinearLayout.LayoutParams tilLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tilLp.setMargins(0, 8, 0, 8);
            til.setLayoutParams(tilLp);

            TextInputEditText et = new TextInputEditText(til.getContext());
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            et.setTag(uid);

            // Auto-ajustar último campo cuando cualquier campo anterior cambia
            final int myIndex = camposPartes.size();
            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
                @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
                @Override public void afterTextChanged(Editable s) {
                    int checkedId = rgDivision.getCheckedRadioButtonId();
                    if (!isAdjusting
                            && myIndex < camposPartes.size() - 1
                            && (checkedId == R.id.rbPersonalizada || checkedId == R.id.rbPorcentaje)) {
                        autoAjustarUltimoCampo();
                    }
                }
            });

            til.addView(et);
            llPartes.addView(til);
            camposPartes.add(et);
            tilesPartes.add(til);
        }

        llPagadores.addView(rgPagadores);
        if (defaultCheckId != View.NO_ID) {
            rgPagadores.check(defaultCheckId);
        } else if (rgPagadores.getChildCount() > 0) {
            rgPagadores.check(rgPagadores.getChildAt(0).getId());
        }

        recalcularEquitativa();
        bloquearCamposPartes(true);
    }

    // -------------------------------------------------------------------------
    // Helpers de división
    // -------------------------------------------------------------------------

    /** Cambia los hints y el sufijo de los campos según el modo activo. */
    private void configurarModoPartes(boolean esPorcentaje) {
        for (int i = 0; i < tilesPartes.size(); i++) {
            String nombre = miembros.get(i).get("nombre").getAsString();
            tilesPartes.get(i).setHint(esPorcentaje ? nombre + " (%)" : nombre);
            tilesPartes.get(i).setSuffixText(esPorcentaje ? "%" : null);
        }
    }

    /** Rellena los campos con porcentajes iguales (suma = 100). */
    private void rellenarPorcentajeIgual() {
        if (miembros.isEmpty()) return;
        int    n         = miembros.size();
        double base      = Math.floor(100.0 * 100 / n) / 100.0;
        double remainder = Math.round((100.0 - base * n) * 100) / 100.0;
        for (int i = 0; i < camposPartes.size(); i++) {
            double pct = (i == 0) ? base + remainder : base;
            camposPartes.get(i).setText(String.format("%.2f", pct));
        }
    }

    /**
     * Ajusta el último campo para que la suma total coincida.
     * Se llama automáticamente cuando cambia el total o cualquier campo en modo personalizada.
     */
    private void autoAjustarUltimoCampo() {
        if (camposPartes.size() < 2) return;
        int checkedId = rgDivision.getCheckedRadioButtonId();

        // Referencia: 100 para porcentaje, monto total para personalizada
        double referencia;
        if (checkedId == R.id.rbPorcentaje) {
            referencia = 100.0;
        } else if (checkedId == R.id.rbPersonalizada) {
            String montoStr = etMonto.getText() != null
                    ? etMonto.getText().toString().replace(",", ".").trim() : "";
            if (TextUtils.isEmpty(montoStr)) return;
            try { referencia = Double.parseDouble(montoStr); } catch (NumberFormatException e) { return; }
        } else {
            return; // equitativa se maneja aparte
        }

        double sum = 0;
        for (int i = 0; i < camposPartes.size() - 1; i++) {
            String pStr = camposPartes.get(i).getText() != null
                    ? camposPartes.get(i).getText().toString().replace(",", ".").trim() : "0";
            try { sum += Double.parseDouble(pStr); } catch (NumberFormatException e) {}
        }
        double last = Math.round((referencia - sum) * 100) / 100.0;
        isAdjusting = true;
        camposPartes.get(camposPartes.size() - 1).setText(
                last >= 0 ? String.format("%.2f", last) : "0.00");
        isAdjusting = false;
    }

    /** Recalcula montos iguales a partir del total ingresado. */
    private void recalcularEquitativa() {
        if (miembros.isEmpty()) return;
        String montoStr = etMonto.getText() != null ? etMonto.getText().toString().trim() : "";
        if (TextUtils.isEmpty(montoStr)) {
            for (TextInputEditText et : camposPartes) et.setText("");
            return;
        }
        try {
            double total     = Double.parseDouble(montoStr);
            int    n         = miembros.size();
            double base      = Math.floor(total * 100 / n) / 100.0;
            double remainder = Math.round((total - base * n) * 100) / 100.0;
            for (int i = 0; i < camposPartes.size(); i++)
                camposPartes.get(i).setText(String.format("%.2f", (i == 0) ? base + remainder : base));
        } catch (NumberFormatException ignored) {}
    }

    private void bloquearCamposPartes(boolean bloquear) {
        for (TextInputEditText et : camposPartes) {
            et.setEnabled(!bloquear);
            et.setFocusable(!bloquear);
            et.setFocusableInTouchMode(!bloquear);
        }
    }

    private int getPagadorSeleccionado() {
        int checkedId = rgPagadores.getCheckedRadioButtonId();
        if (checkedId == View.NO_ID) return -1;
        RadioButton rb = rgPagadores.findViewById(checkedId);
        return rb != null ? (int) rb.getTag() : -1;
    }

    // -------------------------------------------------------------------------
    // Registrar
    // -------------------------------------------------------------------------

    private void registrarGasto() {
        tilDescripcion.setError(null);
        tilMonto.setError(null);

        String descripcion = etDescripcion.getText() != null
                ? etDescripcion.getText().toString().trim() : "";
        String montoStr    = etMonto.getText() != null
                ? etMonto.getText().toString().replace(",", ".").trim() : "";

        if (TextUtils.isEmpty(descripcion)) { tilDescripcion.setError("Ingresa una descripción"); return; }
        if (TextUtils.isEmpty(montoStr))    { tilMonto.setError("Ingresa el monto"); return; }

        double montoTotal;
        try {
            montoTotal = Double.parseDouble(montoStr);
            if (montoTotal <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) { tilMonto.setError("Monto inválido"); return; }

        int pagadoPorId = getPagadorSeleccionado();
        if (pagadoPorId == -1) {
            Toast.makeText(this, "Selecciona quién pagó", Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedDivision = rgDivision.getCheckedRadioButtonId();
        boolean esPorcentaje   = checkedDivision == R.id.rbPorcentaje;
        boolean esPersonalizada = checkedDivision == R.id.rbPersonalizada;

        JsonArray partes = new JsonArray();

        if (esPorcentaje) {
            // ── Modo porcentaje: leer %, validar suma=100, calcular montos ──
            double[] porcentajes = new double[miembros.size()];
            double   sumaPct     = 0;
            for (int i = 0; i < miembros.size(); i++) {
                String pStr = camposPartes.get(i).getText() != null
                        ? camposPartes.get(i).getText().toString().replace(",", ".").trim() : "0";
                try { porcentajes[i] = Double.parseDouble(pStr); }
                catch (NumberFormatException e) { porcentajes[i] = 0; }
                sumaPct += porcentajes[i];
            }
            if (Math.abs(sumaPct - 100.0) > 0.1) {
                Toast.makeText(this,
                        String.format("Los porcentajes suman %.2f%% (deben sumar 100%%)", sumaPct),
                        Toast.LENGTH_LONG).show();
                return;
            }
            for (int i = 0; i < miembros.size(); i++) {
                int    uid   = miembros.get(i).get("id").getAsInt();
                double monto = Math.round((porcentajes[i] / 100.0) * montoTotal * 100) / 100.0;
                JsonObject p = new JsonObject();
                p.addProperty("usuario_id",     uid);
                p.addProperty("monto_asignado", monto);
                p.addProperty("porcentaje",     porcentajes[i]); // guardar el % original
                partes.add(p);
            }
        } else {
            // ── Modo equitativo o personalizado: validar suma de montos ──
            double sumaPartes = 0;
            for (int i = 0; i < miembros.size(); i++) {
                int    uid  = miembros.get(i).get("id").getAsInt();
                String pStr = camposPartes.get(i).getText() != null
                        ? camposPartes.get(i).getText().toString().replace(",", ".").trim() : "0";
                double monto;
                try { monto = Double.parseDouble(pStr); } catch (NumberFormatException e) { monto = 0; }
                sumaPartes += monto;
                JsonObject p = new JsonObject();
                p.addProperty("usuario_id",     uid);
                p.addProperty("monto_asignado", monto);
                partes.add(p);
            }
            if (Math.abs(sumaPartes - montoTotal) > 0.01) {
                Toast.makeText(this,
                        String.format("La suma (S/. %.2f) no coincide con el total (S/. %.2f)",
                                sumaPartes, montoTotal),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        String tipoDivision = esPorcentaje ? "porcentaje"
                : (esPersonalizada ? "personalizada" : "equitativa");

        JsonObject body = new JsonObject();
        body.addProperty("grupo_id",      grupoId);
        body.addProperty("descripcion",   descripcion);
        body.addProperty("monto_total",   montoTotal);
        body.addProperty("pagado_por_id", pagadoPorId);
        body.addProperty("tipo_division", tipoDivision);
        body.add("partes", partes);

        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnRegistrar).setEnabled(false);

        ApiClient.getService().registrarGasto(session.getBearerToken(), body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        progressBar.setVisibility(View.GONE);
                        findViewById(R.id.btnRegistrar).setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(AgregarGastoActivity.this,
                                    "Gasto registrado", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String msg = "Error al registrar";
                            try {
                                JsonObject err = new com.google.gson.JsonParser()
                                        .parse(response.errorBody().string()).getAsJsonObject();
                                if (err.has("error")) msg = err.get("error").getAsString();
                            } catch (Exception ignored) {}
                            Toast.makeText(AgregarGastoActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        findViewById(R.id.btnRegistrar).setEnabled(true);
                        Toast.makeText(AgregarGastoActivity.this, "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
