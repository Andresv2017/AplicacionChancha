package com.example.aplicacionchancha.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME  = "chancha_session";
    private static final String KEY_TOKEN   = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NOMBRE  = "nombre";
    private static final String KEY_CORREO  = "correo";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void guardarSesion(String token, int userId, String nombre, String correo) {
        editor.putString(KEY_TOKEN,   token);
        editor.putInt(KEY_USER_ID,    userId);
        editor.putString(KEY_NOMBRE,  nombre);
        editor.putString(KEY_CORREO,  correo);
        editor.apply();
    }

    public void cerrarSesion() {
        editor.clear().apply();
    }

    public boolean haySesion() {
        return prefs.getString(KEY_TOKEN, null) != null;
    }

    public String getToken()   { return prefs.getString(KEY_TOKEN,  null); }
    public int    getUserId()  { return prefs.getInt(KEY_USER_ID,   -1);   }
    public String getNombre()  { return prefs.getString(KEY_NOMBRE, "");   }
    public String getCorreo()  { return prefs.getString(KEY_CORREO, "");   }
    public String getBearerToken() { return "Bearer " + getToken(); }
}
