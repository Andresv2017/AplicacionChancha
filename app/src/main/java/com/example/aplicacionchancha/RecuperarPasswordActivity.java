package com.example.aplicacionchancha;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class RecuperarPasswordActivity extends AppCompatActivity {

    private TextInputEditText etCorreo;
    private Button btnEnviar;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_password);

        etCorreo    = findViewById(R.id.etCorreo);
        btnEnviar   = findViewById(R.id.btnEnviar);
        progressBar = findViewById(R.id.progressBar);

        btnEnviar.setOnClickListener(v -> enviarRecuperacion());

        TextView tvVolver = findViewById(R.id.tvVolver);
        tvVolver.setOnClickListener(v -> finish());
    }

    private void enviarRecuperacion() {
        String correo = etCorreo.getText().toString().trim();

        if (correo.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_campos_vacios), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!correo.toLowerCase().endsWith(".edu.pe")) {
            Toast.makeText(this, getString(R.string.error_correo_invalido), Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnEnviar.setEnabled(false);

        new android.os.Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            btnEnviar.setEnabled(true);
            Toast.makeText(this,
                    "Si el correo está registrado, recibirás las instrucciones.",
                    Toast.LENGTH_LONG).show();
        }, 1500);
    }
}
