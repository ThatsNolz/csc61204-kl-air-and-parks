package cityair.view;

import cityair.model.AirCategory;
import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;
import static cityair.view.Theme.*;

/**
 * Horizontal AQI scale: six equal-width category segments in EPA colours with a marker at the
 * current index. Equal widths keep the low categories readable, as on the AirNow scale.
 */
final class AqiScale extends JPanel {
    /** Top of the scale; indices above it are pinned to the right edge of the last segment. */
    static final int SCALE_MAXIMUM = 500;
    private static final int BAR = 12, MARKER = 7, HEIGHT = 46;
    private static final AirCategory[] CATEGORIES = AirCategory.values();
    private Integer index;

    AqiScale() {
        setName("aqiScale");
        setOpaque(false);
        setPreferredSize(new Dimension(320, HEIGHT));
        getAccessibleContext().setAccessibleName("AQI category scale");
    }

    /** Moves the marker; {@code null} hides it when no verified reading is loaded. */
    void setIndex(Integer value) {
        index = value;
        getAccessibleContext().setAccessibleDescription(value == null ? "No reading" : "U.S. AQI " + value);
        repaint();
    }

    /** Horizontal fraction (0 to 1) of the scale at which an index is drawn. */
    static double position(int aqi) {
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
        var g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int top = MARKER + 2;
            paintSegments(g, top);
            paintTicks(g, top + BAR + SMALL + 2);
            if (index != null) paintMarker(g, (int) (position(index) * (getWidth() - 1)), top);
        } finally {
            g.dispose();
        }
    }

    private void paintSegments(Graphics2D g, int top) {
        double width = getWidth() / (double) CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length; i++) {
            g.setColor(CategoryPalette.fill(CATEGORIES[i]));
            g.fillRect((int) Math.round(i * width), top, (int) Math.ceil(width), BAR);
        }
    }

    private void paintTicks(Graphics2D g, int baseline) {
        g.setFont(font(SMALL - 1, Font.PLAIN));
        g.setColor(MUTED);
        double width = getWidth() / (double) CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length - 1; i++) {
            String tick = Integer.toString(CATEGORIES[i].upperBound());
            int x = (int) Math.round((i + 1) * width) - g.getFontMetrics().stringWidth(tick) / 2;
            g.drawString(tick, x, baseline);
        }
    }

    private void paintMarker(Graphics2D g, int x, int top) {
        var marker = new Path2D.Double();
        marker.moveTo(x - MARKER, top - MARKER - 1);
        marker.lineTo(x + MARKER, top - MARKER - 1);
        marker.lineTo(x, top);
        marker.closePath();
        g.setColor(TEXT);
        g.fill(marker);
        g.fillRect(x - 1, top, 2, BAR);
    }
}
