package com.transnacala.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.transnacala.app.network.ApiClient;
import com.transnacala.app.network.ApiConfig;
import com.transnacala.app.network.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DriverActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private View loginForm;
    private View driverControls;
    private TextView status;
    private Button shareButton;
    private String vehicleId;
    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        usernameInput = findViewById(R.id.inputUsername);
        passwordInput = findViewById(R.id.inputPassword);
        loginForm = findViewById(R.id.driverLoginForm);
        driverControls = findViewById(R.id.driverControls);
        status = findViewById(R.id.txtDriverStatus);
        shareButton = findViewById(R.id.btnPartilharLocalizacao);

        findViewById(R.id.btnDriverLogin).setOnClickListener(v -> login());
        findViewById(R.id.btnDriverLogout).setOnClickListener(v -> logout());
        shareButton.setOnClickListener(v -> toggleSharing());
        showSignedInState();
    }

    private void login() {
        String baseUrl = ApiConfig.getBaseUrl(this);
        String username = usernameInput.getText() == null
                ? "" : usernameInput.getText().toString().trim();
        String password = passwordInput.getText() == null
                ? "" : passwordInput.getText().toString();
        if (baseUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Informe utilizador e palavra-passe.", Toast.LENGTH_SHORT).show();
            return;
        }

        setBusy(true);
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("username", username);
                payload.put("password", password);
                JSONObject response = new ApiClient(baseUrl, null)
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
                JSONObject page = client.get(
                        "/api/v1/vehicles?limit=100&offset=" + offset
                );
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
                loginForm.setVisibility(View.GONE);
                driverControls.setVisibility(View.VISIBLE);
                if (vehicleId == null) {
                    status.setText("Conta autenticada, mas ainda não há veículo ativo atribuído.");
                    shareButton.setEnabled(false);
                } else {
                    status.setText("Veículo atribuído: " + code);
                    shareButton.setEnabled(true);
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
        if (sessionId != null) {
            endSharing();
            return;
        }
        if (vehicleId == null) {
            return;
        }
        setBusy(true);
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("vehicle_id", vehicleId);
                JSONObject response = driverClient().post("/api/v1/tracking/sessions", payload);
                sessionId = response.getString("id");
                runOnUiThread(() -> {
                    setBusy(false);
                    shareButton.setText("Parar partilha");
                    status.setText("Partilha de localização ativa.");
                });
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> {
                    setBusy(false);
                    if (exception instanceof ApiClient.ApiException
                            && ((ApiClient.ApiException) exception).getStatusCode() == 501) {
                        status.setText("A API ainda não suporta localização ao vivo (Fase 4).");
                        Toast.makeText(this, status.getText(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void endSharing() {
        setBusy(true);
        new Thread(() -> {
            try {
                driverClient().delete("/api/v1/tracking/sessions/" + sessionId);
                sessionId = null;
                runOnUiThread(() -> {
                    setBusy(false);
                    shareButton.setText("Iniciar partilha");
                    status.setText("Partilha de localização terminada.");
                });
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    setBusy(false);
                    Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private ApiClient driverClient() {
        return new ApiClient(ApiConfig.getBaseUrl(this), SessionManager.getToken(this));
    }

    private void logout() {
        if (sessionId != null) {
            Toast.makeText(this, "Termine a partilha antes de sair.", Toast.LENGTH_LONG).show();
            return;
        }
        SessionManager.clear(this);
        vehicleId = null;
        passwordInput.setText("");
        driverControls.setVisibility(View.GONE);
        loginForm.setVisibility(View.VISIBLE);
    }

    private void showSignedInState() {
        if (SessionManager.isDriverSignedIn(this)) {
            loginForm.setVisibility(View.GONE);
            driverControls.setVisibility(View.VISIBLE);
            status.setText("Sessão autenticada. Consulte novamente a atribuição do veículo.");
            loadAssignedVehicle(SessionManager.getUserId(this));
        } else {
            driverControls.setVisibility(View.GONE);
            loginForm.setVisibility(View.VISIBLE);
        }
    }

    private void setBusy(boolean busy) {
        findViewById(R.id.btnDriverLogin).setEnabled(!busy);
        shareButton.setEnabled(!busy && vehicleId != null);
        findViewById(R.id.btnDriverLogout).setEnabled(!busy);
    }
}
