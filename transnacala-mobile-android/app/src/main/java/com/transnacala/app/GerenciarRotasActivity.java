package com.transnacala.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.transnacala.app.adapter.RotasAdminAdapter;
import com.transnacala.app.database.RotaDAO;
import com.transnacala.app.model.Rota;

import java.util.List;

public class GerenciarRotasActivity extends AppCompatActivity {

    private RotaDAO rotaDAO;
    private RecyclerView recyclerView;
    private RotasAdminAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_rotas);

        rotaDAO = new RotaDAO(this);
        recyclerView = findViewById(R.id.rvRotasAdmin);
        View fabAdicionar = findViewById(R.id.fabAdicionarRota);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        carregarRotas();

        fabAdicionar.setOnClickListener(v -> abrirDialogFormulario(null));
    }

    private void carregarRotas() {
        List<Rota> listaRotas = rotaDAO.listarTodas();
        adapter = new RotasAdminAdapter(listaRotas, new RotasAdminAdapter.OnRotaClickListener() {
            @Override
            public void onEditar(Rota rota) {
                abrirDialogFormulario(rota);
            }

            @Override
            public void onEliminar(Rota rota) {
                confirmarEliminacao(rota);
            }

            @Override
            public void onGerirParagens(Rota rota) {
                Intent intent = new Intent(GerenciarRotasActivity.this, VincularParagensActivity.class);
                intent.putExtra("ROTA_ID", rota.getId());
                intent.putExtra("ROTA_NOME", rota.getNome());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void abrirDialogFormulario(Rota rotaExistente) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_formulario_rota, null);

        EditText etNome = view.findViewById(R.id.etNomeRota);
        EditText etOrigem = view.findViewById(R.id.etOrigemRota);
        EditText etDestino = view.findViewById(R.id.etDestinoRota);
        EditText etTarifa = view.findViewById(R.id.etTarifaRota);
        EditText etDescricao = view.findViewById(R.id.etDescricaoRota);

        boolean isEdicao = (rotaExistente != null);

        if (isEdicao) {
            etNome.setText(rotaExistente.getNome());
            etOrigem.setText(rotaExistente.getOrigem());
            etDestino.setText(rotaExistente.getDestino());
            etTarifa.setText(String.valueOf(rotaExistente.getTarifa()));
            etDescricao.setText(rotaExistente.getDescricao());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(isEdicao ? "Editar Rota" : "Nova Rota")
                .setView(view)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String nome = etNome.getText().toString().trim();
                    String origem = etOrigem.getText().toString().trim();
                    String destino = etDestino.getText().toString().trim();
                    String tarifaStr = etTarifa.getText().toString().trim();
                    String descricao = etDescricao.getText().toString().trim();

                    if (nome.isEmpty() || tarifaStr.isEmpty()) {
                        Toast.makeText(this, "Preencha ao menos o nome e a tarifa!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double tarifa;
                    try {
                        tarifa = Double.parseDouble(tarifaStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Tarifa inválida!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isEdicao) {
                        rotaExistente.setNome(nome);
                        rotaExistente.setOrigem(origem);
                        rotaExistente.setDestino(destino);
                        rotaExistente.setTarifa(tarifa);
                        rotaExistente.setDescricao(descricao);
                        rotaDAO.atualizar(rotaExistente);
                        Toast.makeText(this, "Rota atualizada com sucesso!", Toast.LENGTH_SHORT).show();
                    } else {
                        Rota novaRota = new Rota(nome, origem, destino, tarifa, descricao);
                        rotaDAO.inserir(novaRota);
                        Toast.makeText(this, "Rota cadastrada com sucesso!", Toast.LENGTH_SHORT).show();
                    }

                    carregarRotas();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminacao(Rota rota) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar Rota")
                .setMessage("Deseja realmente eliminar a rota \"" + rota.getNome() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    rotaDAO.eliminar(rota.getId());
                    Toast.makeText(this, "Rota eliminada com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarRotas();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}