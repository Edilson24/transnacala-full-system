package com.transnacala.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.transnacala.app.model.Rota;

import java.util.ArrayList;
import java.util.List;

public class RotaDAO {

    private final DatabaseHelper databaseHelper;

    public RotaDAO(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }


    public long inserir(Rota rota) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("nome", rota.getNome());
        values.put("origem", rota.getOrigem());
        values.put("destino", rota.getDestino());
        values.put("tarifa", rota.getTarifa());
        values.put("descricao", rota.getDescricao());

        long id = db.insert("rota", null, values);

        db.close();

        return id;
    }

    public List<Rota> listarTodas() {

        List<Rota> rotas = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM rota WHERE ativo = 1 ORDER BY nome",
                null
        );

        while (cursor.moveToNext()) {

            Rota rota = new Rota();

            rota.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            rota.setRemoteId(cursor.getString(cursor.getColumnIndexOrThrow("remote_id")));
            rota.setAtivo(cursor.getInt(cursor.getColumnIndexOrThrow("ativo")) == 1);
            rota.setNome(cursor.getString(cursor.getColumnIndexOrThrow("nome")));
            rota.setOrigem(cursor.getString(cursor.getColumnIndexOrThrow("origem")));
            rota.setDestino(cursor.getString(cursor.getColumnIndexOrThrow("destino")));
            rota.setTarifa(cursor.getDouble(cursor.getColumnIndexOrThrow("tarifa")));
            rota.setDescricao(cursor.getString(cursor.getColumnIndexOrThrow("descricao")));

            rotas.add(rota);
        }

        cursor.close();
        db.close();

        return rotas;
    }

    public boolean temAlgumaRota() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM rota LIMIT 1", null)) {
            return cursor.moveToFirst();
        } finally {
            db.close();
        }
    }

    public Rota buscarPorId(int id) {

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM rota WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        Rota rota = null;

        if (cursor.moveToFirst()) {

            rota = new Rota();

            rota.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            rota.setRemoteId(cursor.getString(cursor.getColumnIndexOrThrow("remote_id")));
            rota.setAtivo(cursor.getInt(cursor.getColumnIndexOrThrow("ativo")) == 1);
            rota.setNome(cursor.getString(cursor.getColumnIndexOrThrow("nome")));
            rota.setOrigem(cursor.getString(cursor.getColumnIndexOrThrow("origem")));
            rota.setDestino(cursor.getString(cursor.getColumnIndexOrThrow("destino")));
            rota.setTarifa(cursor.getDouble(cursor.getColumnIndexOrThrow("tarifa")));
            rota.setDescricao(cursor.getString(cursor.getColumnIndexOrThrow("descricao")));
        }

        cursor.close();
        db.close();

        return rota;
    }

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    // Atualizar uma rota existente
    public int atualizar(Rota rota) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nome", rota.getNome());
        values.put("origem", rota.getOrigem());
        values.put("destino", rota.getDestino());
        values.put("tarifa", rota.getTarifa());
        values.put("descricao", rota.getDescricao());

        int rowsAffected = db.update("rota", values, "id = ?", new String[]{String.valueOf(rota.getId())});
        db.close();
        return rowsAffected;
    }

    // Eliminar uma rota por ID
    public int eliminar(int id) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int rowsDeleted = db.delete("rota", "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rowsDeleted;
    }
}