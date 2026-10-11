package com.transnacala.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transnacala.app.R;
import com.transnacala.app.model.Rota;

import java.util.List;

public class RotaAdapter extends RecyclerView.Adapter<RotaAdapter.RotaViewHolder> {

    private final List<Rota> listaRotas;
    private final OnRotaClickListener listener;

    public interface OnRotaClickListener {
        void onRotaClick(Rota rota);
    }

    public RotaAdapter(
            List<Rota> listaRotas,
            OnRotaClickListener listener) {

        this.listaRotas = listaRotas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RotaViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rota, parent, false);

        return new RotaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RotaViewHolder holder,
            int position) {

        Rota rota = listaRotas.get(position);

        holder.txtNomeRota.setText(rota.getNome());

        holder.txtPercurso.setText(
                rota.getOrigem() + " até " + rota.getDestino()
        );

        holder.txtTarifa.setText(
                String.format("%.0f MT", rota.getTarifa())
        );

        holder.itemView.setOnClickListener(v ->
                listener.onRotaClick(rota)
        );
    }

    @Override
    public int getItemCount() {
        return listaRotas.size();
    }

    public static class RotaViewHolder
            extends RecyclerView.ViewHolder {

        TextView txtNomeRota;
        TextView txtPercurso;
        TextView txtTarifa;

        public RotaViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNomeRota =
                    itemView.findViewById(R.id.txtNomeRota);

            txtPercurso =
                    itemView.findViewById(R.id.txtPercurso);

            txtTarifa =
                    itemView.findViewById(R.id.txtTarifa);
        }
    }
}