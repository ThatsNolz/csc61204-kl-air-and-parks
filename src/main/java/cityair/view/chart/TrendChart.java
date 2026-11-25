package cityair.view.chart;

import cityair.model.AirCategory;
import cityair.model.District;
import cityair.model.Reading;
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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * The index of the selected area over the model hours collected in this session.
 *
 * <p>Samples are joined with straight segments rather than a smoothed curve. A curve through
 * hourly points invents values between them, and this application never shows a number the
 * provider did not supply. The filled area beneath the line is decoration and carries no extra
 * meaning, so it fades out rather than implying a second quantity.</p>
 *
 * <p>Moving the pointer, or the arrow keys when the chart has focus, selects a sample and shows
 * its exact value, which keeps the chart usable without a mouse.</p>
 */
public final class TrendChart extends AuroraComponent {
    private static final int LEFT = 46, RIGHT = 18, TOP = 18, BOTTOM = 34;
    private static final int POINT = 7, HOVER_POINT = 10, MINIMUM_HEIGHT = 180;
    private static final double MINIMUM_SCALE = 150;
    private static final float[] DASH = {5f, 5f};
    private final transient IntFunction<Color> categoryColour;
    private final transient BiFunction<Reading, Boolean, String> describe;
    private transient List<Reading> samples = List.of();
    private transient District district;
    private int threshold;
    private int highlighted = -1;

    /**
     * Creates a chart.
     *
     * @param categoryColour colour for an index, supplied by the view's category palette
     * @param describe builds the tooltip text for a sample; the flag marks the keyboard selection
     */
    public TrendChart(IntFunction<Color> categoryColour, BiFunction<Reading, Boolean, String> describe) {
        this.categoryColour = categoryColour;
        this.describe = describe;
        setOpaque(false);
        setFocusable(true);
        setPreferredSize(new Dimension(360, MINIMUM_HEIGHT));
        getAccessibleContext().setAccessibleName("Session U.S. AQI chart");
        addMouseListener(pointer());
        addMouseMotionListener(pointer());
        bindKeys();
    }

    /** Replaces the plotted samples, the area they belong to and the alert threshold line. */
    public void setHistory(District area, List<Reading> history, int alertThreshold) {
        district = area;
        samples = history.stream().sorted(Comparator.comparing(Reading::validAt)).toList();
        threshold = alertThreshold;
        highlighted = samples.isEmpty() ? -1 : samples.size() - 1;
        describeChart();
        repaint();
    }

    /** Returns the index of the sample the reader has selected, or -1 when there is none. */
    public int highlighted() { return highlighted; }

