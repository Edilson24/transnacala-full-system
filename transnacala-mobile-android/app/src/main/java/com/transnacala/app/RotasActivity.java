package com.transnacala.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.app.adapter.RotaAdapter;
import com.transnacala.app.database.RotaDAO;
import com.transnacala.app.model.Rota;
import android.content.Intent;

import java.util.List;

public class RotasActivity extends AppCompatActivity {

    private RecyclerView recyclerRotas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rotas);

        recyclerRotas = findViewById(R.id.recyclerRotas);

        recyclerRotas.setLayoutManager(
                new LinearLayoutManager(this)
        );

        RotaDAO rotaDAO = new RotaDAO(this);

        List<Rota> rotas = rotaDAO.listarTodas();

        RotaAdapter adapter = new RotaAdapter(
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
    }
}