package cityair.view;

import cityair.model.AirCategory;
import cityair.model.GuidelineLevel;
import cityair.view.component.AuroraComponent;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Card;
import cityair.view.component.Panels;
import cityair.view.component.Text;
import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Data and impact: where the numbers come from, what they can and cannot say, how to read the
 * index, and how the pilot relates to SDG 11.
 *
 * <p>This screen carries the application's honesty. Each statement here constrains what the rest
 * of the interface is allowed to claim: modeled estimates rather than measurements, district
 * coordinates rather than park sensors, and a scale whose colours are adjusted for the dark theme
 * but never used as the only signal.</p>
 */
final class AboutPane extends JPanel implements Themed {
    private static final int LEGEND_WIDTH = 440, CHIP_WIDTH = 92, CHIP_HEIGHT = 26;

    AboutPane() {
        super(new BorderLayout(Metrics.SPACE_5, 0));
        setOpaque(false);
        add(legend(), BorderLayout.WEST);
        add(notes(), BorderLayout.CENTER);
    }

    private JComponent legend() {
        var rows = Panels.rows(Metrics.SPACE_2);
        int lower = 0;
        for (AirCategory category : AirCategory.values()) {
            rows.add(legendRow(category, lower));
            lower = category.upperBound() + 1;
        }
        var body = Panels.column(Metrics.SPACE_4);
        body.add(rows, BorderLayout.NORTH);
        body.add(Text.note("Aurora Light uses the published U.S. EPA AirNow colours. Aurora Dark keeps the same "
                + "six hues with adjusted luminance so text stays legible on a dark surface. Every category is "
                + "written in words wherever its colour appears, and an index above 500 is still Hazardous."),
                BorderLayout.CENTER);
        return Panels.fixedWidth(Card.titled("How to read the U.S. AQI", body), LEGEND_WIDTH);
    }

    private JComponent legendRow(AirCategory category, int lower) {
        var row = Panels.row(Metrics.SPACE_3);
        String range = category == AirCategory.HAZARDOUS ? lower + "+" : lower + "–" + category.upperBound();
        row.add(chip(range, category), BorderLayout.WEST);
        row.add(new AuroraLabel(category.toString(), Type.text(Type.BODY), Tone.INK), BorderLayout.CENTER);
        return row;
    }

    private static JComponent chip(String range, AirCategory category) {
        var chip = new CategoryChip(range, category);
        chip.getAccessibleContext().setAccessibleName(category + ", U.S. AQI " + range);
        return chip;
    }

    /** The colour and index range of one category, drawn as a single chip. */
    private static final class CategoryChip extends AuroraComponent {
        private final String range;
        private final transient AirCategory category;

        CategoryChip(String range, AirCategory category) {
            this.range = range;
            this.category = category;
        }

        /** Returns the size this component asks its layout for. */
        @Override public Dimension getPreferredSize() { return new Dimension(CHIP_WIDTH, CHIP_HEIGHT); }

        @Override protected void paintComponent(Graphics graphics) {
            var g = Paints.quality((Graphics2D) graphics.create());
            try {
                g.setColor(CategoryPalette.fill(category));
                g.fill(Paints.round(0, 0, CHIP_WIDTH, CHIP_HEIGHT, Metrics.RADIUS_SMALL));
                g.setFont(Type.numberStrong(Type.CAPTION));
                g.setColor(CategoryPalette.onFill(category));
                var metrics = g.getFontMetrics();
                g.drawString(range, (CHIP_WIDTH - metrics.stringWidth(range)) / 2f,
                        CHIP_HEIGHT / 2f + metrics.getAscent() / 2f - 1);
            } finally {
                g.dispose();
            }
        }
    }

    private JComponent notes() {
        var notes = Panels.rows(Metrics.SPACE_4);
        notes.add(note("Data source", "Open-Meteo Air Quality API using the Copernicus CAMS Global model. Values "
                + "are modeled estimates for representative district coordinates, not park sensor readings and "
                + "not Malaysia's official Air Pollutant Index. Nearby areas can share one model grid cell, "
                + "which is why two districts may return the same figure."));
        notes.add(note("Reading the timestamps", "Model time is the hour an estimate represents. Fetch time is "
                + "when this application received it. A cached value keeps both original times and is labelled "
                + "as cached wherever it appears."));
        notes.add(note("Guideline comparison", "Pollutant tiles are compared with the WHO 2021 guideline levels "
                + "(" + GuidelineLevel.SOURCE + "). " + GuidelineLevel.CAVEAT));
        notes.add(note("SDG 11.6 and 11.7", "The pilot helps residents interpret dated air-quality information "
                + "(11.6) and find a verified public green space (11.7). It does not measure reduced pollution "
                + "and does not confirm that a park is open or accessible."));
        notes.add(note("Privacy and shortcuts", "Reports and preferences stay on this device under "
                + ".kl-air-and-parks in your home folder. Ctrl+R refreshes the selected area, Ctrl+Shift+R "
                + "compares all areas, Ctrl+D switches theme, and Alt+1 to Alt+7 open each screen."));
        return Panels.top(notes);
    }

    private static JComponent note(String title, String body) {
        return Card.titled(title, Text.note(body));
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }
}