    @Override protected javax.accessibility.AccessibleRole role() { return javax.accessibility.AccessibleRole.CANVAS; }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            int width = getWidth() - LEFT - RIGHT;
            int height = getHeight() - TOP - BOTTOM;
            if (width < 60 || height < 40) return;
            if (samples.isEmpty()) {
                paintNoSamples(g, width, height);
                return;
            }
            plot(g, width, height);
        } finally {
            g.dispose();
        }
    }

    private void paintNoSamples(Graphics2D g, int width, int height) {
        g.setFont(Type.text(Type.LABEL));
        g.setColor(Skin.color(Tone.INK_FAINT));
        String message = "No model hours collected yet. Refresh to plot this session.";
        var metrics = g.getFontMetrics();
        g.drawString(message, LEFT + (width - metrics.stringWidth(message)) / 2f, TOP + height / 2f);
    }

    private void plot(Graphics2D g, int width, int height) {
        double max = ceiling();
        paintBands(g, width, height, max);
        paintAxes(g, width, height, max);
        paintThreshold(g, width, height, max);
        paintSeries(g, width, height, max);
        paintTimeAxis(g, width, height);
        paintTooltip(g, width, height, max);
    }

    private double ceiling() {
        double highest = samples.stream().mapToInt(Reading::aqi).max().orElse(0);
        return Math.max(Math.max(MINIMUM_SCALE, threshold + 20), highest * 1.15);
    }

    private void paintBands(Graphics2D g, int width, int height, double max) {
        int lower = 0;
        for (AirCategory category : AirCategory.values()) {
            if (lower > max) return;
            int upper = (int) Math.min(category.upperBound(), max);
            int top = y(upper, height, max);
            int bottom = y(lower, height, max);
            g.setColor(Paints.alpha(categoryColour.apply(upper), Skin.theme().isDark() ? 20 : 26));
            g.fillRect(LEFT, top, width, bottom - top);
            lower = upper + 1;
        }
    }

    private void paintAxes(Graphics2D g, int width, int height, double max) {
        g.setFont(Type.number(Type.MICRO));
        var metrics = g.getFontMetrics();
        g.setStroke(new BasicStroke(Metrics.STROKE_HAIRLINE));
        for (int step = 0; step <= 2; step++) {
            int value = (int) Math.round(max / 2 * step);
            int lineY = y(value, height, max);
            g.setColor(Skin.color(Tone.GRID));
            g.draw(new Line2D.Double(LEFT, lineY, LEFT + width, lineY));
            g.setColor(Skin.color(Tone.INK_FAINT));
            String text = Integer.toString(value);
            g.drawString(text, LEFT - 8f - metrics.stringWidth(text), lineY + metrics.getAscent() / 2f - 1);
        }
        g.setFont(Type.caption());
        g.drawString("U.S. AQI", LEFT - 38f, TOP - 6f);
    }

    private void paintThreshold(Graphics2D g, int width, int height, double max) {
        if (threshold <= 0 || threshold > max) return;
        int lineY = y(threshold, height, max);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DASH, 0f));
        g.setColor(Skin.color(Tone.DANGER));
        g.draw(new Line2D.Double(LEFT, lineY, LEFT + width, lineY));
        g.setFont(Type.number(Type.MICRO));
        // The label sits at the start of the line: the newest sample, which matters most, is at
        // the right-hand end and must never be covered.
        g.drawString("alert " + threshold, LEFT + 6f, lineY - 5f);
    }

    private void paintSeries(Graphics2D g, int width, int height, double max) {
        var line = new Path2D.Double();
        for (int i = 0; i < samples.size(); i++) {
            double px = x(i, width);
            double py = y(samples.get(i).aqi(), height, max);
            if (i == 0) line.moveTo(px, py); else line.lineTo(px, py);
        }
        paintArea(g, line, width, height);
        g.setStroke(new BasicStroke(Metrics.STROKE_SERIES, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Skin.color(Tone.ACCENT));
        g.draw(line);
        paintPoints(g, width, height, max);
    }

    private void paintArea(Graphics2D g, Path2D.Double line, int width, int height) {
        var area = (Path2D.Double) line.clone();
        area.lineTo(x(samples.size() - 1, width), TOP + height);
        area.lineTo(x(0, width), TOP + height);
        area.closePath();
        var accent = Skin.color(Tone.ACCENT);
        g.setPaint(Paints.vertical(TOP, height, Paints.alpha(accent, 70), Paints.alpha(accent, 0)));
        g.fill(area);
    }

    private void paintPoints(Graphics2D g, int width, int height, double max) {
        for (int i = 0; i < samples.size(); i++) {
            Reading sample = samples.get(i);
            double px = x(i, width);
            double py = y(sample.aqi(), height, max);
            int size = i == highlighted ? HOVER_POINT : POINT;
            g.setColor(Skin.color(Tone.SURFACE));
            g.fill(new Ellipse2D.Double(px - size / 2.0, py - size / 2.0, size, size));
            g.setStroke(new BasicStroke(i == highlighted ? 3f : 2.2f));
            g.setColor(categoryColour.apply(sample.aqi()));
            g.draw(new Ellipse2D.Double(px - size / 2.0, py - size / 2.0, size, size));
        }
    }

    private void paintTimeAxis(Graphics2D g, int width, int height) {
        g.setFont(Type.number(Type.MICRO));
        g.setColor(Skin.color(Tone.INK_FAINT));
        var metrics = g.getFontMetrics();
        int baseline = TOP + height + 20;
        String first = describe.apply(samples.get(0), false);
        if (samples.size() == 1) {
            g.drawString(first, LEFT + (width - metrics.stringWidth(first)) / 2f, baseline);
            return;
        }
        g.drawString(first, LEFT, baseline);
        String last = describe.apply(samples.get(samples.size() - 1), false);
        g.drawString(last, LEFT + width - metrics.stringWidth(last), baseline);
    }

    private void paintTooltip(Graphics2D g, int width, int height, double max) {
        if (highlighted < 0 || highlighted >= samples.size()) return;
        Reading sample = samples.get(highlighted);
        String text = describe.apply(sample, true);
        g.setFont(Type.number(Type.MICRO));
        var metrics = g.getFontMetrics();
        double px = x(highlighted, width);
        double py = y(sample.aqi(), height, max);
        paintCrosshair(g, px, height);
        var box = tooltipBounds(metrics, text, px, py, width);
        g.setColor(Skin.color(Tone.TOOLTIP));
        g.fill(box);
        g.setColor(Skin.color(Tone.TOOLTIP_INK));
        g.drawString(text, (float) box.getX() + 9, (float) box.getY() + metrics.getAscent() + 5);
    }

    private java.awt.geom.RoundRectangle2D tooltipBounds(FontMetrics metrics, String text, double px, double py,
                                                         int width) {
        double boxWidth = metrics.stringWidth(text) + 18.0;
        double boxHeight = metrics.getHeight() + 10.0;
        double boxX = Math.min(Math.max(LEFT, px - boxWidth / 2), LEFT + width - boxWidth);
        double boxY = Math.max(TOP, py - boxHeight - 14);
        return Paints.round(boxX, boxY, boxWidth, boxHeight, Metrics.RADIUS_SMALL);
    }

    private void paintCrosshair(Graphics2D g, double px, int height) {
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DASH, 0f));
        g.setColor(Paints.alpha(Skin.color(Tone.INK_MUTED), 110));
        g.draw(new Line2D.Double(px, TOP, px, TOP + height));
    }

    private double x(int sample, int width) {
        if (samples.size() == 1) return LEFT + width / 2.0;
        return LEFT + width * sample / (double) (samples.size() - 1);
    }

    private int y(double aqi, int height, double max) {
        return (int) Math.round(TOP + height - height * Math.min(aqi, max) / max);
    }

    private MouseAdapter pointer() {
        return new MouseAdapter() {
            /** Tracks the pointer so the component under it can show its hovered state. */
            @Override public void mouseMoved(MouseEvent event) { highlight(nearest(event.getX())); }

            /** Clears the hovered state when the pointer leaves. */
            @Override public void mouseExited(MouseEvent event) { highlight(samples.size() - 1); }

            /** Takes focus, so the keyboard can continue what the pointer started. */
            @Override public void mousePressed(MouseEvent event) { requestFocusInWindow(); }
        };
    }

    private int nearest(int pointerX) {
        if (samples.isEmpty()) return -1;
        int width = getWidth() - LEFT - RIGHT;
        int best = 0;
        for (int i = 1; i < samples.size(); i++) {
            if (Math.abs(x(i, width) - pointerX) < Math.abs(x(best, width) - pointerX)) best = i;
        }
        return best;
    }

    private void highlight(int sample) {
        if (highlighted == sample) return;
        highlighted = sample;
        describeChart();
        repaint();
    }

    private void bindKeys() {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "earlier");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "later");
        getActionMap().put("earlier", step(-1));
        getActionMap().put("later", step(1));
    }

    private AbstractAction step(int delta) {
        return new AbstractAction() {
            /** Runs the action this control is bound to. */
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (samples.isEmpty()) return;
                highlight(Math.max(0, Math.min(samples.size() - 1, highlighted + delta)));
            }
        };
    }

    private void describeChart() {
        String area = district == null ? "the selected area" : district.toString();
        String selection = highlighted < 0 || highlighted >= samples.size()
                ? "no sample selected" : describe.apply(samples.get(highlighted), true);
        getAccessibleContext().setAccessibleDescription(samples.size() + " model hours for " + area
                + ". Selected: " + selection + ". Exact values are listed in the table beside the chart.");
        setToolTipText("Session model hours for " + area + ", Malaysia time.");
    }

    /** Index at the top of the drawn axis, exposed so a caller can label the scale. */
    public int axisCeiling() { return (int) Math.round(ceiling()); }
}
