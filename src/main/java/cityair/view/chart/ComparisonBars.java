package cityair.view.chart;

import cityair.model.District;
import cityair.view.component.AuroraComponent;
import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import javax.swing.JComponent;

/**
 * The pilot areas side by side, each as a bar in its own category colour.
 *
 * <p>Bars start at zero and share one axis, so their lengths can be compared directly. Areas that
 * have not been refreshed are drawn as an empty track labelled in words, which keeps the absence
 * of a measurement visible instead of leaving a gap that could be read as a low value.</p>
 */
public final class ComparisonBars extends AuroraComponent {
    private static final int ROW_HEIGHT = 46, BAR_HEIGHT = 10, LABEL_WIDTH = 150, VALUE_WIDTH = 54;
    private final transient IntFunction<Color> categoryColour;
    private final transient Map<District, Integer> values = new EnumMap<>(District.class);
    private final transient Map<District, Double> grown = new EnumMap<>(District.class);
    private transient District selected;
    private int ceiling = 100;

    /** Creates the comparison with the view's category palette. */
    public ComparisonBars(IntFunction<Color> categoryColour) {
        this.categoryColour = categoryColour;
        setOpaque(false);
        getAccessibleContext().setAccessibleName("Pilot area comparison");
    }

    /**
     * Replaces the comparison. A district absent from {@code indices} has no verified reading and
     * is drawn as an empty track.
     */
    public void setValues(Map<District, Integer> indices, District current) {
        values.clear();
        values.putAll(indices);
        selected = current;
        ceiling = Math.max(100, values.values().stream().mapToInt(Integer::intValue).max().orElse(0));
        describe();
        grow();
    }

    /** Returns the height every pilot area's row needs. */
    @Override public Dimension getPreferredSize() {
        return new Dimension(320, ROW_HEIGHT * District.values().length);
    }

    @Override protected javax.accessibility.AccessibleRole role() { return javax.accessibility.AccessibleRole.CANVAS; }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            List<District> areas = List.of(District.values());
            for (int i = 0; i < areas.size(); i++) {
                paintRow(g, areas.get(i), i * ROW_HEIGHT);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintRow(Graphics2D g, District area, int top) {
        boolean current = area == selected;
        g.setFont(current ? Type.strong(Type.LABEL) : Type.text(Type.LABEL));
        g.setColor(Skin.color(current ? Tone.INK : Tone.INK_MUTED));
        var metrics = g.getFontMetrics();
        g.drawString(area.toString(), 0, top + ROW_HEIGHT / 2f + metrics.getAscent() / 2f - 3);
        paintTrack(g, area, top);
        paintValue(g, area, top);
    }

    private void paintTrack(Graphics2D g, District area, int top) {
        int width = Math.max(10, getWidth() - LABEL_WIDTH - VALUE_WIDTH);
        double y = top + ROW_HEIGHT / 2.0 - BAR_HEIGHT / 2.0;
        g.setColor(Skin.color(Tone.SURFACE_SUNKEN));
        g.fill(Paints.round(LABEL_WIDTH, y, width, BAR_HEIGHT, Metrics.RADIUS_PILL));
        Integer value = values.get(area);
        if (value == null) return;
        double fraction = grown.getOrDefault(area, 0.0) / ceiling;
        g.setColor(categoryColour.apply(value));
        g.fill(Paints.round(LABEL_WIDTH, y, Math.max(BAR_HEIGHT, width * fraction), BAR_HEIGHT, Metrics.RADIUS_PILL));
    }

    private void paintValue(Graphics2D g, District area, int top) {
        Integer value = values.get(area);
        String text = value == null ? "no data" : Integer.toString(value);
        g.setFont(value == null ? Type.text(Type.MICRO) : Type.numberStrong(Type.LABEL));
        g.setColor(Skin.color(value == null ? Tone.INK_FAINT : Tone.INK));
        var metrics = g.getFontMetrics();
        g.drawString(text, getWidth() - metrics.stringWidth(text), top + ROW_HEIGHT / 2f + metrics.getAscent() / 2f - 3);
    }

    private void grow() {
        var targets = new EnumMap<District, Integer>(values);
        var from = new EnumMap<District, Double>(grown);
        Motion.play(Metrics.MOTION_DATA, progress -> {
            double eased = Motion.easeOut(progress);
            grown.clear();
            targets.forEach((area, value) -> grown.put(area, from.getOrDefault(area, 0.0)
                    + (value - from.getOrDefault(area, 0.0)) * eased));
            repaint();
        });
    }

    private void describe() {
        var text = new StringBuilder("Pilot area comparison. ");
        for (District area : District.values()) {
            Integer value = values.get(area);
            text.append(area).append(": ").append(value == null ? "no verified reading" : "U.S. AQI " + value).append(". ");
        }
        getAccessibleContext().setAccessibleDescription(text.toString().strip());
    }
}
