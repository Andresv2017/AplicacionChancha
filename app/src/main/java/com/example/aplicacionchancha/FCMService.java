package com.example.aplicacionchancha;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FCMService extends FirebaseMessagingService {

    private static final String CHANNEL_ID   = "chancha_canal";
    private static final String CHANNEL_NAME = "Notificaciones Chancha";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        SessionManager session = new SessionManager(this);
        if (session.haySesion()) {
            JsonObject body = new JsonObject();
            body.addProperty("fcm_token", token);
            ApiClient.getService().guardarFcmToken(session.getBearerToken(), body)
                    .enqueue(new Callback<JsonObject>() {
                        @Override public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {}
                        @Override public void onFailure(Call<JsonObject> c, Throwable t) {}
                    });
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String titulo  = "Chancha";
        String cuerpo  = "";

        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null)
                titulo = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null)
                cuerpo = message.getNotification().getBody();
        } else if (!message.getData().isEmpty()) {
            titulo = message.getData().getOrDefault("title", titulo);
            cuerpo = message.getData().getOrDefault("body", "");
        }

        mostrarNotificacion(titulo, cuerpo);
    }

    private void mostrarNotificacion(String titulo, String cuerpo) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(canal);
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
