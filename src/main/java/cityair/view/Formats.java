package cityair.view;

import cityair.api.OpenMeteoClient;
import cityair.service.ReadingService.Result;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Text formats shared by all screens, so a time or source is always written the same way. */
final class Formats {
    static final String MISSING = "Unavailable";
    static final String DASH = "—";
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm z", Locale.ENGLISH)
            .withZone(OpenMeteoClient.ZONE);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH)
            .withZone(OpenMeteoClient.ZONE);

    private Formats() {}

    /** Date, time and zone, for example "25 Nov 2026 16:00 MYT". */
    static String time(Instant value) { return FULL.format(value); }

    /** Compact date and time for tables and chart axes. */
    static String shortTime(Instant value) { return SHORT.format(value); }

    /** Concentration with one decimal place, or {@link #MISSING} when the provider omitted it. */
    static String quantity(Double value) {
        return value == null ? MISSING : String.format(Locale.ENGLISH, "%.1f µg/m³", value);
    }

    /** Long provenance label shown next to the headline index. */
    static String source(Result result, boolean fixture) {
        if (result == null) return "NOT LOADED";
        return switch (result.state()) {
            case CACHED -> "CACHED (last verified response)";
            case LIVE -> fixture ? "TEST FIXTURE" : "LIVE API (modeled estimate)";
            case UNAVAILABLE -> "UNAVAILABLE";
        };
    }

    /** Short provenance label used in the comparison table. */
    static String tableSource(Result result, boolean fixture) {
        return switch (result.state()) {
            case LIVE -> fixture ? "TEST FIXTURE" : "Live model";
            case CACHED -> "Verified cache";
            case UNAVAILABLE -> MISSING;
        };
    }
}
