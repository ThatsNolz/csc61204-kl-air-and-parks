package cityair.view;

import cityair.model.AirCategory;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static cityair.view.Theme.*;

/** Data and impact screen: provenance, how to read the index, SDG 11 scope and privacy notes. */
final class AboutPane extends JPanel {
    private static final int TEXT_WIDTH = 300;

    AboutPane() {
        super(new GridLayout(1, 2, GAP, 0));
        setOpaque(false);
        add(legend());
        var notes = panel(new GridLayout(0, 1, 0, GAP));
        notes.add(note("Data source", "Open-Meteo Air Quality API using the Copernicus CAMS Global model. "
                + "Values are modeled estimates for representative coordinates, not park sensor readings "
                + "or Malaysia's official Air Pollutant Index."));
        notes.add(note("Reading the timestamps", "Model time is the hour the estimate represents. Fetch time is "
                + "when this app received it. Cached values keep both original times."));
        notes.add(note("SDG 11.6 and 11.7", "Helps residents interpret dated air-quality information (11.6) and "
                + "find a verified public green space (11.7). It does not measure reduced pollution or confirm "
                + "that a park is accessible."));
        notes.add(note("Privacy and shortcuts", "Reports and preferences stay on this device. Ctrl+R refreshes "
                + "the selected area; Alt+1 to Alt+6 open each screen."));
        add(notes);
    }

    private static JPanel legend() {
        var rows = panel(new GridLayout(0, 1, 0, 6));
        int lower = 0;
        for (AirCategory category : AirCategory.values()) {
            rows.add(legendRow(category, lower));
            lower = category.upperBound() + 1;
        }
        var note = label("<html><body style='width:" + TEXT_WIDTH + "px'>Colours follow the U.S. EPA AirNow scale. "
                + "Each category is also written in words, so colour is never the only cue. Indices above 500 "
                + "are still classified as Hazardous.</body></html>", SMALL, Font.PLAIN, MUTED);
        note.setVerticalAlignment(SwingConstants.TOP);
        var body = panel(new BorderLayout(0, PAD));
        body.add(rows, BorderLayout.NORTH);
        body.add(note, BorderLayout.CENTER);
        return titled("How to read the U.S. AQI", body);
    }

    private static JPanel legendRow(AirCategory category, int lower) {
        var row = panel(new BorderLayout(GAP, 0));
        String range = category == AirCategory.HAZARDOUS ? lower + "+" : lower + "–" + category.upperBound();
        var chip = label(range, SMALL, Font.BOLD, CategoryPalette.onFill(category));
        chip.setOpaque(true);
        chip.setBackground(CategoryPalette.fill(category));
        chip.setHorizontalAlignment(SwingConstants.CENTER);
        chip.setPreferredSize(new Dimension(76, 28));
        chip.setBorder(new EmptyBorder(2, 6, 2, 6));
        row.add(chip, BorderLayout.WEST);
        row.add(label(category.toString(), BODY, Font.PLAIN, TEXT), BorderLayout.CENTER);
        return row;
    }

    private static JPanel note(String title, String body) {
        var text = label("<html><body style='width:" + TEXT_WIDTH + "px'>" + body + "</body></html>", BODY, Font.PLAIN, TEXT);
        text.setVerticalAlignment(SwingConstants.TOP);
        return titled(title, text);
    }
}
