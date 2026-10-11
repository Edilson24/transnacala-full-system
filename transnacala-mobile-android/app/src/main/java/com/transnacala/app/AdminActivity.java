package com.transnacala.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        View btnGerirRotas = findViewById(R.id.btnGerirRotas);
        View btnGerirParagens = findViewById(R.id.btnGerirParagens);

        btnGerirRotas.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, GerenciarRotasActivity.class))
        );

        btnGerirParagens.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, GerenciarParagensActivity.class))
        );
    }
}