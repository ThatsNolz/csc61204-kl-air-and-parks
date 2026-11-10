package cityair.view;

import cityair.api.OpenMeteoClient;
import cityair.model.District;
import cityair.model.Reading;
import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import static cityair.view.DeskStyle.*;

/** Draws an AQI trend on elapsed model time; the adjacent table exposes exact values. */
final class TrendChart extends JPanel {
    private static final int LEFT = 50, RIGHT = 24, TOP = 20, BOTTOM = 33, POINT = 8;
    private static final double MINIMUM_SCALE = 100;
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH)
            .withZone(OpenMeteoClient.ZONE);
    private List<Reading> samples = List.of();

    TrendChart() {
        setBackground(SURFACE);
        setName("trendChart");
        getAccessibleContext().setAccessibleName("Session U.S. AQI chart; exact values in the adjacent table");
    }

    void setHistory(District district, List<Reading> history) {
        samples = history.stream().sorted(Comparator.comparing(Reading::validAt)).toList();
        setToolTipText(district + ": session model times, Malaysia time. Exact values are in the adjacent table.");
        repaint();
    }

    static String time(Instant time) { return TIMESTAMP.format(time); }

    /** Renders only actual supplied samples; no placeholder measurements are drawn. */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        var g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, BODY - 3));
            if (samples.isEmpty()) {
                g.setColor(MUTED);
                g.drawString("Refresh to collect a model sample.", GAP, TOP + BODY);
            } else if (getWidth() > LEFT + RIGHT && getHeight() > TOP + BOTTOM) {
                plot(g);
            }
        } finally {
            g.dispose();
        }
    }

    private void plot(Graphics2D g) {
        int width = getWidth() - LEFT - RIGHT;
        int height = getHeight() - TOP - BOTTOM;
        double max = Math.max(MINIMUM_SCALE, samples.stream().mapToInt(Reading::aqi).max().orElse(0));
        axes(g, width, height, max);
        var start = samples.get(0).validAt();
        var end = samples.get(samples.size() - 1).validAt();
        Point previous = null;
        for (Reading r : samples) {
            Point p = point(r, start, end, width, height, max);
            g.setColor(TEAL);
            if (previous != null) g.drawLine(previous.x, previous.y, p.x, p.y);
            g.fillOval(p.x - POINT / 2, p.y - POINT / 2, POINT, POINT);
            previous = p;
        }
        timeAxis(g, start, end, width, height);
    }

    private void axes(Graphics2D g, int w, int h, double max) {
        g.setColor(LINE);
        g.drawLine(LEFT, TOP, LEFT, TOP + h);
        g.drawLine(LEFT, TOP + h, LEFT + w, TOP + h);
        g.drawLine(LEFT, TOP, LEFT + w, TOP);
        g.setColor(MUTED);
        g.drawString("AQI", GAP / 2, TOP - GAP / 2);
        g.drawString(Integer.toString((int) max), GAP / 2, TOP + GAP / 2);
        g.drawString("0", GAP / 2, TOP + h);
    }

    private void timeAxis(Graphics2D g, Instant start, Instant end, int width, int height) {
        g.setColor(MUTED);
        g.drawString(time(start), LEFT, TOP + height + GAP);
        if (end.equals(start) || width < 170) return;
        var last = time(end);
        g.drawString(last, LEFT + width - g.getFontMetrics().stringWidth(last), TOP + height + GAP);
    }

    private static Point point(Reading r, Instant start, Instant end, int width, int height, double max) {
        long span = end.getEpochSecond() - start.getEpochSecond();
        double offset = span == 0 ? 0 : (r.validAt().getEpochSecond() - start.getEpochSecond()) / (double) span;
        return new Point(LEFT + (int) (width * offset), TOP + height - (int) (height * r.aqi() / max));
    }
}
