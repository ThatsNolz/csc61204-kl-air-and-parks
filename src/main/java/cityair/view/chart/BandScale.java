package cityair.view.chart;

import cityair.model.AirCategory;
import cityair.view.component.AuroraComponent;
import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.function.IntFunction;
import javax.swing.JComponent;

/**
 * The six U.S. AQI categories as a horizontal scale with a marker at the current index.
 *
 * <p>Each category is drawn the same width. On a linear axis the two categories most readings fall
 * into would occupy a fifth of the scale and the three severe ones four fifths, which hides exactly
 * the part a reader needs. Published scales, including the U.S. AirNow scale this application
 * follows, make the same choice. The boundary values are printed under the scale so the mapping is
 * visible rather than implied, and the exact index is always shown as a figure elsewhere.</p>
 */
public final class BandScale extends AuroraComponent {
    /** Top of the published U.S. AQI scale. */
    public static final int SCALE_MAXIMUM = 500;
    private static final AirCategory[] CATEGORIES = AirCategory.values();
    private static final int BAR = 10, MARKER = 7, HEIGHT = 44, LABEL_GAP = 14;
    private final transient IntFunction<Color> categoryColour;
    private Integer index;

    /** Creates a scale that asks the supplied function for the colour of an index. */
    public BandScale(IntFunction<Color> categoryColour) {
        this.categoryColour = categoryColour;
        setOpaque(false);
        setPreferredSize(new Dimension(300, HEIGHT));
        getAccessibleContext().setAccessibleName("U.S. AQI category scale");
        setIndex(null);
    }

    /** Moves the marker; {@code null} hides it when no verified reading is loaded. */
    public void setIndex(Integer value) {
        index = value;
        getAccessibleContext().setAccessibleDescription(value == null
                ? "No reading" : "U.S. AQI " + value + ", " + AirCategory.from(value));
        repaint();
    }

    /**
     * Horizontal position of an index on the scale, from 0 at the left edge to 1 at the right.
     * Each category occupies an equal share, and the index is placed proportionally within its own
     * category.
     */
    public static double position(int aqi) {
        int lower = 0;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int upper = Math.min(CATEGORIES[i].upperBound(), SCALE_MAXIMUM);
            if (aqi <= upper) {
                double within = (Math.max(aqi, lower) - lower) / (double) Math.max(1, upper - lower);
                return (i + within) / CATEGORIES.length;
            }
            lower = upper + 1;
        }
        return 1.0;
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            int top = MARKER + 3;
            paintBands(g, top);
            paintTicks(g, top + BAR + LABEL_GAP);
            if (index != null) paintMarker(g, position(index) * (getWidth() - 1), top);
        } finally {
            g.dispose();
        }
    }

    private void paintBands(Graphics2D g, int top) {
        double width = getWidth() / (double) CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean active = index != null && AirCategory.from(index) == CATEGORIES[i];
            Color colour = categoryColour.apply(Math.min(CATEGORIES[i].upperBound(), SCALE_MAXIMUM));
            g.setColor(active ? colour : Paints.alpha(colour, Skin.theme().isDark() ? 90 : 110));
            double inset = i == 0 || i == CATEGORIES.length - 1 ? 0 : 1;
            g.fill(Paints.round(i * width + inset, active ? top - 2 : top, width - inset * 2,
                    active ? BAR + 4 : BAR, Metrics.RADIUS_SMALL - 3));
        }
    }

    private void paintTicks(Graphics2D g, int baseline) {
        var font = Type.number(Type.MICRO);
        g.setFont(font);
        g.setColor(Skin.color(Tone.INK_FAINT));
        var metrics = g.getFontMetrics(font);
        double width = getWidth() / (double) CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length - 1; i++) {
            String tick = Integer.toString(CATEGORIES[i].upperBound());
            g.drawString(tick, (float) ((i + 1) * width - metrics.stringWidth(tick) / 2.0), baseline);
        }
    }

    private void paintMarker(Graphics2D g, double x, int top) {
        var marker = new Path2D.Double();
        marker.moveTo(x - MARKER, top - MARKER - 1);
        marker.lineTo(x + MARKER, top - MARKER - 1);
        marker.lineTo(x, top + 1);
        marker.closePath();
        g.setColor(Skin.color(Tone.INK));
        g.fill(marker);
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new java.awt.geom.Line2D.Double(x, top, x, top + BAR));
    }
}
