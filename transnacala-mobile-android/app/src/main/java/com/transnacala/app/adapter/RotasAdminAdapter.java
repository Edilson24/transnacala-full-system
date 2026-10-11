package com.transnacala.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.app.R;
import com.transnacala.app.model.Rota;

import java.util.List;

public class RotasAdminAdapter extends RecyclerView.Adapter<RotasAdminAdapter.RotaViewHolder> {

    public interface OnRotaClickListener {
        void onEditar(Rota rota);
        void onEliminar(Rota rota);
        void onGerirParagens(Rota rota);
    }

    private final List<Rota> listaRotas;
    private final OnRotaClickListener listener;

    public RotasAdminAdapter(List<Rota> listaRotas, OnRotaClickListener listener) {
        this.listaRotas = listaRotas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RotaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rota_admin, parent, false);
        return new RotaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RotaViewHolder holder, int position) {
        Rota rota = listaRotas.get(position);
        holder.tvNome.setText(rota.getNome());
        holder.tvTarifa.setText("Tarifa: " + rota.getTarifa() + " MT");

        holder.btnEditar.setOnClickListener(v -> listener.onEditar(rota));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminar(rota));
        holder.itemView.setOnClickListener(v -> listener.onGerirParagens(rota));
    }

    @Override
    public int getItemCount() {
        return listaRotas != null ? listaRotas.size() : 0;
    }

    static class RotaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNome, tvTarifa;
        ImageButton btnEditar, btnEliminar;

        public RotaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNome = itemView.findViewById(R.id.tvNomeRota);
            tvTarifa = itemView.findViewById(R.id.tvTarifaRota);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }


}