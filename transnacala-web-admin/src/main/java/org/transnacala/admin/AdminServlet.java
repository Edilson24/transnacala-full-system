package org.transnacala.admin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final Type MAP_LIST_TYPE = new TypeToken<List<Map<String, Object>>>() { }.getType();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        setSecurityHeaders(response);
        String path = request.getServletPath();
        if ("/login".equals(path)) {
            ensureCsrf(request.getSession());
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }
        if ("/logout".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!isAuthenticated(request.getSession(false))) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        HttpSession session = request.getSession();
        ensureCsrf(session);
        request.setAttribute("csrf", session.getAttribute("csrf"));
        request.setAttribute("username", session.getAttribute("username"));
        request.setAttribute("success", request.getParameter("success"));
        String page = adminPage(request);
        try {
            ApiClient api = new ApiClient((String) session.getAttribute("apiToken"));
            loadPageData(request, api, page);
            forwardPage(request, response, page);
        } catch (ApiClient.ApiException error) {
            if (error.getStatusCode() == 401) {
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?expired=1");
                return;
            }
            showPageError(request, response, page, error.getMessage());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            showPageError(request, response, page, "O pedido à API foi interrompido.");
        } catch (IOException error) {
            showPageError(request, response, page, "Não foi possível contactar a API TransNacala.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        setSecurityHeaders(response);
        HttpSession session = request.getSession(false);
        if (!validCsrf(session, request.getParameter("csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token.");
            return;
        }

        String path = request.getServletPath();
        if ("/login".equals(path)) {
            handleLogin(request, response);
            return;
        }
        if ("/logout".equals(path)) {
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!isAuthenticated(session)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String page = actionPage(request.getParameter("action"));
        try {
            applyAction(request, new ApiClient((String) session.getAttribute("apiToken")));
            String message = java.net.URLEncoder.encode("Alterações guardadas.", StandardCharsets.UTF_8.name());
            response.sendRedirect(request.getContextPath() + "/admin/" + page + "?success=" + message);
        } catch (ApiClient.ApiException error) {
            if (error.getStatusCode() == 401) {
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?expired=1");
                return;
            }
            showPageError(request, response, page, error.getMessage());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            showPageError(request, response, page, "O pedido à API foi interrompido.");
        } catch (IOException error) {
            showPageError(request, response, page, "Não foi possível contactar a API TransNacala.");
        } catch (IllegalArgumentException error) {
            showPageError(request, response, page, error.getMessage());
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            if (username == null || username.trim().isEmpty()
                    || password == null || password.isEmpty()) {
                request.setAttribute("error", "Introduza o utilizador e a palavra-passe.");
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }
            ApiClient api = new ApiClient(null);
            com.google.gson.JsonObject login = api.login(
                    username.trim(), password);
            Map<String, Object> user = GSON.fromJson(login.get("user"), MAP_TYPE);
            if (!"ADMIN".equals(user.get("role"))) {
                request.setAttribute("error", "Esta conta não tem acesso administrativo.");
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }
            request.changeSessionId();
            HttpSession session = request.getSession();
            session.setAttribute("apiToken", login.get("access_token").getAsString());
            session.setAttribute("username", user.get("username"));
            rotateCsrf(session);
            response.sendRedirect(request.getContextPath() + "/admin");
        } catch (ApiClient.ApiException error) {
            request.setAttribute("error", loginErrorMessage(error));
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            request.setAttribute("error", "O pedido de autenticação foi interrompido.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        } catch (IOException error) {
            request.setAttribute(
                    "error",
                    "Não foi possível contactar a API em "
                            + apiBaseUrl()
                            + ". Confirme que a API está iniciada e acessível.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }

    private static String loginErrorMessage(ApiClient.ApiException error) {
        switch (error.getCode()) {
            case "INVALID_CREDENTIALS":
                return "Utilizador ou palavra-passe inválidos.";
            case "DATABASE_NOT_CONFIGURED":
                return "A API está acessível, mas a base de dados não está configurada. "
                        + "Configure MYSQL_USER e MYSQL_PASSWORD (ou DATABASE_URL) no .env da API e reinicie-a.";
            case "DATABASE_UNAVAILABLE":
                return "A API não consegue ligar à base de dados. Verifique se o MySQL está iniciado "
                        + "e se as credenciais no .env da API estão corretas.";
            default:
                return error.getMessage();
        }
    }

    private static String apiBaseUrl() {
        return System.getenv().getOrDefault(
                "TRANSNACALA_API_URL", "http://127.0.0.1:8000");
    }

    private void applyAction(HttpServletRequest request, ApiClient api)
            throws IOException, InterruptedException {
        String action = request.getParameter("action");
        switch (action == null ? "" : action) {
            case "create-user":
                api.post("/api/v1/users", values(
                        "username", required(request, "username"),
                        "password", required(request, "password"),
                        "full_name", required(request, "full_name"),
                        "phone", blankToNull(request.getParameter("phone")),
                        "role", required(request, "role")));
                break;
            case "create-route":
                api.post("/api/v1/routes", values(
                        "name", required(request, "name"),
                        "origin", required(request, "origin"),
                        "destination", required(request, "destination"),
                        "fare", required(request, "fare"),
                        "description", blankToNull(request.getParameter("description"))));
                break;
            case "update-user": {
                Map<String, Object> userChanges = values(
                        "full_name", required(request, "full_name"),
                        "phone", blankToNull(request.getParameter("phone")),
                        "role", required(request, "role"));
                String password = blankToNull(request.getParameter("password"));
                if (password != null) {
                    userChanges.put("password", password);
                }
                api.patch("/api/v1/users/" + uuid(request, "entity_id"), userChanges);
                break;
            }
            case "update-route":
                api.patch("/api/v1/routes/" + uuid(request, "entity_id"), values(
                        "name", required(request, "name"),
                        "origin", required(request, "origin"),
                        "destination", required(request, "destination"),
                        "fare", required(request, "fare"),
                        "description", blankToNull(request.getParameter("description"))));
                break;
            case "create-stop":
                api.post("/api/v1/stops", values(
                        "name", required(request, "name"),
                        "latitude", required(request, "latitude"),
                        "longitude", required(request, "longitude")));
                break;
            case "create-stop-for-route":
                api.post("/api/v1/routes/" + uuid(request, "route_id") + "/stops", values(
                        "name", required(request, "name"),
                        "latitude", required(request, "latitude"),
                        "longitude", required(request, "longitude")));
                break;
            case "update-stop":
                api.patch("/api/v1/stops/" + uuid(request, "entity_id"), values(
                        "name", required(request, "name"),
                        "latitude", required(request, "latitude"),
                        "longitude", required(request, "longitude")));
                break;
            case "create-vehicle":
                api.post("/api/v1/vehicles", new LinkedHashMap<String, Object>());
                break;
            case "assign-vehicle":
                api.put("/api/v1/vehicles/" + uuid(request, "vehicle_id") + "/assignment",
                        values("driver_user_id", blankToNull(request.getParameter("driver_user_id")),
                                "route_id", blankToNull(request.getParameter("route_id"))));
                break;
            case "set-user-active":
                api.patch("/api/v1/users/" + uuid(request, "entity_id"),
                        values("is_active", Boolean.valueOf(required(request, "active"))));
                break;
            case "set-route-active":
                api.patch("/api/v1/routes/" + uuid(request, "entity_id"),
                        values("is_active", Boolean.valueOf(required(request, "active"))));
                break;
            case "set-stop-active":
                api.patch("/api/v1/stops/" + uuid(request, "entity_id"),
                        values("is_active", Boolean.valueOf(required(request, "active"))));
                break;
            case "set-vehicle-active":
                api.patch("/api/v1/vehicles/" + uuid(request, "entity_id"),
                        values("is_active", Boolean.valueOf(required(request, "active"))));
                break;
            case "save-route-stops":
                List<Map<String, Object>> orderedStops = new ArrayList<>();
                String rawIds = request.getParameter("stop_ids");
                if (rawIds == null) {
                    throw new IllegalArgumentException("Selecione as paragens da rota.");
                }
                int order = 1;
                for (String stopId : rawIds.split(",")) {
                    String normalized = stopId.trim();
                    if (!normalized.isEmpty()) {
                        orderedStops.add(values("stop_id", UUID.fromString(normalized).toString(),
                                "stop_order", order++));
                    }
                }
                api.put("/api/v1/routes/" + uuid(request, "route_id") + "/stops", orderedStops);
                break;
            default:
                throw new IllegalArgumentException("Ação administrativa desconhecida.");
        }
    }

    private void showPageError(
            HttpServletRequest request, HttpServletResponse response, String page, String message)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        request.setAttribute("csrf", session.getAttribute("csrf"));
        request.setAttribute("username", session.getAttribute("username"));
        request.setAttribute("error", message);
        try {
            loadPageData(request, new ApiClient((String) session.getAttribute("apiToken")), page);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            request.setAttribute("loadError", "A atualização dos dados foi interrompida.");
        } catch (IOException | ApiClient.ApiException error) {
            request.setAttribute("loadError", "Não foi possível atualizar os dados.");
        }
        forwardPage(request, response, page);
    }

    private void loadPageData(HttpServletRequest request, ApiClient api, String page)
            throws IOException, InterruptedException {
        if ("dashboard".equals(page)) {
            List<Map<String, Object>> routes =
                    items(api.get("/api/v1/routes?limit=100&offset=0&include_inactive=true"));
            request.setAttribute("users", items(api.get("/api/v1/users?limit=100&offset=0")));
            request.setAttribute("routes", routes);
            request.setAttribute("activeRouteCount", routes.stream()
                    .filter(route -> Boolean.TRUE.equals(route.get("is_active"))).count());
            request.setAttribute("stops", items(api.get("/api/v1/stops?limit=100&offset=0&include_inactive=true")));
            request.setAttribute("vehicles", items(api.get("/api/v1/vehicles?limit=100&offset=0&include_inactive=true")));
        } else if ("routes".equals(page)) {
            request.setAttribute("routes", items(api.get("/api/v1/routes?limit=100&offset=0&include_inactive=true")));
            request.setAttribute("stops", items(api.get("/api/v1/stops?limit=100&offset=0&include_inactive=true")));
        } else if ("team".equals(page)) {
            request.setAttribute("users", items(api.get("/api/v1/users?limit=100&offset=0")));
            request.setAttribute("routes", items(api.get("/api/v1/routes?limit=100&offset=0&include_inactive=true")));
            request.setAttribute("vehicles", items(api.get("/api/v1/vehicles?limit=100&offset=0&include_inactive=true")));
        } else {
            request.setAttribute("stops", items(api.get("/api/v1/stops?limit=100&offset=0&include_inactive=true")));
        }
    }

    private static void forwardPage(
            HttpServletRequest request, HttpServletResponse response, String page)
            throws ServletException, IOException {
        request.setAttribute("pageKey", page);
        switch (page) {
            case "routes":
                request.setAttribute("pageTitle", "Rotas");
                break;
            case "team":
                request.setAttribute("pageTitle", "Utilizadores e veículos");
                break;
            case "stops":
                request.setAttribute("pageTitle", "Paragens");
                break;
            default:
                request.setAttribute("pageTitle", "Visão geral");
        }
        request.getRequestDispatcher("/WEB-INF/views/" + page + ".jsp").forward(request, response);
    }

    private static String adminPage(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || "/".equals(path) || path.isEmpty()) {
            return "dashboard";
        }
        switch (path) {
            case "/routes":
                return "routes";
            case "/team":
                return "team";
            case "/stops":
                return "stops";
            default:
                return "dashboard";
        }
    }

    private static String actionPage(String action) {
        if (action == null) {
            return "dashboard";
        }
        if (action.contains("route") || action.contains("stop-for-route")) {
            return "routes";
        }
        if (action.contains("user") || action.contains("vehicle")) {
            return "team";
        }
        if (action.contains("stop")) {
            return "stops";
        }
        return "dashboard";
    }

    private static List<Map<String, Object>> items(com.google.gson.JsonObject page) {
        return GSON.fromJson(page.getAsJsonArray("items"), MAP_LIST_TYPE);
    }

    private static Map<String, Object> values(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put((String) pairs[i], pairs[i + 1]);
        }
        return result;
    }

    private static String required(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Preencha todos os campos obrigatórios.");
        }
        return value.trim();
    }

    private static String uuid(HttpServletRequest request, String name) {
        try {
            return UUID.fromString(required(request, name)).toString();
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Identificador inválido.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static boolean isAuthenticated(HttpSession session) {
        return session != null && session.getAttribute("apiToken") instanceof String;
    }

    private static void ensureCsrf(HttpSession session) {
        if (session.getAttribute("csrf") == null) {
            rotateCsrf(session);
        }
    }

    private static void rotateCsrf(HttpSession session) {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        session.setAttribute("csrf", java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value));
    }

    private static boolean validCsrf(HttpSession session, String submitted) {
        if (session == null || submitted == null || !(session.getAttribute("csrf") instanceof String)) {
            return false;
        }
        return MessageDigest.isEqual(
                ((String) session.getAttribute("csrf")).getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }

    private static void setSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'self'; style-src 'self' https://fonts.googleapis.com; "
                        + "font-src 'self' https://fonts.gstatic.com; script-src 'self'; "
                        + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; "
                        + "base-uri 'self'; form-action 'self'");
    }
}
