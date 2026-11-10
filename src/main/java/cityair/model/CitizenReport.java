package cityair.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A resident's observation saved on this device. */
public record CitizenReport(UUID id, District district, Issue issue, String description, Instant createdAt) {
    /** Shortest accepted description after trimming. */
    public static final int MIN_DESCRIPTION = 10;
    /** Longest accepted description after trimming. */
    public static final int MAX_DESCRIPTION = 500;
    /** Shared display range, built from the actual validation bounds. */
    public static final String DESCRIPTION_RANGE = MIN_DESCRIPTION + " to " + MAX_DESCRIPTION;

    /** Report categories shown in the form. */
    public enum Issue { SMOKE, DUST, ODOUR, OTHER }

    /** Validates and normalizes a resident's input. */
    public CitizenReport {
        Objects.requireNonNull(id);
        Objects.requireNonNull(district);
        Objects.requireNonNull(issue);
        Objects.requireNonNull(createdAt);
        if (description == null) throw new IllegalArgumentException("Enter a description.");
        description = description.strip();
        if (description.length() < MIN_DESCRIPTION || description.length() > MAX_DESCRIPTION) {
            throw new IllegalArgumentException("Description must contain " + DESCRIPTION_RANGE + " characters.");
        }
    }
}
