package cityair.view;

import cityair.model.AirCategory;
import cityair.model.District;
import cityair.model.Reading;
import java.awt.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import static cityair.view.Theme.*;

/**
 * AQI trend on elapsed model time over tinted category bands, with the alert threshold drawn as
 * a dashed line. Only supplied samples are drawn; the adjacent table exposes the exact values.
 */
final class TrendChart extends JPanel {
    private static final int LEFT = 50, RIGHT = 24, TOP = 20, BOTTOM = 33, POINT = 10;
    private static final double MINIMUM_SCALE = 150, TINT = 0.82;
    private static final float[] DASH_PATTERN = {6f, 4f};
    private List<Reading> samples = List.of();
    private int threshold;

    TrendChart() {
        setBackground(SURFACE);
        setName("trendChart");
        getAccessibleContext().setAccessibleName("Session U.S. AQI chart; exact values in the adjacent table");
    }

    /** Replaces the plotted samples and the alert threshold line. */
    void setHistory(District district, List<Reading> history, int alertThreshold) {
        samples = history.stream().sorted(Comparator.comparing(Reading::validAt)).toList();
        threshold = alertThreshold;
        setToolTipText(district + ": session model times, Malaysia time. Exact values are in the adjacent table.");
        repaint();
    }

    /** Renders only actual supplied samples; no placeholder measurements are drawn. */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        var g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(font(SMALL - 1, Font.PLAIN));
            if (samples.isEmpty()) {
                g.setColor(MUTED);
                g.drawString("Refresh to collect a model sample.", GAP, TOP + BODY);
            } else if (getWidth() > LEFT + RIGHT && getHeight() > TOP + BOTTOM) {
                plot(g, getWidth() - LEFT - RIGHT, getHeight() - TOP - BOTTOM);
            }
        } finally {
            g.dispose();
        }
    }

    private void plot(Graphics2D g, int width, int height) {
        double max = Math.max(Math.max(MINIMUM_SCALE, threshold + GAP),
                samples.stream().mapToInt(Reading::aqi).max().orElse(0));
        bands(g, width, height, max);
        axes(g, width, height, max);
        thresholdLine(g, width, height, max);
        series(g, width, height, max);
        timeAxis(g, samples.get(0).validAt(), samples.get(samples.size() - 1).validAt(), width, height);
    }

    private void bands(Graphics2D g, int width, int height, double max) {
        int lower = 0;
        for (AirCategory category : AirCategory.values()) {
            if (lower > max) return;
            int upper = (int) Math.min(category.upperBound(), max);
            int top = y(upper, height, max), bottom = y(lower, height, max);
            g.setColor(tint(CategoryPalette.fill(category)));
            g.fillRect(LEFT, top, width, bottom - top);
            lower = upper + 1;
        }
    }

    private void axes(Graphics2D g, int w, int h, double max) {
        g.setColor(LINE);
        g.drawLine(LEFT, TOP, LEFT, TOP + h);
        g.drawLine(LEFT, TOP + h, LEFT + w, TOP + h);
        g.setColor(MUTED);
        g.drawString("AQI", GAP / 2, TOP - GAP / 2);
        g.drawString(Integer.toString((int) max), GAP / 2, TOP + GAP / 2);
        g.drawString("0", GAP / 2, TOP + h);
    }

    private void thresholdLine(Graphics2D g, int width, int height, double max) {
        if (threshold <= 0) return;
        int y = y(threshold, height, max);
        var saved = g.getStroke();
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DASH_PATTERN, 0f));
        g.setColor(ERROR_TEXT);
        g.drawLine(LEFT, y, LEFT + width, y);
        g.setStroke(saved);
        g.drawString("Alert " + threshold, LEFT + width - g.getFontMetrics().stringWidth("Alert " + threshold), y - 3);
    }

    private void series(Graphics2D g, int width, int height, double max) {
        var start = samples.get(0).validAt();
        var end = samples.get(samples.size() - 1).validAt();
        Point previous = null;
        g.setStroke(new BasicStroke(2f));
        for (Reading r : samples) {
            Point p = new Point(x(r.validAt(), start, end, width), y(r.aqi(), height, max));
            g.setColor(TEAL_DARK);
            if (previous != null) g.drawLine(previous.x, previous.y, p.x, p.y);
            g.setColor(CategoryPalette.fill(r.category()));
            g.fillOval(p.x - POINT / 2, p.y - POINT / 2, POINT, POINT);
            g.setColor(TEXT);
            g.drawOval(p.x - POINT / 2, p.y - POINT / 2, POINT, POINT);
            previous = p;
        }
    }

    private void timeAxis(Graphics2D g, Instant start, Instant end, int width, int height) {
        g.setColor(MUTED);
        var first = Formats.shortTime(start);
        int firstX = end.equals(start) ? LEFT + (width - g.getFontMetrics().stringWidth(first)) / 2 : LEFT;
        g.drawString(first, firstX, TOP + height + GAP + 2);
        if (end.equals(start) || width < 170) return;
        var last = Formats.shortTime(end);
        g.drawString(last, LEFT + width - g.getFontMetrics().stringWidth(last), TOP + height + GAP + 2);
    }

    private static int x(Instant time, Instant start, Instant end, int width) {
        long span = end.getEpochSecond() - start.getEpochSecond();
        double offset = span == 0 ? 0.5 : (time.getEpochSecond() - start.getEpochSecond()) / (double) span;
        return LEFT + (int) (width * offset);
    }

    private static int y(double aqi, int height, double max) {
        return TOP + height - (int) (height * Math.min(aqi, max) / max);
    }

    private static Color tint(Color colour) {
        return new Color(mix(colour.getRed()), mix(colour.getGreen()), mix(colour.getBlue()));
    }

    private static int mix(int channel) { return (int) Math.round(channel + (255 - channel) * TINT); }
}
