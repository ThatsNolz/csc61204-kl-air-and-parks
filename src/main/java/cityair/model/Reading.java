package cityair.model;

import java.time.Instant;
import java.util.Objects;

/**
 * One modeled observation for a pilot location. Null pollutant values are unavailable.
 * @param district pilot location
 * @param validAt provider's model validity time
 * @param fetchedAt time received by this client
 * @param aqi U.S. AQI
 * @param pm25 PM2.5 in micrograms per cubic metre
 * @param pm10 PM10 in micrograms per cubic metre
 * @param nitrogenDioxide NO2 in micrograms per cubic metre
 * @param ozone ozone in micrograms per cubic metre
 */
public record Reading(District district, Instant validAt, Instant fetchedAt, int aqi,
                      Double pm25, Double pm10, Double nitrogenDioxide, Double ozone) {
    /** Validates identifiers and optional pollutant measurements. */
    public Reading {
        Objects.requireNonNull(district);
        Objects.requireNonNull(validAt);
        Objects.requireNonNull(fetchedAt);
        AirCategory.from(aqi);
        validate(pm25);
        validate(pm10);
        validate(nitrogenDioxide);
        validate(ozone);
    }

    /** Returns the U.S. AQI category. */
    public AirCategory category() { return AirCategory.from(aqi); }

    private static void validate(Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0)) {
            throw new IllegalArgumentException("Pollutant concentration must be finite and non-negative.");
        }
    }
}
