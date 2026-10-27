package com.transnacala.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.transnacala.app.database.DatabaseSeeder;
import com.transnacala.app.network.SyncScheduler;

public class MainActivity extends AppCompatActivity {

    private View btnRotas;
    private View btnMotorista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Garante a semeadura condicional de dados na primeira execução
        DatabaseSeeder.inserirDadosIniciais(this);

        btnRotas = findViewById(R.id.btnRotas);
        btnMotorista = findViewById(R.id.btnMotorista);
        btnMotorista.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DriverActivity.class))
        );
        SyncScheduler.syncNow(this);

        // Fluxo para passageiro/utilizador comum
        btnRotas.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RotasActivity.class);
            startActivity(intent);
        });

    }
}