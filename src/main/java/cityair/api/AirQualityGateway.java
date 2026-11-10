package cityair.api;

import cityair.model.District;
import cityair.model.Reading;

/** Source of modeled readings, injectable for deterministic tests. */
@FunctionalInterface
public interface AirQualityGateway {
    /** Retrieves one reading or reports why it was unavailable. */
    Reading fetch(District district) throws ApiFailure;
}
