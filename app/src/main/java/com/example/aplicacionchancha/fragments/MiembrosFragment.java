package com.example.aplicacionchancha.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicacionchancha.R;
import com.example.aplicacionchancha.adapters.MemberAdapter;
import com.example.aplicacionchancha.network.ApiClient;
import com.example.aplicacionchancha.utils.SessionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MiembrosFragment extends Fragment {

    private static final String ARG_GRUPO_ID = "grupo_id";
    private static final String ARG_CODIGO   = "codigo_invitacion";

    private RecyclerView rvMiembros;
    private TextView tvCodigo;

    public static MiembrosFragment newInstance(int grupoId, String codigo) {
        MiembrosFragment f = new MiembrosFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_GRUPO_ID, grupoId);
        args.putString(ARG_CODIGO, codigo);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_miembros, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMiembros = view.findViewById(R.id.rvMiembros);
        tvCodigo   = view.findViewById(R.id.tvCodigo);
        rvMiembros.setLayoutManager(new LinearLayoutManager(requireContext()));

        String codigo = getArguments() != null ? getArguments().getString(ARG_CODIGO) : null;
        if (codigo != null) {
            tvCodigo.setText(codigo);
            view.findViewById(R.id.cardCodigo).setOnClickListener(v -> {
                ClipboardManager cb = (ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("codigo", codigo));
                Toast.makeText(requireContext(), "Código copiado", Toast.LENGTH_SHORT).show();
            });
        }

        int grupoId = getArguments() != null ? getArguments().getInt(ARG_GRUPO_ID) : 0;
        cargarMiembros(grupoId);
    }

    private void cargarMiembros(int grupoId) {
        SessionManager session = new SessionManager(requireContext());

        ApiClient.getService().miembrosGrupo(session.getBearerToken(), grupoId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            JsonArray arr = response.body().getAsJsonArray("miembros");
                            int usuarioActualId = response.body().get("usuario_actual").getAsInt();
                            List<JsonObject> lista = new ArrayList<>();
                            for (int i = 0; i < arr.size(); i++) {
                                lista.add(arr.get(i).getAsJsonObject());
                            }
                            rvMiembros.setAdapter(new MemberAdapter(lista, usuarioActualId));
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }
}
