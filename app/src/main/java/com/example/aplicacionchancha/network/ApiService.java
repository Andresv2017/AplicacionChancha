package com.example.aplicacionchancha.network;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {

    // --- AUTH ---
    @POST("auth/register.php")
    Call<JsonObject> register(@Body JsonObject body);

    @POST("auth/firebase_login.php")
    Call<JsonObject> firebaseLogin(@Body JsonObject body);

    @POST("auth/registro_firebase.php")
    Call<JsonObject> registroFirebase(@Body JsonObject body);

    @POST("auth/login.php")
    Call<JsonObject> login(@Body JsonObject body);

    @POST("auth/logout.php")
    Call<JsonObject> logout(@Header("Authorization") String token);

    // --- GRUPOS ---
    @GET("grupos/listar.php")
    Call<JsonObject> listarGrupos(@Header("Authorization") String token);

    @POST("grupos/crear.php")
    Call<JsonObject> crearGrupo(@Header("Authorization") String token, @Body JsonObject body);

    @GET("grupos/detalle.php")
    Call<JsonObject> detalleGrupo(@Header("Authorization") String token, @Query("grupo_id") int grupoId);

    @GET("grupos/miembros.php")
    Call<JsonObject> miembrosGrupo(@Header("Authorization") String token, @Query("grupo_id") int grupoId);

    @POST("grupos/agregar_miembro.php")
    Call<JsonObject> agregarMiembro(@Header("Authorization") String token, @Body JsonObject body);

    @POST("grupos/unirse.php")
    Call<JsonObject> unirseGrupo(@Header("Authorization") String token, @Body JsonObject body);

    @POST("usuario/fcm_token.php")
    Call<JsonObject> guardarFcmToken(@Header("Authorization") String token, @Body JsonObject body);

    // --- USUARIO ---
    @GET("usuario/buscar.php")
    Call<JsonObject> buscarUsuario(@Header("Authorization") String token,
                                   @Query("correo") String correo,
                                   @Query("grupo_id") int grupoId);

    // --- GASTOS ---
    @GET("gastos/listar.php")
    Call<JsonObject> listarGastos(@Header("Authorization") String token, @Query("grupo_id") int grupoId);

    @POST("gastos/registrar.php")
    Call<JsonObject> registrarGasto(@Header("Authorization") String token, @Body JsonObject body);

    @GET("gastos/detalle.php")
    Call<JsonObject> detalleGasto(@Header("Authorization") String token, @Query("gasto_id") int gastoId);

    @POST("gastos/editar.php")
    Call<JsonObject> editarGasto(@Header("Authorization") String token, @Body JsonObject body);

    @POST("gastos/eliminar.php")
    Call<JsonObject> eliminarGasto(@Header("Authorization") String token, @Body JsonObject body);

    @GET("gastos/balances.php")
    Call<JsonObject> balancesGrupo(@Header("Authorization") String token, @Query("grupo_id") int grupoId);

    @Multipart
    @POST("gastos/subir_comprobante.php")
    Call<JsonObject> subirComprobanteGasto(
            @Header("Authorization") String token,
            @Part("gasto_id") RequestBody gastoId,
            @Part MultipartBody.Part comprobante);

    @POST("gastos/revisar_pago.php")
    Call<JsonObject> revisarPagoGasto(@Header("Authorization") String token, @Body JsonObject body);

    // --- COLECTAS ---
    @POST("colectas/crear.php")
    Call<JsonObject> crearColecta(@Header("Authorization") String token, @Body JsonObject body);

    @GET("colectas/listar.php")
    Call<JsonObject> listarColectas(@Header("Authorization") String token, @Query("grupo_id") int grupoId);

    @GET("colectas/detalle.php")
    Call<JsonObject> detalleColecta(@Header("Authorization") String token, @Query("colecta_id") int colectaId);

    @GET("colectas/detalle.php")
    Call<JsonObject> detalleColectaPorQr(@Header("Authorization") String token, @Query("codigo_qr") String codigoQr);

    @Multipart
    @POST("colectas/pagar.php")
    Call<JsonObject> pagarColecta(
            @Header("Authorization") String token,
            @Part("colecta_id") RequestBody colectaId,
            @Part MultipartBody.Part comprobante);

    @POST("colectas/confirmar.php")
    Call<JsonObject> confirmarPago(@Header("Authorization") String token, @Body JsonObject body);

    // --- NOTIFICACIONES ---
    @GET("notificaciones/listar.php")
    Call<JsonObject> listarNotificaciones(@Header("Authorization") String token);

    @POST("notificaciones/marcar_leidas.php")
    Call<JsonObject> marcarNotificacionesLeidas(@Header("Authorization") String token,
                                                @Body JsonObject body);

    // --- PERFIL ---
    @GET("usuario/perfil.php")
    Call<JsonObject> obtenerPerfil(@Header("Authorization") String token);

    @PUT("usuario/perfil.php")
    Call<JsonObject> editarPerfil(@Header("Authorization") String token, @Body JsonObject body);

    // --- AUTH EXTRA ---
    @POST("auth/cambiar_password.php")
    Call<JsonObject> cambiarPassword(@Header("Authorization") String token, @Body JsonObject body);
}
