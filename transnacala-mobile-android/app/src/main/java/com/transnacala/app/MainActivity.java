package com.transnacala.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.transnacala.app.database.DatabaseSeeder;

public class MainActivity extends AppCompatActivity {

    private View btnRotas;
    private View btnAdmin;

    // Palavra-passe definida para acesso do administrador
    private static final String ADMIN_PIN = "1234";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Garante a semeadura condicional de dados na primeira execução
        DatabaseSeeder.inserirDadosIniciais(this);

        btnRotas = findViewById(R.id.btnRotas);
        btnAdmin = findViewById(R.id.btnAdmin);

        // Fluxo para passageiro/utilizador comum
        btnRotas.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RotasActivity.class);
            startActivity(intent);
        });

        // Fluxo para acesso do administrador
        btnAdmin.setOnClickListener(v -> solicitarAcessoAdmin());
    }

    private void solicitarAcessoAdmin() {
        FrameLayout container = new FrameLayout(this);
        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(paddingPx, paddingPx / 2, paddingPx, 0);

        TextInputLayout inputLayout = new TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        inputLayout.setHint("Palavra-passe do Administrador");

        TextInputEditText input = new TextInputEditText(inputLayout.getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        inputLayout.addView(input);
        container.addView(inputLayout);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Acesso Restrito")
                .setMessage("Digite a palavra-passe para aceder ao painel de administração:")
                .setView(container)
                .setPositiveButton("Entrar", (dialog, which) -> {
                    String pinDigitado = input.getText() != null ? input.getText().toString().trim() : "";
                    if (ADMIN_PIN.equals(pinDigitado)) {
                        Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Palavra-passe incorreta!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel())
                .show();
    }
}