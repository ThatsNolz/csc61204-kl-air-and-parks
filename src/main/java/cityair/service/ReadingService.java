package cityair.service;

import cityair.api.AirQualityGateway;
import cityair.api.ApiFailure;
import cityair.model.District;
import cityair.model.Reading;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coordinates verified readings, bounded session history, cached fallback and alert publication. */
public final class ReadingService {
    /** Distinguishes the acquisition path used by the view. */
    public enum State { LIVE, CACHED, UNAVAILABLE }
    /** Result of a refresh. A missing reading is allowed only for UNAVAILABLE. */
    public record Result(State state, Reading reading, String message) {
        /** Ensures the result state and presence of data agree. */
        public Result {
            Objects.requireNonNull(state);
            Objects.requireNonNull(message);
            if ((state == State.UNAVAILABLE) != (reading == null)) throw new IllegalArgumentException("Invalid result state.");
        }
    }

    private static final int HISTORY_LIMIT = 12;
    private static final Logger LOGGER = Logger.getLogger(ReadingService.class.getName());
    private final AirQualityGateway api;
    private final ReadingCache cache;
    private final AlertMonitor monitor;
    private final Map<District, Reading> memory = new EnumMap<>(District.class);
    private final Map<District, ArrayDeque<Reading>> history = new EnumMap<>(District.class);

    /** Creates the service and supplies its testable dependencies. */
    public ReadingService(AirQualityGateway api, ReadingCache cache, AlertMonitor monitor) {
        this.api = Objects.requireNonNull(api);
        this.cache = Objects.requireNonNull(cache);
        this.monitor = Objects.requireNonNull(monitor);
    }

    /** Requests a real reading or returns the last valid reading with an explicit fallback message. */
    public synchronized Result refresh(District district) {
        Objects.requireNonNull(district);
        try {
            Reading reading = api.fetch(district);
            if (reading.district() != district) throw new ApiFailure(ApiFailure.Kind.INVALID, "Wrong district in response.", null);
            Reading previous = cached(district);
            if (previous != null && reading.validAt().isBefore(previous.validAt())) {
                return new Result(State.CACHED, previous, "The provider returned an older model hour.");
            }
            remember(reading);
            monitor.publish(reading);
            return new Result(State.LIVE, reading, "Open-Meteo request succeeded.");
        } catch (ApiFailure ex) {
            LOGGER.log(Level.INFO, "Refresh failed for {0}: {1}", new Object[]{district, ex.kind()});
            return fallback(district, ex.getMessage());
        }
    }

    /** Provides recent distinct live model hours for the selected district, newest first. */
    public synchronized List<Reading> history(District district) {
        return List.copyOf(history.getOrDefault(district, new ArrayDeque<>()));
    }

    private void remember(Reading reading) {
        memory.put(reading.district(), reading);
        var recent = history.computeIfAbsent(reading.district(), key -> new ArrayDeque<>());
        if (!recent.isEmpty() && recent.peekFirst().validAt().equals(reading.validAt())) recent.removeFirst();
        if (recent.size() == HISTORY_LIMIT) recent.removeLast();
        recent.addFirst(reading);
        try {
            cache.save(reading);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Could not save local reading cache", ex);
        }
    }

    private Result fallback(District district, String message) {
        Reading previous = cached(district);
        if (previous == null) return new Result(State.UNAVAILABLE, null, message + " No verified data is available.");
        return new Result(State.CACHED, previous, message + " Showing the last verified response.");
    }

    private Reading cached(District district) {
        if (memory.containsKey(district)) return memory.get(district);
        try {
            return cache.load(district).orElse(null);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Could not read local reading cache", ex);
            return null;
        }
    }
}
