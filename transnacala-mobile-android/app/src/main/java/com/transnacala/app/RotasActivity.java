package com.transnacala.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.transnacala.app.adapter.RotaAdapter;
import com.transnacala.app.database.RotaDAO;
import com.transnacala.app.model.Rota;
import com.transnacala.app.network.ApiConfig;
import com.transnacala.app.network.SyncScheduler;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class RotasActivity extends AppCompatActivity {

    private RecyclerView recyclerRotas;
    private RotaAdapter adapter;
    private final List<Rota> rotas = new ArrayList<>();
    private TextView syncStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rotas);

        recyclerRotas = findViewById(R.id.recyclerRotas);

        recyclerRotas.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rotas.addAll(new RotaDAO(this).listarTodas());
        syncStatus = findViewById(R.id.txtSyncStatus);
        findViewById(R.id.btnSyncRotas).setOnClickListener(v -> sincronizar());
        findViewById(R.id.btnLiveMap).setOnClickListener(v ->
                startActivity(new Intent(RotasActivity.this, LiveMapActivity.class))
        );

        adapter = new RotaAdapter(
                rotas,
                rota -> {

                    Intent intent =
                            new Intent(RotasActivity.this,
                                    DetalhesRotaActivity.class);

                    intent.putExtra("rota", rota);

                    startActivity(intent);
                }
        );

        recyclerRotas.setAdapter(adapter);
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData("transnacala_catalog_sync")
                .observe(this, this::atualizarEstadoSincronizacao);
        sincronizar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarRotas();
        sincronizar();
    }

    private void sincronizar() {
        syncStatus.setText("A sincronizar catálogo...");
        SyncScheduler.syncNow(this);
    }

    private void atualizarRotas() {
        if (adapter == null) {
            return;
        }
        rotas.clear();
        rotas.addAll(new RotaDAO(this).listarTodas());
        adapter.notifyDataSetChanged();
    }

    private void atualizarEstadoSincronizacao(List<WorkInfo> workInfos) {
        if (workInfos == null || workInfos.isEmpty()) {
            return;
        }
        WorkInfo.State state = workInfos.get(0).getState();
        if (state == WorkInfo.State.SUCCEEDED) {
            atualizarRotas();
            syncStatus.setText("Catálogo atualizado. As rotas guardadas continuam disponíveis offline.");
        } else if (state == WorkInfo.State.FAILED) {
            syncStatus.setText("Não foi possível sincronizar. As rotas guardadas continuam disponíveis.");
        } else if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
            syncStatus.setText("A sincronizar catálogo...");
        }
    }
}