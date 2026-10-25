package com.transnacala.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.transnacala.app.database.DatabaseSeeder;
import com.transnacala.app.network.ApiConfig;
import com.transnacala.app.network.SyncScheduler;

public class MainActivity extends AppCompatActivity {

    private View btnRotas;
    private View btnAdmin;
    private View btnMotorista;
    private View btnConfigurarApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Garante a semeadura condicional de dados na primeira execução
        DatabaseSeeder.inserirDadosIniciais(this);

        btnRotas = findViewById(R.id.btnRotas);
        btnAdmin = findViewById(R.id.btnAdmin);
        btnMotorista = findViewById(R.id.btnMotorista);
        btnConfigurarApi = findViewById(R.id.btnConfigurarApi);
        btnConfigurarApi.setOnClickListener(v -> configurarApi());
        btnMotorista.setOnClickListener(v -> {
            if (ApiConfig.getBaseUrl(this).isEmpty()) {
                configurarApi();
                return;
            }
            startActivity(new Intent(MainActivity.this, DriverActivity.class));
        });
        SyncScheduler.syncNow(this);

        // Fluxo para passageiro/utilizador comum
        btnRotas.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RotasActivity.class);
            startActivity(intent);
        });

        // Fluxo para acesso do administrador
        btnAdmin.setOnClickListener(v -> Toast.makeText(
                this,
                "A administração de dados é feita pelo painel Web.",
                Toast.LENGTH_LONG
        ).show());
    }

    private void configurarApi() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://endereco-publico-da-api");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(ApiConfig.getBaseUrl(this));
        new MaterialAlertDialogBuilder(this)
                .setTitle("Conexão com a API")
                .setMessage("Informe o endereço HTTPS da API publicada pelo túnel.")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    try {
                        ApiConfig.setBaseUrl(this, input.getText().toString());
                        SyncScheduler.syncNow(this);
                        Toast.makeText(this, "Endereço guardado.", Toast.LENGTH_SHORT).show();
                    } catch (IllegalArgumentException exception) {
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

}