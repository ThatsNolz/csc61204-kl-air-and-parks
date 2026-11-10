package cityair.observer;

import cityair.model.District;
import cityair.model.Reading;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Evaluates new readings and notifies subscribers on the initial exceedance or a fresh upward crossing. */
public final class AlertMonitor {
    /** Start of the U.S. AQI sensitive-groups category. */
    public static final int DEFAULT_THRESHOLD = 101;
    private static final int HISTORY_LIMIT = 100;
    private static final Logger LOGGER = Logger.getLogger(AlertMonitor.class.getName());
    private final int threshold;
    private final Map<District, Reading> latest = new EnumMap<>(District.class);
    private final Map<District, Set<PollutionObserver>> subscriptions = new EnumMap<>(District.class);
    private final ArrayDeque<Alert> history = new ArrayDeque<>();

    /** Alert as displayed by the notification view. */
    public record Alert(District district, int aqi, int threshold, Instant validAt) {}

    /** Creates a monitor with a fixed positive U.S. AQI alert threshold. */
    public AlertMonitor(int threshold) {
        if (threshold <= 0) throw new IllegalArgumentException("Threshold must be positive.");
        this.threshold = threshold;
    }

    /** Registers an observer once for a district. */
    public synchronized void subscribe(District district, PollutionObserver observer) {
        Objects.requireNonNull(observer);
        subscriptions.computeIfAbsent(Objects.requireNonNull(district), key -> new LinkedHashSet<>()).add(observer);
    }

    /** Removes the observer from a district's subscription list. */
    public synchronized void unsubscribe(District district, PollutionObserver observer) {
        Set<PollutionObserver> listeners = subscriptions.get(district);
        if (listeners != null) listeners.remove(observer);
    }

    /** Returns a snapshot of alerts, newest first. */
    public synchronized List<Alert> history() { return List.copyOf(history); }

    /** Returns the fixed crossing threshold. */
    public int threshold() { return threshold; }

    /** Publishes a fresh model hour and calls subscriber code outside the monitor lock. */
    public void publish(Reading reading) {
        Alert alert;
        List<PollutionObserver> listeners;
        synchronized (this) {
            if (!crossed(Objects.requireNonNull(reading))) return;
            alert = new Alert(reading.district(), reading.aqi(), threshold, reading.validAt());
            if (history.size() == HISTORY_LIMIT) history.removeLast();
            history.addFirst(alert);
            listeners = new ArrayList<>(subscriptions.getOrDefault(reading.district(), Set.of()));
        }
        listeners.forEach(listener -> notifyListener(listener, alert));
    }

    private boolean crossed(Reading reading) {
        Reading previous = latest.get(reading.district());
        if (previous != null && !reading.validAt().isAfter(previous.validAt())) return false;
        latest.put(reading.district(), reading);
        return reading.aqi() >= threshold && (previous == null || previous.aqi() < threshold);
    }

    private static void notifyListener(PollutionObserver listener, Alert alert) {
        try {
            listener.onAlert(alert);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "An alert listener failed", ex);
        }
    }
}
