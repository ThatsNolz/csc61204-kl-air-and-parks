package cityair.repository;

import cityair.model.CitizenReport;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Local append-and-replace storage for resident reports. One application process owns the store. */
public final class ReportStore {
    private final Path file;

    /** Uses reports.json under the supplied application data directory. */
    public ReportStore(Path folder) { file = Objects.requireNonNull(folder).resolve("reports.json"); }

    /** Lists stored reports with the newest first. */
    public synchronized List<CitizenReport> list() throws IOException {
        if (!Files.exists(file)) return List.of();
        List<CitizenReport> reports = JsonFiles.JSON.readValue(file.toFile(), new TypeReference<>() {});
        if (reports == null || reports.stream().anyMatch(Objects::isNull)) throw new IOException("Invalid report file.");
        return List.copyOf(reports);
    }

    /** Adds a report without dropping older entries if a read or write fails. */
    public synchronized void save(CitizenReport report) throws IOException {
        List<CitizenReport> reports = new ArrayList<>(list());
        reports.add(0, Objects.requireNonNull(report));
        JsonFiles.write(file, reports);
    }
}
