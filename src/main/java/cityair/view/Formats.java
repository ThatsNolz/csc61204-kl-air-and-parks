package cityair.view;

import cityair.api.OpenMeteoClient;
import cityair.service.ReadingService.Result;
import cityair.view.component.Formatting;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Text formats shared by every screen, so a time, a quantity or a source always reads the same. */
public final class Formats {
    /** Shown where the provider returned no value. */
    public static final String MISSING = Formatting.ABSENT;
    /** Shown in a cell that has nothing to display. */
    public static final String DASH = Formatting.DASH;
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm z", Locale.ENGLISH)
            .withZone(OpenMeteoClient.ZONE);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH)
            .withZone(OpenMeteoClient.ZONE);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
            .withZone(OpenMeteoClient.ZONE);

    private Formats() {}

    /** Date, time and zone, for example "25 Nov 2026 16:00 MYT". */
    public static String time(Instant value) { return FULL.format(value); }

    /** Compact date and time for tables and chart axes. */
    public static String shortTime(Instant value) { return SHORT.format(value); }

    /** Hour and minute only, for points inside one day. */
    public static String hour(Instant value) { return HOUR.format(value); }

    /** Concentration with one decimal place, or {@link #MISSING} when the provider omitted it. */
    public static String quantity(Double value) {
        return value == null ? MISSING : String.format(Locale.ENGLISH, "%.1f µg/m³", value);
    }

    /** Concentration without its unit, for a tile that prints the unit separately. */
    public static String amount(Double value) {
        return value == null ? MISSING : String.format(Locale.ENGLISH, "%.1f", value);
    }

    /** How long ago something happened, in plain words, for example "4 min ago". */
    public static String since(Instant moment, Instant now) {
        if (moment == null) return "never";
        long seconds = Math.max(0, Duration.between(moment, now).toSeconds());
        if (seconds < 60) return "just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        return hours < 24 ? hours + (hours == 1 ? " hour ago" : " hours ago") : shortTime(moment);
    }

    /** Long provenance label shown next to the headline index. */
    public static String source(Result result, boolean fixture) {
        if (result == null) return "NOT LOADED";
        return switch (result.state()) {
            case CACHED -> "CACHED (last verified response)";
            case LIVE -> fixture ? "TEST FIXTURE" : "LIVE API (modeled estimate)";
            case UNAVAILABLE -> "UNAVAILABLE";
        };
    }

    /** Short provenance label used in the comparison table. */
    public static String tableSource(Result result, boolean fixture) {
        return switch (result.state()) {
            case LIVE -> fixture ? "TEST FIXTURE" : "Live model";
            case CACHED -> "Verified cache";
            case UNAVAILABLE -> MISSING;
        };
    }

    /** One-word data state for the status pill: Live, Cached, Unavailable or Not loaded. */
    public static String state(Result result, boolean fixture) {
        if (result == null) return Formatting.NOT_LOADED;
        return switch (result.state()) {
            case LIVE -> fixture ? "Fixture" : "Live";
            case CACHED -> "Cached";
            case UNAVAILABLE -> "Unavailable";
        };
    }
}
