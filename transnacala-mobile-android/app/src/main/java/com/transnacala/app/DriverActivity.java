package com.transnacala.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.transnacala.app.network.ApiClient;
import com.transnacala.app.network.ApiConfig;
import com.transnacala.app.network.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DriverActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST = 41;
    private EditText usernameInput;
    private EditText passwordInput;
    private View loginForm;
    private View driverControls;
    private View shareRings;
    private View pulseButton;
    private TextView status;
    private TextView driverName;
    private TextView driverUsername;
    private TextView vehicleCode;
    private MaterialButton shareButton;
    private String vehicleId;
    private String sessionId;
    private boolean busy;
    private boolean pendingStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        usernameInput = findViewById(R.id.inputUsername);
        passwordInput = findViewById(R.id.inputPassword);
        loginForm = findViewById(R.id.driverLoginForm);
        driverControls = findViewById(R.id.driverControls);
        shareRings = findViewById(R.id.shareRings);
        pulseButton = findViewById(R.id.btnPartilharLocalizacao);
        status = findViewById(R.id.txtDriverStatus);
        driverName = findViewById(R.id.txtDriverName);
        driverUsername = findViewById(R.id.txtDriverUsername);
        vehicleCode = findViewById(R.id.txtVehicleCode);
        shareButton = findViewById(R.id.btnPartilharLocalizacao);

        findViewById(R.id.btnDriverLogin).setOnClickListener(v -> login());
        findViewById(R.id.btnDriverLogout).setOnClickListener(v -> logout());
        shareButton.setOnClickListener(v -> toggleSharing());
        setSignedOutState();
        showSignedInState();
    }

    private void login() {
        String username = usernameInput.getText() == null
                ? "" : usernameInput.getText().toString().trim();
        String password = passwordInput.getText() == null
                ? "" : passwordInput.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Informe utilizador e palavra-passe.", Toast.LENGTH_SHORT).show();
            return;
        }

        setBusy(true);
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("username", username);
                payload.put("password", password);
                JSONObject response = new ApiClient(ApiConfig.getBaseUrl(this), null)
                        .post("/api/v1/auth/login", payload);
                JSONObject user = response.getJSONObject("user");
                if (!"DRIVER".equals(user.optString("role"))) {
                    runOnUiThread(() -> {
                        setBusy(false);
                        Toast.makeText(this, "Esta conta não tem perfil de motorista.",
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                SessionManager.saveDriverSession(
                        this, response.getString("access_token"), user
                );
                runOnUiThread(() -> passwordInput.setText(""));
                loadAssignedVehicle(user.getString("id"));
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> {
                    setBusy(false);
                    Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void loadAssignedVehicle(String userId) {
        try {
            ApiClient client = new ApiClient(
                    ApiConfig.getBaseUrl(this), SessionManager.getToken(this)
            );
            int offset = 0;
            String foundVehicleId = null;
            String foundCode = null;
            while (true) {
                JSONObject page = client.get("/api/v1/vehicles?limit=100&offset=" + offset);
                JSONArray vehicles = page.getJSONArray("items");
                for (int i = 0; i < vehicles.length(); i++) {
                    JSONObject vehicle = vehicles.getJSONObject(i);
                    if (userId.equals(vehicle.optString("driver_user_id"))
                            && vehicle.optBoolean("is_active", false)) {
                        foundVehicleId = vehicle.getString("id");
                        foundCode = vehicle.getString("code");
                        break;
                    }
                }
                if (foundVehicleId != null || vehicles.length() < 100) {
                    break;
                }
                offset += vehicles.length();
            }

            String id = foundVehicleId;
            String code = foundCode;
            runOnUiThread(() -> {
                setBusy(false);
                vehicleId = id;
                driverName.setText(SessionManager.getFullName(this));
                driverUsername.setText("@" + SessionManager.getUsername(this));
                vehicleCode.setText(code == null ? "Veículo ainda não atribuído" : code);
                loginForm.setVisibility(View.GONE);
                driverControls.setVisibility(View.VISIBLE);
                if (vehicleId == null) {
                    status.setText("A sua conta ainda não tem um veículo ativo atribuído.");
                    shareButton.setEnabled(false);
                } else {
                    status.setText("Pronto para iniciar a partilha da localização.");
                    shareButton.setEnabled(true);
                    sessionId = SessionManager.getActiveSessionId(this);
                    if (!sessionId.isEmpty()) {
                        startLocationService(sessionId);
                        showSharingState(true);
                    } else {
                        showSharingState(false);
                    }
                }
            });
        } catch (IOException | JSONException exception) {
            SessionManager.clear(this);
            runOnUiThread(() -> {
                setBusy(false);
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private void toggleSharing() {
        if (busy) {
            return;
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            endSharing();
            return;
        }
        if (vehicleId == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            pendingStart = true;
            requestLocationPermissions();
            return;
        }
        startSharing();
    }

    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS},
                    LOCATION_PERMISSION_REQUEST
            );
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST || !pendingStart) {
            return;
        }
        pendingStart = false;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startSharing();
        } else {
            status.setText("A permissão de localização é necessária para partilhar o percurso.");
            Toast.makeText(this, status.getText(), Toast.LENGTH_LONG).show();
        }
    }

    private void startSharing() {
        setBusy(true);
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("vehicle_id", vehicleId);
                JSONObject response = driverClient().post("/api/v1/tracking/sessions", payload);
                String startedSessionId = response.getString("id");
                SessionManager.setActiveSessionId(this, startedSessionId);
                runOnUiThread(() -> {
                    sessionId = startedSessionId;
                    startLocationService(startedSessionId);
                    setBusy(false);
                    showSharingState(true);
                });
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> {
                    setBusy(false);
                    showApiError(exception);
                });
            }
        }).start();
    }

    private void endSharing() {
        setBusy(true);
        new Thread(() -> {
            try {
                driverClient().delete("/api/v1/tracking/sessions/" + sessionId);
                SessionManager.setActiveSessionId(this, "");
                runOnUiThread(() -> {
                    sessionId = "";
                    stopLocationService();
                    setBusy(false);
                    showSharingState(false);
                });
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    setBusy(false);
                    showApiError(exception);
                });
            }
        }).start();
    }

    private void startLocationService(String trackingSessionId) {
        Intent service = new Intent(this, LocationShareService.class);
        service.putExtra(LocationShareService.EXTRA_SESSION_ID, trackingSessionId);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
        } catch (SecurityException exception) {
            Toast.makeText(
                    this,
                    "Não foi possível iniciar o serviço de localização. Verifique as permissões.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopLocationService() {
        Intent service = new Intent(this, LocationShareService.class);
        service.setAction(LocationShareService.ACTION_STOP);
        startService(service);
    }

    private ApiClient driverClient() {
        return new ApiClient(ApiConfig.getBaseUrl(this), SessionManager.getToken(this));
    }

    private void showApiError(Exception exception) {
        if (exception instanceof ApiClient.ApiException
                && ((ApiClient.ApiException) exception).getStatusCode() == 401) {
            SessionManager.clear(this);
            vehicleId = null;
            setSignedOutState();
        }
        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void logout() {
        if (sessionId != null && !sessionId.isEmpty()) {
            Toast.makeText(this, "Termine a partilha antes de sair.", Toast.LENGTH_LONG).show();
            return;
        }
        SessionManager.clear(this);
        vehicleId = null;
        passwordInput.setText("");
        setSignedOutState();
    }

    private void showSignedInState() {
        if (SessionManager.isDriverSignedIn(this)) {
            loginForm.setVisibility(View.GONE);
            driverControls.setVisibility(View.VISIBLE);
            driverName.setText(SessionManager.getFullName(this));
            driverUsername.setText("@" + SessionManager.getUsername(this));
            sessionId = SessionManager.getActiveSessionId(this);
            loadAssignedVehicle(SessionManager.getUserId(this));
        }
    }

    private void setSignedOutState() {
        loginForm.setVisibility(View.VISIBLE);
        driverControls.setVisibility(View.GONE);
        shareButton.setContentDescription("Iniciar partilha da localização");
        shareButton.setEnabled(false);
        shareRings.setVisibility(View.GONE);
        pulseButton.setScaleX(1f);
        pulseButton.setScaleY(1f);
    }

    private void showSharingState(boolean sharing) {
        if (sharing) {
            shareButton.setContentDescription("Parar partilha da localização");
            status.setText("A sua localização está a ser partilhada.");
            shareRings.setVisibility(View.VISIBLE);
            startWaves();
        } else {
            pulseButton.animate().cancel();
            shareButton.setContentDescription("Iniciar partilha da localização");
            status.setText("Partilha terminada. A sua localização já não aparece no mapa.");
            shareRings.setVisibility(View.GONE);
            shareRings.findViewById(R.id.waveOne).animate().cancel();
            shareRings.findViewById(R.id.waveTwo).animate().cancel();
        }
    }

    private void startWaves() {
        pulseButton.animate().cancel();
        pulseButton.setScaleX(1f);
        pulseButton.setScaleY(1f);
        pulseButton.animate()
                .scaleX(1.07f)
                .scaleY(1.07f)
                .setDuration(850)
                .withEndAction(() -> pulseButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(850)
                        .withEndAction(this::startWaves)
                        .start())
                .start();
        animateWave(shareRings.findViewById(R.id.waveOne), 0);
        animateWave(shareRings.findViewById(R.id.waveTwo), 900);
    }

    private void animateWave(View wave, long delay) {
        wave.setScaleX(0.55f);
        wave.setScaleY(0.55f);
        wave.setAlpha(0.75f);
        wave.animate().cancel();
        wave.animate()
                .scaleX(1.65f)
                .scaleY(1.65f)
                .alpha(0f)
                .setStartDelay(delay)
                .setDuration(1800)
                .withEndAction(() -> {
                    if (shareRings.getVisibility() == View.VISIBLE) {
                        animateWave(wave, 0);
                    }
                })
                .start();
    }

    private void setBusy(boolean value) {
        busy = value;
        findViewById(R.id.btnDriverLogin).setEnabled(!value);
        shareButton.setEnabled(!value && vehicleId != null);
        findViewById(R.id.btnDriverLogout).setEnabled(!value);
        if (value) {
            status.setText("A processar pedido...");
        }
    }
}
