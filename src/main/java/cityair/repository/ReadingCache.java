package cityair.repository;

import cityair.model.District;
import cityair.model.Reading;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Persistent last-known reading by district. Only a validated real response is saved. */
public final class ReadingCache {
    private final Path folder;

    /** Uses the provided local cache directory. */
    public ReadingCache(Path folder) { this.folder = Objects.requireNonNull(folder); }

    /** Reads a validated cached record if present. */
    public Optional<Reading> load(District district) throws IOException {
        Path file = file(district);
        if (!Files.exists(file)) return Optional.empty();
        Reading reading = JsonFiles.JSON.readValue(file.toFile(), Reading.class);
        if (reading == null || reading.district() != district) throw new IOException("Invalid cache entry.");
        return Optional.of(reading);
    }

    /** Stores one validated response with an atomic file replacement when available. */
    public void save(Reading reading) throws IOException {
        JsonFiles.write(file(reading.district()), reading);
    }

    private Path file(District district) { return folder.resolve(district.name() + ".json"); }
}
