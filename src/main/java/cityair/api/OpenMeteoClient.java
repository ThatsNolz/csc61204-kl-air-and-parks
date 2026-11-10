package cityair.api;

import cityair.model.District;
import cityair.model.Reading;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import static cityair.api.ApiFailure.Kind.*;

/** HTTP and provider-specific JSON adapter for Open-Meteo. */
public final class OpenMeteoClient implements AirQualityGateway {
    /** Public noncommercial Open-Meteo endpoint. */
    public static final URI ENDPOINT = URI.create("https://air-quality-api.open-meteo.com/v1/air-quality");
    /** Local display and response interpretation timezone. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    private static final int OK = 200, REDIRECT = 300, NOT_FOUND_STATUS = 404, RATE_STATUS = 429, SERVER_STATUS = 500;
    private static final String FIELDS = "us_aqi,pm2_5,pm10,nitrogen_dioxide,ozone";
    private final URI endpoint;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final Duration timeout;
    private final Clock clock;

    /** Supplies the transport, endpoint, per-request timeout and acquisition clock. */
    public OpenMeteoClient(URI endpoint, HttpClient http, Duration timeout, Clock clock) {
        this.endpoint = Objects.requireNonNull(endpoint);
        this.http = Objects.requireNonNull(http);
        this.timeout = Objects.requireNonNull(timeout);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Performs a GET, maps its result, and translates transport failures. */
    @Override public Reading fetch(District district) throws ApiFailure {
        HttpRequest request = HttpRequest.newBuilder(requestUri(Objects.requireNonNull(district)))
                .timeout(timeout).header("Accept", "application/json").GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response.statusCode());
            return parse(district, response.body());
        } catch (HttpTimeoutException ex) {
            throw new ApiFailure(TIMEOUT, "The provider did not respond in time.", ex);
        } catch (IOException ex) {
            throw new ApiFailure(NETWORK, "Could not connect to the air-quality provider.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiFailure(NETWORK, "The air-quality request was interrupted.", ex);
        }
    }

    private URI requestUri(District district) {
        return URI.create(endpoint + "?latitude=" + district.latitude() + "&longitude=" + district.longitude()
                + "&current=" + FIELDS + "&timezone=Asia%2FKuala_Lumpur&domains=cams_global");
    }

    private static void checkStatus(int status) throws ApiFailure {
        if (status >= OK && status < REDIRECT) return;
        if (status == NOT_FOUND_STATUS) throw new ApiFailure(NOT_FOUND, "The provider endpoint was not found (404).", null);
        if (status == RATE_STATUS) throw new ApiFailure(RATE_LIMIT, "The provider is rate limiting requests (429). Try later.", null);
        if (status >= SERVER_STATUS) throw new ApiFailure(SERVER, "The provider is temporarily unavailable (" + status + ").", null);
        throw new ApiFailure(HTTP, "The provider rejected this request (" + status + ").", null);
    }

    private Reading parse(District district, String body) throws ApiFailure {
        try {
            JsonNode root = json.readTree(body);
            if (root == null || !root.isObject() || !ZONE.getId().equals(root.path("timezone").asText())) {
                throw new IllegalArgumentException("Invalid response timezone or root.");
            }
            return toReading(district, root.path("current"));
        } catch (IOException | IllegalArgumentException | java.time.DateTimeException ex) {
            throw new ApiFailure(INVALID, "The provider returned unusable or incomplete data.", ex);
        }
    }

    private Reading toReading(District district, JsonNode current) {
        JsonNode index = current.path("us_aqi");
        if (!index.isIntegralNumber() || !index.canConvertToInt()) {
            throw new IllegalArgumentException("Missing or invalid U.S. AQI.");
        }
        var time = LocalDateTime.parse(current.path("time").asText()).atZone(ZONE).toInstant();
        return new Reading(district, time, clock.instant(), index.intValue(), pollutant(current, "pm2_5"),
                pollutant(current, "pm10"), pollutant(current, "nitrogen_dioxide"), pollutant(current, "ozone"));
    }

    private static Double pollutant(JsonNode current, String field) {
        JsonNode value = current.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        if (!value.isNumber()) throw new IllegalArgumentException("Non-numeric pollutant: " + field);
        return value.doubleValue();
    }
}
