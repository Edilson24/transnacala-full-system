package com.transnacala.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.transnacala.app.model.Paragem;

import java.util.ArrayList;
import java.util.List;

public class RotaParagemDAO {

    private final DatabaseHelper databaseHelper;

    public RotaParagemDAO(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    // Associar uma paragem a uma rota
    public long inserir(int rotaId, int paragemId, int ordem) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("rota_id", rotaId);
        values.put("paragem_id", paragemId);
        values.put("ordem", ordem);

        long id = db.insert("rota_paragem", null, values);

        db.close();

        return id;
    }

    // Buscar todas as paragens de uma rota, respeitando a ordem do percurso
    public List<Paragem> listarParagensDaRota(int rotaId) {

        List<Paragem> paragens = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String sql =
                "SELECT p.id, p.nome, p.latitude, p.longitude " +
                        "FROM paragem p " +
                        "INNER JOIN rota_paragem rp " +
                        "ON p.id = rp.paragem_id " +
                        "WHERE rp.rota_id = ? " +
                        "ORDER BY rp.ordem ASC";

        Cursor cursor = db.rawQuery(
                sql,
                new String[]{String.valueOf(rotaId)}
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

    // Vincular uma paragem a uma rota com uma ordem específica
    public long associarParagem(int rotaId, int paragemId, int ordem) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("rota_id", rotaId);
        values.put("paragem_id", paragemId);
        values.put("ordem", ordem);

        long id = db.insert("rota_paragem", null, values);
        db.close();
        return id;
    }

    // Remover uma vinculação específica
    public int removerParagemDaRota(int rotaId, int paragemId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int rows = db.delete(
                "rota_paragem",
                "rota_id = ? AND paragem_id = ?",
                new String[]{String.valueOf(rotaId), String.valueOf(paragemId)}
        );
        db.close();
        return rows;
    }

    // Atualizar a ordem de uma paragem na rota
    public int atualizarOrdem(int rotaId, int paragemId, int novaOrdem) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("ordem", novaOrdem);

        int rows = db.update(
                "rota_paragem",
                values,
                "rota_id = ? AND paragem_id = ?",
                new String[]{String.valueOf(rotaId), String.valueOf(paragemId)}
        );
        db.close();
        return rows;
    }
}