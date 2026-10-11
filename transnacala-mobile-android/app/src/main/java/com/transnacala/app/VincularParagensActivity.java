package com.transnacala.app;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.transnacala.app.database.ParagemDAO;
import com.transnacala.app.database.RotaParagemDAO;
import com.transnacala.app.model.Paragem;

import java.util.ArrayList;
import java.util.List;

public class VincularParagensActivity extends AppCompatActivity {

    private int rotaId;
    private String nomeRota;

    private ParagemDAO paragemDAO;
    private RotaParagemDAO rotaParagemDAO;

    private Spinner spParagens;
    private RecyclerView recyclerView;

    private List<Paragem> todasParagens;
    private List<Paragem> paragensDaRota;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vincular_paragens);

        rotaId = getIntent().getIntExtra("ROTA_ID", -1);
        nomeRota = getIntent().getStringExtra("ROTA_NOME");

        if (rotaId == -1) {
            Toast.makeText(this, "Rota inválida!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        paragemDAO = new ParagemDAO(this);
        rotaParagemDAO = new RotaParagemDAO(this);

        TextView tvNomeRota = findViewById(R.id.tvNomeRotaSelecionada);
        spParagens = findViewById(R.id.spParagensDisponiveis);
        recyclerView = findViewById(R.id.rvParagensDaRota);
        MaterialButton btnAdicionar = findViewById(R.id.btnAdicionarParagemNaRota);

        tvNomeRota.setText("Rota: " + nomeRota);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        carregarSpinnerParagens();
        carregarListaParagensDaRota();

        btnAdicionar.setOnClickListener(v -> adicionarParagem());
    }

    private void carregarSpinnerParagens() {
        todasParagens = paragemDAO.listarTodas();
        List<String> nomesParagens = new ArrayList<>();
        for (Paragem p : todasParagens) {
            nomesParagens.add(p.getNome());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nomesParagens);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spParagens.setAdapter(adapter);
    }

    private void carregarListaParagensDaRota() {
        paragensDaRota = rotaParagemDAO.listarParagensDaRota(rotaId);
        // Utiliza o mesmo Adapter de paragens ou um adaptador simples de ordenação
    }

    private void adicionarParagem() {
        int posicaoSelecionada = spParagens.getSelectedItemPosition();
        if (posicaoSelecionada != Spinner.INVALID_POSITION && todasParagens.size() > 0) {
            Paragem paragemEscolhida = todasParagens.get(posicaoSelecionada);

            // A ordem será o próximo número da sequência
            int novaOrdem = paragensDaRota.size() + 1;

            rotaParagemDAO.associarParagem(rotaId, paragemEscolhida.getId(), novaOrdem);
            Toast.makeText(this, "Paragem adicionada à rota!", Toast.LENGTH_SHORT).show();
            carregarListaParagensDaRota();
        }
    }
}