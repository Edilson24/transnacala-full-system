package com.transnacala.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.transnacala.app.model.Paragem;

import java.util.ArrayList;
import java.util.List;

public class ParagemDAO {

    private final DatabaseHelper databaseHelper;

    public ParagemDAO(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    // Inserir uma nova paragem
    public long inserir(Paragem paragem) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("nome", paragem.getNome());
        values.put("latitude", paragem.getLatitude());
        values.put("longitude", paragem.getLongitude());

        long id = db.insert("paragem", null, values);

        db.close();

        return id;
    }

    // Verificar se uma paragem já existe pelo nome
    public Paragem buscarPorNome(String nome) {

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM paragem WHERE nome = ? LIMIT 1",
                new String[]{nome}
        );

        Paragem paragem = null;

        if (cursor.moveToFirst()) {

            paragem = new Paragem();

            paragem.setId(
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                    )
            );

            paragem.setNome(
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("nome")
                    )
            );

            paragem.setLatitude(
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow("latitude")
                    )
            );

            paragem.setLongitude(
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow("longitude")
                    )
            );
        }

        cursor.close();
        db.close();

        return paragem;
    }

    // Listar todas as paragens
    public List<Paragem> listarTodas() {

        List<Paragem> paragens = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM paragem ORDER BY nome",
                null
        );

        while (cursor.moveToNext()) {

            Paragem paragem = new Paragem();

            paragem.setId(
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                    )
            );

            paragem.setNome(
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("nome")
                    )
            );

            paragem.setLatitude(
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow("latitude")
                    )
            );

            paragem.setLongitude(
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow("longitude")
                    )
            );

            paragens.add(paragem);
        }

        cursor.close();
        db.close();

        return paragens;
    }

    public int atualizar(Paragem paragem) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nome", paragem.getNome());
        values.put("latitude", paragem.getLatitude());
        values.put("longitude", paragem.getLongitude());

        int rowsAffected = db.update("paragem", values, "id = ?", new String[]{String.valueOf(paragem.getId())});
        db.close();
        return rowsAffected;
    }

    public int eliminar(int id) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int rowsDeleted = db.delete("paragem", "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rowsDeleted;
    }
}