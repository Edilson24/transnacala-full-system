package com.transnacala.app.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.transnacala.app.database.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CatalogSyncWorker extends Worker {
    private static final int PAGE_SIZE = 100;
    private static final String CURSOR_KEY = "catalog_cursor";

    public CatalogSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String baseUrl = ApiConfig.getBaseUrl(getApplicationContext());
        if (baseUrl.isEmpty()) {
            return Result.failure();
        }

        ApiClient client = new ApiClient(baseUrl, null);
        DatabaseHelper helper = new DatabaseHelper(getApplicationContext());
        try {
            SQLiteDatabase database = helper.getWritableDatabase();
            long cursor = readCursor(database);
            while (!isStopped()) {
                JSONObject page = client.get(
                        "/api/v1/sync/changes?since_id=" + cursor + "&limit=" + PAGE_SIZE
                );
                JSONArray changes = page.getJSONArray("items");
                long nextCursor = page.getLong("next_since_id");
                if (page.getLong("since_id") != cursor || nextCursor < cursor) {
                    return Result.failure();
                }
                if (changes.length() == 0) {
                    return nextCursor == cursor ? Result.success() : Result.failure();
                }

                List<Change> prepared = prepareChanges(client, changes);
                if (prepared.get(prepared.size() - 1).id != nextCursor) {
                    return Result.failure();
                }
                long previousId = cursor;
                for (Change change : prepared) {
                    if (change.id <= previousId) {
                        return Result.failure();
                    }
                    previousId = change.id;
                }
                database.beginTransaction();
                try {
                    for (Change change : prepared) {
                        applyChange(database, change);
                    }
                    ContentValues state = new ContentValues();
                    state.put("valor", Long.toString(nextCursor));
                    database.insertWithOnConflict(
                            "sync_state", null, withKey(state, CURSOR_KEY),
                            SQLiteDatabase.CONFLICT_REPLACE
                    );
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }
                cursor = nextCursor;
                if (!page.getBoolean("has_more")) {
                    return Result.success();
                }
            }
            return Result.retry();
        } catch (ApiClient.ApiException exception) {
            Log.e("CatalogSyncWorker", "API rejected catalog sync: HTTP "
                    + exception.getStatusCode() + " " + exception.getErrorCode(), exception);
            return exception.getStatusCode() == 429 || exception.getStatusCode() >= 500
                    ? Result.retry() : Result.failure();
        } catch (IOException exception) {
            Log.w("CatalogSyncWorker", "Catalog sync will be retried.", exception);
            return Result.retry();
        } catch (JSONException | RuntimeException exception) {
            Log.e("CatalogSyncWorker", "Invalid catalog sync response or local data.", exception);
            return Result.failure();
        } finally {
            helper.close();
        }
    }

    private static List<Change> prepareChanges(ApiClient client, JSONArray changes)
            throws IOException, JSONException {
        List<Change> result = new ArrayList<>(changes.length());
        for (int i = 0; i < changes.length(); i++) {
            JSONObject item = changes.getJSONObject(i);
            Change change = new Change(
                    item.getLong("id"),
                    item.getString("entity_type"),
                    item.getString("entity_id"),
                    item.optString("related_entity_id", null),
                    item.getString("operation")
            );
            if ("UPSERT".equals(change.operation)) {
                String path;
                if ("ROUTE".equals(change.entityType) || "ROUTE_STOP".equals(change.entityType)) {
                    path = "/api/v1/routes/" + change.entityId;
                } else if ("STOP".equals(change.entityType)) {
                    path = "/api/v1/stops/" + change.entityId;
                } else if ("VEHICLE".equals(change.entityType)) {
                    result.add(change);
                    continue;
                } else {
                    throw new JSONException("Tipo de alteração desconhecido.");
                }
                try {
                    change.payload = client.get(path);
                } catch (ApiClient.ApiException exception) {
                    if (exception.getStatusCode() != 404) {
                        throw exception;
                    }
                    change.operation = "DELETE";
                }
            }
            result.add(change);
        }
        return result;
    }

    private static void applyChange(SQLiteDatabase db, Change change) throws JSONException {
        if ("ROUTE".equals(change.entityType)) {
            if ("DELETE".equals(change.operation)) {
                setActive(db, "rota", change.entityId, false);
            } else {
                upsertRoute(db, change.payload);
                replaceRouteStops(db, change.payload);
            }
        } else if ("STOP".equals(change.entityType)) {
            if ("DELETE".equals(change.operation)) {
                setActive(db, "paragem", change.entityId, false);
            } else {
                upsertStop(db, change.payload);
            }
        } else if ("ROUTE_STOP".equals(change.entityType)) {
            int routeId = localId(db, "rota", change.entityId);
            if (routeId < 0) {
                if ("UPSERT".equals(change.operation)) {
                    upsertRoute(db, change.payload);
                    replaceRouteStops(db, change.payload);
                }
                return;
            }
            if ("DELETE".equals(change.operation)) {
                int stopId = localId(db, "paragem", change.relatedEntityId);
                if (stopId >= 0) {
                    db.delete(
                            "rota_paragem", "rota_id = ? AND paragem_id = ?",
                            new String[]{Integer.toString(routeId), Integer.toString(stopId)}
                    );
                }
            } else {
                upsertRoute(db, change.payload);
                replaceRouteStops(db, change.payload);
            }
        }
    }

    private static void upsertRoute(SQLiteDatabase db, JSONObject route) throws JSONException {
        ContentValues values = new ContentValues();
        values.put("remote_id", route.getString("id"));
        String routeName = normalizeRouteName(route.getString("name"));
        values.put("nome", routeName);
        values.put("origem", route.getString("origin"));
        values.put("destino", route.getString("destination"));
        values.put("tarifa", route.getDouble("fare"));
        if (route.isNull("description")) {
            values.putNull("descricao");
        } else {
            values.put("descricao", route.getString("description"));
        }
        values.put("ativo", route.optBoolean("is_active", true) ? 1 : 0);
        String remoteId = route.getString("id");
        if (!updateExistingOrAdoptLegacy(
                db, "rota", remoteId, values,
                "REPLACE(REPLACE(REPLACE(nome, ' → ', ' – '), ' - ', ' – '), ' — ', ' – ') = ? " +
                        "AND origem = ? AND destino = ?",
                new String[]{routeName, route.getString("origin"),
                        route.getString("destination")}
        )) {
            db.insertOrThrow("rota", null, values);
        }
    }

    private static void upsertStop(SQLiteDatabase db, JSONObject stop) throws JSONException {
        ContentValues values = new ContentValues();
        values.put("remote_id", stop.getString("id"));
        values.put("nome", stop.getString("name"));
        values.put("latitude", stop.getDouble("latitude"));
        values.put("longitude", stop.getDouble("longitude"));
        values.put("ativo", stop.optBoolean("is_active", true) ? 1 : 0);
        String remoteId = stop.getString("id");
        if (!updateExistingOrAdoptLegacy(
                db, "paragem", remoteId, values,
                "nome = ? AND latitude = ? AND longitude = ?",
                new String[]{stop.getString("name"), stop.getString("latitude"),
                        stop.getString("longitude")}
        )) {
            db.insertOrThrow("paragem", null, values);
        }
    }

    private static void replaceRouteStops(SQLiteDatabase db, JSONObject route) throws JSONException {
        int routeId = localId(db, "rota", route.getString("id"));
        if (routeId < 0) {
            throw new JSONException("A rota sincronizada não foi persistida.");
        }
        JSONArray stops = route.optJSONArray("stops");
        if (stops == null) {
            throw new JSONException("A resposta da rota não inclui as paragens.");
        }

        db.delete("rota_paragem", "rota_id = ?", new String[]{Integer.toString(routeId)});
        for (int i = 0; i < stops.length(); i++) {
            JSONObject stop = stops.getJSONObject(i);
            upsertEmbeddedStop(db, stop);
            int stopId = localId(db, "paragem", stop.getString("stop_id"));
            if (stopId < 0) {
                throw new JSONException("Paragem da rota não foi persistida.");
            }
            ContentValues link = new ContentValues();
            link.put("rota_id", routeId);
            link.put("paragem_id", stopId);
            link.put("ordem", stop.getInt("stop_order"));
            if (db.insertWithOnConflict(
                    "rota_paragem", null, link, SQLiteDatabase.CONFLICT_IGNORE
            ) == -1) {
                throw new JSONException("Não foi possível atualizar a ordem das paragens.");
            }
        }
    }

    private static void upsertEmbeddedStop(SQLiteDatabase db, JSONObject stop)
            throws JSONException {
        ContentValues values = new ContentValues();
        values.put("remote_id", stop.getString("stop_id"));
        values.put("nome", stop.getString("name"));
        values.put("latitude", stop.getDouble("latitude"));
        values.put("longitude", stop.getDouble("longitude"));
        String remoteId = stop.getString("stop_id");
        if (!updateExistingOrAdoptLegacy(
                db, "paragem", remoteId, values,
                "nome = ? AND latitude = ? AND longitude = ?",
                new String[]{stop.getString("name"), stop.getString("latitude"),
                        stop.getString("longitude")}
        )) {
            db.insertOrThrow("paragem", null, values);
        }
    }

    private static boolean updateExistingOrAdoptLegacy(
            SQLiteDatabase db,
            String table,
            String remoteId,
            ContentValues values,
            String legacySelection,
            String[] legacyArguments
    ) {
        if (!"rota".equals(table) && !"paragem".equals(table)) {
            throw new IllegalArgumentException("Tabela de catálogo inválida.");
        }
        if (db.update(table, values, "remote_id = ?", new String[]{remoteId}) > 0) {
            return true;
        }
        int localId;
        try (Cursor cursor = db.rawQuery(
                "SELECT id FROM " + table + " WHERE remote_id IS NULL AND "
                        + legacySelection + " LIMIT 2",
                legacyArguments
        )) {
            if (cursor.getCount() != 1 || !cursor.moveToFirst()) {
                return false;
            }
            localId = cursor.getInt(0);
        }
        return db.update(table, values, "id = ?", new String[]{Integer.toString(localId)}) > 0;
    }

    private static int localId(SQLiteDatabase db, String table, String remoteId) {
        if (remoteId == null || (!"rota".equals(table) && !"paragem".equals(table))) {
            return -1;
        }
        try (Cursor cursor = db.rawQuery(
                "SELECT id FROM " + table + " WHERE remote_id = ?",
                new String[]{remoteId}
        )) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    private static String normalizeRouteName(String name) {
        return name.replace(" → ", " – ")
                .replace(" - ", " – ")
                .replace(" — ", " – ");
    }

    private static void setActive(
            SQLiteDatabase db, String table, String remoteId, boolean active
    ) {
        if (!"rota".equals(table) && !"paragem".equals(table)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("ativo", active ? 1 : 0);
        db.update(table, values, "remote_id = ?", new String[]{remoteId});
    }

    private static long readCursor(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery(
                "SELECT valor FROM sync_state WHERE chave = ?",
                new String[]{CURSOR_KEY}
        )) {
            return cursor.moveToFirst() ? Long.parseLong(cursor.getString(0)) : 0L;
        }
    }

    private static ContentValues withKey(ContentValues values, String key) {
        values.put("chave", key);
        return values;
    }

    private static final class Change {
        final long id;
        final String entityType;
        final String entityId;
        final String relatedEntityId;
        String operation;
        JSONObject payload;

        Change(long id, String entityType, String entityId, String relatedEntityId,
               String operation) {
            this.id = id;
            this.entityType = entityType;
            this.entityId = entityId;
            this.relatedEntityId = relatedEntityId;
            this.operation = operation;
        }
    }
}
