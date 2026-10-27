package com.transnacala.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.transnacala.app.database.RotaParagemDAO;
import com.transnacala.app.model.Paragem;
import com.transnacala.app.model.Rota;

import java.util.List;
import java.util.Locale;

public class DetalhesRotaActivity extends AppCompatActivity {

    private TextView txtNomeRota;
    private TextView txtOrigem;
    private TextView txtDestino;
    private TextView txtTarifa;
    private TextView txtDescricao;

    private LinearLayout layoutParagens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detalhes_rota);

        txtNomeRota = findViewById(R.id.txtNomeRota);
        txtOrigem = findViewById(R.id.txtOrigem);
        txtDestino = findViewById(R.id.txtDestino);
        txtTarifa = findViewById(R.id.txtTarifa);
        txtDescricao = findViewById(R.id.txtDescricao);

        layoutParagens = findViewById(R.id.layoutParagens);

        Rota rota =
                (Rota) getIntent().getSerializableExtra("rota");

        if (rota != null) {

            txtNomeRota.setText(rota.getNome());

            txtOrigem.setText(rota.getOrigem());

            txtDestino.setText(rota.getDestino());

            txtTarifa.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.0f MT",
                            rota.getTarifa()
                    )
            );

            txtDescricao.setText(rota.getDescricao());

            carregarParagens(rota.getId());
        }
    }

    private void carregarParagens(int rotaId) {

        RotaParagemDAO rotaParagemDAO =
                new RotaParagemDAO(this);

        List<Paragem> paragens =
                rotaParagemDAO.listarParagensDaRota(rotaId);

        layoutParagens.removeAllViews();

        int numero = 1;
        for (Paragem paragem : paragens) {
            View stopRow = LayoutInflater.from(this)
                    .inflate(R.layout.item_route_stop, layoutParagens, false);
            TextView stopNumber = stopRow.findViewById(R.id.txtStopNumber);
            TextView stopName = stopRow.findViewById(R.id.txtStopName);
            View connector = stopRow.findViewById(R.id.viewStopConnector);
            stopNumber.setText(String.valueOf(numero));
            stopName.setText(paragem.getNome());
            connector.setVisibility(numero == paragens.size() ? View.INVISIBLE : View.VISIBLE);
            layoutParagens.addView(stopRow);
            numero++;
        }

        if (paragens.isEmpty()) {
            TextView emptyState = new TextView(this);
            emptyState.setText("Ainda não há paragens associadas a esta rota.");
            emptyState.setTextSize(14);
            emptyState.setTextColor(getColor(R.color.text_secondary));
            emptyState.setPadding(8, 8, 8, 8);
            layoutParagens.addView(emptyState);
        }
    }
}