package com.transnacala.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "transnacala.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("PRAGMA foreign_keys = ON");

        String tabelaRota = "CREATE TABLE rota (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nome TEXT NOT NULL, " +
                "origem TEXT NOT NULL, " +
                "destino TEXT NOT NULL, " +
                "tarifa REAL NOT NULL, " +
                "descricao TEXT" +
                ")";

        String tabelaParagem = "CREATE TABLE paragem (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nome TEXT NOT NULL, " +
                "latitude REAL NOT NULL, " +
                "longitude REAL NOT NULL" +
                ")";

        String tabelaRotaParagem = "CREATE TABLE rota_paragem (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "rota_id INTEGER NOT NULL, " +
                "paragem_id INTEGER NOT NULL, " +
                "ordem INTEGER NOT NULL, " +
                "FOREIGN KEY(rota_id) REFERENCES rota(id), " +
                "FOREIGN KEY(paragem_id) REFERENCES paragem(id)" +
                ")";

        db.execSQL(tabelaRota);
        db.execSQL(tabelaParagem);
        db.execSQL(tabelaRotaParagem);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2) {

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS paragem (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "nome TEXT NOT NULL, " +
                            "latitude REAL NOT NULL, " +
                            "longitude REAL NOT NULL" +
                            ")"
            );

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS rota_paragem (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "rota_id INTEGER NOT NULL, " +
                            "paragem_id INTEGER NOT NULL, " +
                            "ordem INTEGER NOT NULL, " +
                            "FOREIGN KEY(rota_id) REFERENCES rota(id), " +
                            "FOREIGN KEY(paragem_id) REFERENCES paragem(id)" +
                            ")"
            );
        }
    }
}