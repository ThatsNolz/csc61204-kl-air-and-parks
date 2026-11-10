package cityair;

import cityair.api.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.*;
import static cityair.DomainTest.AT;
import static cityair.model.District.TITIWANGSA;
import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    static final String GOOD = """
            {"timezone":"Asia/Kuala_Lumpur","current":{"time":"2026-11-10T15:00",
            "us_aqi":79,"pm2_5":17.1,"pm10":null,"nitrogen_dioxide":28.2,"ozone":96.0}}
            """;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    @Test void fetch_validJson_mapsIndexTimeAndMissingPollutant() throws Exception {
        useServer(200, GOOD, 0, client -> {
            var reading = client.fetch(TITIWANGSA);
            assertAll(() -> assertEquals(79, reading.aqi()), () -> assertEquals(AT, reading.validAt()),
                    () -> assertEquals(17.1, reading.pm25()), () -> assertNull(reading.pm10()));
        });
    }

    @ParameterizedTest @CsvSource({"404,NOT_FOUND","500,SERVER","429,RATE_LIMIT","403,HTTP"})
    void fetch_httpError_preservesFailureKind(int status, ApiFailure.Kind expected) throws Exception {
        useServer(status, "{}", 0, client -> assertEquals(expected,
                assertThrows(ApiFailure.class, () -> client.fetch(TITIWANGSA)).kind()));
    }

    @ParameterizedTest @ValueSource(strings = {"", "{garbage", "{}", "null",
            "{\"timezone\":\"Asia/Kuala_Lumpur\",\"current\":{\"time\":\"2026-11-10T15:00\"}}",
            "{\"timezone\":\"Asia/Kuala_Lumpur\",\"current\":{\"time\":\"bad\",\"us_aqi\":7}}",
            "{\"timezone\":\"Asia/Kuala_Lumpur\",\"current\":{\"time\":\"2026-11-10T15:00\",\"us_aqi\":79,\"pm2_5\":\"bad\"}}"})
    void fetch_unusablePayload_returnsInvalid(String payload) throws Exception {
        useServer(200, payload, 0, client -> assertEquals(ApiFailure.Kind.INVALID,
                assertThrows(ApiFailure.class, () -> client.fetch(TITIWANGSA)).kind()));
    }

    @Test void fetch_slowEndpoint_returnsTimeout() throws Exception {
        useServer(200, GOOD, 1800, client -> assertEquals(ApiFailure.Kind.TIMEOUT,
                assertThrows(ApiFailure.class, () -> client.fetch(TITIWANGSA)).kind()));
    }

    @Test void fetch_closedEndpoint_returnsNetworkError() throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.stop(0);
        assertEquals(ApiFailure.Kind.NETWORK, assertThrows(ApiFailure.class, () -> client(port).fetch(TITIWANGSA)).kind());
    }

    private static OpenMeteoClient client(int port) {
        return new OpenMeteoClient(URI.create("http://127.0.0.1:" + port + "/v1/air-quality"),
                HTTP, Duration.ofMillis(950), Clock.fixed(AT, ZoneOffset.UTC));
    }

    static void useServer(int status, String body, long delay, Checked work) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/air-quality", exchange -> {
            try (exchange) {
                if (delay > 0) Thread.sleep(delay);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        });
        server.start();
        try { work.run(client(server.getAddress().getPort())); }
        finally { server.stop(0); }
    }

    interface Checked { void run(OpenMeteoClient client) throws Exception; }
}
