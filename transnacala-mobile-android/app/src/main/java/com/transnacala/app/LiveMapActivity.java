package com.transnacala.app;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.transnacala.app.network.ApiClient;
import com.transnacala.app.network.ApiConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveMapActivity extends AppCompatActivity {
    private static final long REFRESH_INTERVAL_MS = 15_000L;
    private static final String[] ROUTE_COLORS = {
            "#ed8a24", "#268b70", "#8263bb", "#397bb5", "#cc5b51", "#bc9a2c"
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Overlay> routeOverlays = new ArrayList<>();
    private final List<Marker> vehicleMarkers = new ArrayList<>();
    private MapView map;
    private TextView status;
    private TextView vehicleCount;
    private LinearLayout routeLegend;
    private boolean destroyed;
    private volatile boolean requestInProgress;
    private boolean focusedLiveVehicle;

    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            loadMapData();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );
        Configuration.getInstance().setUserAgentValue(
                getPackageName() + " Android"
        );
        setContentView(R.layout.activity_live_map);

        status = findViewById(R.id.txtLiveMapStatus);
        vehicleCount = findViewById(R.id.txtLiveMapVehicleCount);
        routeLegend = findViewById(R.id.liveMapRouteLegend);
        map = findViewById(R.id.liveMap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(13.5);
        map.getController().setCenter(new GeoPoint(-14.542, 40.672));
        findViewById(R.id.btnRefreshLiveMap).setOnClickListener(v -> loadMapData());
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        handler.removeCallbacks(refreshTask);
        refreshTask.run();
    }

    private void loadMapData() {
        if (requestInProgress || destroyed) {
            return;
        }
        requestInProgress = true;
        status.setText("A atualizar rotas e veículos...");
        executor.execute(() -> {
            try {
                ApiClient client = new ApiClient(ApiConfig.getBaseUrl(this), null);
                JSONObject routeResponse = client.get(
                        "/api/v1/routes?limit=100&offset=0&include_inactive=true"
                );
                JSONObject locationResponse = client.get(
                        "/api/v1/tracking/live?limit=100&offset=0"
                );
                List<LiveRoute> routes = parseRoutes(routeResponse.getJSONArray("items"));
                List<LiveVehicle> vehicles = parseVehicles(
                        locationResponse.getJSONArray("items")
                );
                runOnUiThread(() -> showMapData(routes, vehicles));
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> {
                    if (!destroyed) {
                        status.setText(
                                "Não foi possível atualizar. Verifique a ligação à API."
                        );
                    }
                });
            } finally {
                requestInProgress = false;
            }
        });
    }

    private List<LiveRoute> parseRoutes(JSONArray items) throws JSONException {
        List<LiveRoute> routes = new ArrayList<>(items.length());
        int otherRouteIndex = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String name = item.optString("name", "Rota sem nome");
            boolean baixaAlta = name.trim().matches(
                    "(?i)Baixa\\s*[–-]\\s*Alta"
            );
            int colorIndex = baixaAlta
                    ? 0
                    : 1 + (otherRouteIndex++ % (ROUTE_COLORS.length - 1));
            int color = Color.parseColor(ROUTE_COLORS[colorIndex]);
            JSONArray stops = item.optJSONArray("stops");
            List<GeoPoint> points = new ArrayList<>();
            if (stops != null) {
                for (int stopIndex = 0; stopIndex < stops.length(); stopIndex++) {
                    JSONObject stop = stops.getJSONObject(stopIndex);
                    double latitude = stop.optDouble("latitude", Double.NaN);
                    double longitude = stop.optDouble("longitude", Double.NaN);
                    if (Double.isFinite(latitude) && Double.isFinite(longitude)
                            && latitude >= -90 && latitude <= 90
                            && longitude >= -180 && longitude <= 180) {
                        points.add(new GeoPoint(latitude, longitude));
                    }
                }
            }
            routes.add(new LiveRoute(
                    item.optString("id", ""),
                    name,
                    color,
                    item.optBoolean("is_active", true),
                    points
            ));
        }
        return routes;
    }

    private List<LiveVehicle> parseVehicles(JSONArray items) throws JSONException {
        List<LiveVehicle> vehicles = new ArrayList<>(items.length());
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            double latitude = item.optDouble("latitude", Double.NaN);
            double longitude = item.optDouble("longitude", Double.NaN);
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
                continue;
            }
            vehicles.add(new LiveVehicle(
                    latitude,
                    longitude,
                    item.optString("vehicle_code", "Veículo"),
                    item.optString("route_id", ""),
                    item.optString("route_name", ""),
                    item.optString("updated_at", "")
            ));
        }
        return vehicles;
    }

    private void showMapData(List<LiveRoute> routes, List<LiveVehicle> vehicles) {
        if (destroyed) {
            return;
        }
        removeOldOverlays();
        routeLegend.removeAllViews();
        int drawableRoutes = 0;

        for (LiveRoute route : routes) {
            addRouteLegendItem(route);
            if (route.points.size() < 2) {
                continue;
            }
            Polyline line = new Polyline(map);
            line.setPoints(route.points);
            line.setColor(route.active ? route.color : withAlpha(route.color, 110));
            line.setWidth(6f);
            line.setTitle(route.name);
            map.getOverlays().add(line);
            routeOverlays.add(line);
            drawableRoutes++;
        }

        int renderedVehicles = 0;
        for (LiveVehicle vehicle : vehicles) {
            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(vehicle.latitude, vehicle.longitude));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setTitle(vehicle.code);
            marker.setSnippet((vehicle.route.isEmpty()
                    ? "Rota não atribuída" : vehicle.route)
                    + "\nAtualizado: " + vehicle.updatedAt);
            Drawable icon = getDrawable(R.drawable.ic_bus);
            if (icon != null) {
                icon = icon.mutate();
                icon.setTint(findRouteColor(routes, vehicle.routeId));
                marker.setIcon(icon);
            }
            map.getOverlays().add(marker);
            vehicleMarkers.add(marker);
            renderedVehicles++;
        }

        vehicleCount.setText(String.format(
                Locale.getDefault(), "%d veículo(s) ao vivo", renderedVehicles
        ));
        if (renderedVehicles > 0) {
            status.setText(drawableRoutes + " rota(s) desenhada(s) · posições atualizadas.");
            if (!focusedLiveVehicle) {
                map.getController().animateTo(new GeoPoint(
                        vehicles.get(0).latitude, vehicles.get(0).longitude
                ));
                focusedLiveVehicle = true;
            }
        } else if (drawableRoutes > 0) {
            status.setText(drawableRoutes
                    + " rota(s) no mapa · nenhum veículo está a partilhar a localização.");
        } else {
            status.setText(
                    "Sem rotas com coordenadas suficientes e sem veículos ao vivo."
            );
        }
        map.invalidate();
    }

    private void addRouteLegendItem(LiveRoute route) {
        TextView item = new TextView(this);
        item.setText("●  " + route.name);
        item.setTextColor(route.color);
        item.setTextSize(13);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(12, 8, 20, 8);
        if (!route.active) {
            item.setAlpha(0.55f);
        }
        routeLegend.addView(item);
    }

    private int findRouteColor(List<LiveRoute> routes, String routeId) {
        for (LiveRoute route : routes) {
            if (route.id.equals(routeId)) {
                return route.color;
            }
        }
        return Color.parseColor("#1d7453");
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void removeOldOverlays() {
        map.getOverlays().removeAll(routeOverlays);
        map.getOverlays().removeAll(vehicleMarkers);
        routeOverlays.clear();
        vehicleMarkers.clear();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshTask);
        map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        map.onDetach();
        super.onDestroy();
    }

    private static final class LiveRoute {
        final String id;
        final String name;
        final int color;
        final boolean active;
        final List<GeoPoint> points;

        LiveRoute(String id, String name, int color, boolean active, List<GeoPoint> points) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.active = active;
            this.points = points;
        }
    }

    private static final class LiveVehicle {
        final double latitude;
        final double longitude;
        final String code;
        final String routeId;
        final String route;
        final String updatedAt;

        LiveVehicle(double latitude, double longitude, String code, String routeId,
                    String route, String updatedAt) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.code = code;
            this.routeId = routeId;
            this.route = route;
            this.updatedAt = updatedAt;
        }
    }
}
