package com.transnacala.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.transnacala.app.network.ApiClient;
import com.transnacala.app.network.ApiConfig;
import com.transnacala.app.network.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocationShareService extends Service implements LocationListener {
    public static final String ACTION_STOP = "com.transnacala.app.STOP_LOCATION_SHARE";
    public static final String EXTRA_SESSION_ID = "tracking_session_id";
    private static final String CHANNEL_ID = "location_sharing";
    private static final int NOTIFICATION_ID = 2401;
    private static final String TAG = "LocationShareService";

    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean uploadPending = new AtomicBoolean();
    private LocationManager locationManager;
    private String sessionId;
    private ApiClient apiClient;
    private boolean locationUpdatesRequested;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, notification("A preparar a localização..."));
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSharing();
            return START_NOT_STICKY;
        }
        if (intent == null) {
            stopSharing();
            return START_NOT_STICKY;
        }

        String requestedSessionId = intent.getStringExtra(EXTRA_SESSION_ID);
        String token = SessionManager.getToken(this);
        if (requestedSessionId == null || requestedSessionId.isEmpty() || token.isEmpty()) {
            Log.e(TAG, "Tracking session or authenticated token is unavailable.");
            stopSharing();
            return START_NOT_STICKY;
        }
        if (locationUpdatesRequested && requestedSessionId.equals(sessionId)) {
            return START_NOT_STICKY;
        }
        if (locationUpdatesRequested && locationManager != null) {
            locationManager.removeUpdates(this);
        }
        sessionId = requestedSessionId;
        apiClient = new ApiClient(ApiConfig.getBaseUrl(this), token);
        requestLocationUpdates();
        return START_NOT_STICKY;
    }

    private void requestLocationUpdates() {
        locationUpdatesRequested = false;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Fine location permission was revoked during an active session.");
            updateNotification("Permissão de localização necessária.");
            stopSharing();
            return;
        }
        try {
            boolean requested = false;
            for (String provider : new String[]{
                    LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER
            }) {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(provider, 5_000L, 10f, this);
                    requested = true;
                }
            }
            if (!requested) {
                updateNotification("Ative a localização do dispositivo.");
            } else {
                locationUpdatesRequested = true;
                updateNotification("Partilha de localização ativa.");
            }
        } catch (SecurityException exception) {
            Log.e(TAG, "Location permission denied while requesting updates.", exception);
            updateNotification("Permissão de localização necessária.");
            stopSharing();
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "No location provider is available.", exception);
            updateNotification("Localização indisponível neste dispositivo.");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (apiClient == null || sessionId == null || !uploadPending.compareAndSet(false, true)) {
            return;
        }
        uploadExecutor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("latitude", location.getLatitude());
                payload.put("longitude", location.getLongitude());
                if (location.hasAccuracy()) {
                    payload.put("accuracy_meters", location.getAccuracy());
                }
                if (location.hasBearing()) {
                    payload.put("heading_degrees", location.getBearing());
                }
                if (location.hasSpeed()) {
                    payload.put("speed_meters_per_second", location.getSpeed());
                }
                apiClient.put(
                        "/api/v1/tracking/sessions/" + sessionId + "/location",
                        payload
                );
                updateNotification("Partilha de localização ativa.");
            } catch (ApiClient.ApiException exception) {
                Log.e(TAG, "API rejected the location update (HTTP "
                        + exception.getStatusCode() + ", "
                        + exception.getErrorCode() + "): " + exception.getMessage());
                updateNotification(
                        "A API rejeitou a posição (HTTP " + exception.getStatusCode() + ")."
                );
            } catch (IOException | JSONException exception) {
                Log.w(TAG, "Location update could not be delivered; a later fix will retry.",
                        exception);
                updateNotification("Sem ligação à API. A tentar novamente...");
            } finally {
                uploadPending.set(false);
            }
        });
    }

    @Override
    public void onProviderEnabled(String provider) {
        updateNotification("Localização ativa. A aguardar posição...");
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (locationManager != null) {
            boolean anyEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!anyEnabled) {
                updateNotification("Ative a localização do dispositivo.");
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void stopSharing() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException exception) {
                Log.w(TAG, "Location permission was revoked before updates could stop.", exception);
            }
        }
        locationUpdatesRequested = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private Notification notification(String message) {
        Intent openApp = new Intent(this, DriverActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle("TransNacala")
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String message) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification(message));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Partilha de localização",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Indica quando a localização do motorista está ativa.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException exception) {
                Log.w(TAG, "Location permission was revoked during service shutdown.", exception);
            }
        }
        uploadExecutor.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
