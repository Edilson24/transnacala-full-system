package org.transnacala.admin;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ApiClientTest {
    private HttpServer server;

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @After
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void reportsNonJsonResponseInsteadOfThrowingParserError() throws Exception {
        respond(502, "text/html", "<html>Bad Gateway</html>");

        try {
            client().login("admin", "password");
            fail("Expected the non-JSON response to be rejected.");
        } catch (ApiClient.ApiException error) {
            assertEquals(502, error.getStatusCode());
            assertEquals("INVALID_API_RESPONSE", error.getCode());
            assertTrue(error.getMessage().contains("não é um objeto JSON"));
        }
    }

    @Test
    public void preservesJsonApiErrorCodeAndMessage() throws Exception {
        respond(
                503,
                "application/json",
                "{\"error\":{\"code\":\"DATABASE_NOT_CONFIGURED\","
                        + "\"message\":\"DATABASE_URL is not configured.\"}}");

        try {
            client().login("admin", "password");
            fail("Expected the API error response to be rejected.");
        } catch (ApiClient.ApiException error) {
            assertEquals(503, error.getStatusCode());
            assertEquals("DATABASE_NOT_CONFIGURED", error.getCode());
            assertEquals("DATABASE_URL is not configured.", error.getMessage());
        }
    }

    @Test
    public void acceptsSuccessfulJsonObject() throws Exception {
        respond(
                200,
                "application/json",
                "{\"access_token\":\"test-token\",\"expires_in\":3600}");

        assertEquals(
                "test-token",
                client().login("admin", "password").get("access_token").getAsString());
    }

    @Test
    public void usesHttp11ForUvicornCompatibility() throws Exception {
        AtomicReference<String> protocol = new AtomicReference<>();
        server.createContext("/api/v1/auth/login", exchange -> {
            protocol.set(exchange.getProtocol());
            byte[] content = "{\"access_token\":\"test-token\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        });

        client().login("admin", "password");

        assertEquals("HTTP/1.1", protocol.get());
    }

    private ApiClient client() {
        return new ApiClient("http://127.0.0.1:" + server.getAddress().getPort(), null);
    }

    private void respond(int status, String contentType, String body) {
        server.createContext("/api/v1/auth/login", exchange -> {
            byte[] content = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        });
    }
}
