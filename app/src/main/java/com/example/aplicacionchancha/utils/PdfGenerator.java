package com.example.aplicacionchancha.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfGenerator {

    // A4 en puntos a 72 DPI: 595 x 842
    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;
    private static final int MARGIN = 40;

    public static File generarResumenGrupo(Context context,
                                           String grupoNombre,
                                           JsonArray gastos,
                                           JsonArray balances) throws IOException {
        PdfDocument doc  = new PdfDocument();
        Paint       p    = new Paint();
        Paint       line = new Paint();
        line.setColor(Color.parseColor("#DDDDDD"));
        line.setStrokeWidth(1);

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create();
        PdfDocument.Page page   = doc.startPage(pageInfo);
        Canvas        canvas    = page.getCanvas();

        int y = MARGIN;

        // ── Encabezado ────────────────────────────────────────────────────
        p.setColor(Color.parseColor("#1565C0"));
        p.setTextSize(22);
        p.setFakeBoldText(true);
        canvas.drawText("Chancha – Resumen de grupo", MARGIN, y + 22, p);

        p.setColor(Color.parseColor("#757575"));
        p.setTextSize(12);
        p.setFakeBoldText(false);
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Exportado: " + fecha, MARGIN, y + 42, p);
        y += 60;

        // Nombre del grupo
        p.setColor(Color.parseColor("#0D47A1"));
        p.setTextSize(16);
        p.setFakeBoldText(true);
        canvas.drawText("Grupo: " + grupoNombre, MARGIN, y, p);
        y += 6;
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line);
        y += 18;

        // ── Sección Gastos ────────────────────────────────────────────────
        p.setColor(Color.parseColor("#1565C0"));
        p.setTextSize(14);
        p.setFakeBoldText(true);
        canvas.drawText("Gastos recientes", MARGIN, y, p);
        y += 14;

        // Cabecera tabla
        p.setColor(Color.parseColor("#E3F2FD"));
        canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 20, p);
        p.setColor(Color.parseColor("#0D47A1"));
        p.setTextSize(10);
        p.setFakeBoldText(true);
        canvas.drawText("Descripción",   MARGIN + 4,   y + 14, p);
        canvas.drawText("Fecha",         MARGIN + 220, y + 14, p);
        canvas.drawText("Pagó",          MARGIN + 300, y + 14, p);
        canvas.drawText("Monto",         MARGIN + 440, y + 14, p);
        y += 22;

        // Filas de gastos
        p.setFakeBoldText(false);
        p.setTextSize(10);
        boolean altRow = false;
        for (int i = 0; i < gastos.size() && y < PAGE_H - 180; i++) {
            JsonObject g = gastos.get(i).getAsJsonObject();
            String desc  = truncar(g.get("descripcion").getAsString(), 30);
            String fec   = g.has("fecha") && !g.get("fecha").isJsonNull()
                            ? g.get("fecha").getAsString().substring(0, 10) : "-";
            String pago  = truncar(g.get("pagado_por_nombre").getAsString(), 16);
            String monto = "S/. " + String.format("%.2f", g.get("monto_total").getAsDouble());

            if (altRow) {
                p.setColor(Color.parseColor("#F5F7FA"));
                canvas.drawRect(MARGIN, y - 12, PAGE_W - MARGIN, y + 6, p);
            }
            p.setColor(Color.parseColor("#212121"));
            canvas.drawText(desc,  MARGIN + 4,   y, p);
            canvas.drawText(fec,   MARGIN + 220, y, p);
            canvas.drawText(pago,  MARGIN + 300, y, p);
            canvas.drawText(monto, MARGIN + 440, y, p);
            y += 18;
            altRow = !altRow;
        }
        if (gastos.size() == 0) {
            p.setColor(Color.parseColor("#757575"));
            canvas.drawText("Sin gastos registrados", MARGIN + 4, y, p);
            y += 18;
        }

        y += 16;
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line);
        y += 18;

        // ── Sección Balances ──────────────────────────────────────────────
        if (y < PAGE_H - 100 && balances != null && balances.size() > 0) {
            p.setColor(Color.parseColor("#1565C0"));
            p.setTextSize(14);
            p.setFakeBoldText(true);
            canvas.drawText("Balances", MARGIN, y, p);
            y += 14;

            p.setColor(Color.parseColor("#E3F2FD"));
            canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 20, p);
            p.setColor(Color.parseColor("#0D47A1"));
            p.setTextSize(10);
            canvas.drawText("De",   MARGIN + 4,   y + 14, p);
            canvas.drawText("A",    MARGIN + 180, y + 14, p);
            canvas.drawText("Monto", MARGIN + 380, y + 14, p);
            y += 22;

            p.setFakeBoldText(false);
            altRow = false;
            for (int i = 0; i < balances.size() && y < PAGE_H - 60; i++) {
                JsonObject b = balances.get(i).getAsJsonObject();
                String de    = truncar(b.has("deudor_nombre")   ? b.get("deudor_nombre").getAsString()   : "-", 24);
                String a     = truncar(b.has("acreedor_nombre") ? b.get("acreedor_nombre").getAsString() : "-", 24);
                String amt   = "S/. " + String.format("%.2f",
                        b.has("monto") ? b.get("monto").getAsDouble() : 0.0);

                if (altRow) {
                    p.setColor(Color.parseColor("#F5F7FA"));
                    canvas.drawRect(MARGIN, y - 12, PAGE_W - MARGIN, y + 6, p);
                }
                p.setColor(Color.parseColor("#212121"));
                canvas.drawText(de,  MARGIN + 4,   y, p);
                canvas.drawText(a,   MARGIN + 180, y, p);
                canvas.drawText(amt, MARGIN + 380, y, p);
                y += 18;
                altRow = !altRow;
            }
        } else if (y < PAGE_H - 60) {
            p.setColor(Color.parseColor("#2E7D32"));
            p.setTextSize(12);
            p.setFakeBoldText(true);
            canvas.drawText("¡Sin deudas pendientes en el grupo!", MARGIN, y, p);
        }

        // Pie de página
        p.setColor(Color.parseColor("#BDBDBD"));
        p.setTextSize(9);
        p.setFakeBoldText(false);
        canvas.drawText("Generado por Chancha App", MARGIN, PAGE_H - 20, p);

        doc.finishPage(page);

        // Guardar en cache
        File cacheDir = context.getCacheDir();
        String nombre = "chancha_" + grupoNombre.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
        File file = new File(cacheDir, nombre);
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.writeTo(out);
        }
        doc.close();
        return file;
    }

    private static String truncar(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
