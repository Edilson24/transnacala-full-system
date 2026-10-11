package com.transnacala.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.app.R;
import com.transnacala.app.model.Paragem;

import java.util.List;

public class ParagensAdminAdapter extends RecyclerView.Adapter<ParagensAdminAdapter.ParagemViewHolder> {

    public interface OnParagemClickListener {
        void onEditar(Paragem paragem);
        void onEliminar(Paragem paragem);
    }

    private final List<Paragem> listaParagens;
    private final OnParagemClickListener listener;

    public ParagensAdminAdapter(List<Paragem> listaParagens, OnParagemClickListener listener) {
        this.listaParagens = listaParagens;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ParagemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paragem_admin, parent, false);
        return new ParagemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParagemViewHolder holder, int position) {
        Paragem paragem = listaParagens.get(position);
        holder.tvNome.setText(paragem.getNome());
        holder.tvCoordenadas.setText(String.format("Lat: %.5f | Long: %.5f", paragem.getLatitude(), paragem.getLongitude()));

        holder.btnEditar.setOnClickListener(v -> listener.onEditar(paragem));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminar(paragem));
    }

    @Override
    public int getItemCount() {
        return listaParagens != null ? listaParagens.size() : 0;
    }

    static class ParagemViewHolder extends RecyclerView.ViewHolder {
        TextView tvNome, tvCoordenadas;
        ImageButton btnEditar, btnEliminar;

        public ParagemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNome = itemView.findViewById(R.id.tvNomeParagem);
            tvCoordenadas = itemView.findViewById(R.id.tvCoordenadasParagem);
            btnEditar = itemView.findViewById(R.id.btnEditarParagem);
            btnEliminar = itemView.findViewById(R.id.btnEliminarParagem);
        }
    }
}