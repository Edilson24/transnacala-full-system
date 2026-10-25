package com.transnacala.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "transnacala.db";
    private static final int DATABASE_VERSION = 3;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("PRAGMA foreign_keys = ON");

        String tabelaRota = "CREATE TABLE rota (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "remote_id TEXT UNIQUE, " +
                "nome TEXT NOT NULL, " +
                "origem TEXT NOT NULL, " +
                "destino TEXT NOT NULL, " +
                "tarifa REAL NOT NULL, " +
                "descricao TEXT, " +
                "ativo INTEGER NOT NULL DEFAULT 1" +
                ")";

        String tabelaParagem = "CREATE TABLE paragem (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "remote_id TEXT UNIQUE, " +
                "nome TEXT NOT NULL, " +
                "latitude REAL NOT NULL, " +
                "longitude REAL NOT NULL, " +
                "ativo INTEGER NOT NULL DEFAULT 1" +
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
        db.execSQL("CREATE UNIQUE INDEX uq_rota_paragem ON rota_paragem(rota_id, paragem_id)");
        db.execSQL("CREATE TABLE sync_state (chave TEXT PRIMARY KEY, valor TEXT NOT NULL)");
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

        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE rota ADD COLUMN remote_id TEXT");
            db.execSQL("ALTER TABLE rota ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS uq_rota_remote_id ON rota(remote_id)");

            db.execSQL("ALTER TABLE paragem ADD COLUMN remote_id TEXT");
            db.execSQL("ALTER TABLE paragem ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS uq_paragem_remote_id ON paragem(remote_id)");

            db.execSQL(
                    "DELETE FROM rota_paragem WHERE id NOT IN " +
                            "(SELECT MIN(id) FROM rota_paragem GROUP BY rota_id, paragem_id)"
            );
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS uq_rota_paragem ON rota_paragem(rota_id, paragem_id)");
            db.execSQL("CREATE TABLE IF NOT EXISTS sync_state (chave TEXT PRIMARY KEY, valor TEXT NOT NULL)");
        }
    }
}