package cityair.view.chart;

import cityair.model.AirCategory;
import cityair.view.component.AuroraComponent;
import cityair.view.design.Metrics;
import cityair.view.design.Motion;
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
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.function.IntFunction;
import javax.swing.JComponent;

/**
 * The air-quality dial: the headline index drawn as a sweep over the six U.S. AQI categories.
 *
 * <p>The track behind the sweep carries the six official categories, each given an equal share of
 * the arc for the reason set out in {@link BandScale}. A separate tick marks the user's alert
 * threshold, which puts the number they chose and the number the model produced on one dial.</p>
 *
 * <p>The sweep animates to a new value so a refresh can be seen, and the figure in the centre is a
 * real label rather than painted text, so it is selectable, readable by assistive technology and
 * verifiable by the integration tests.</p>
 */
public final class AqiDial extends AuroraComponent {
    /** Top of the drawn scale; the published U.S. AQI scale ends at 500. */
    public static final int SCALE_MAXIMUM = BandScale.SCALE_MAXIMUM;
    private static final double START_ANGLE = 216, SWEEP_ANGLE = 252;
    private static final int TRACK_WEIGHT = 13, GAP_DEGREES = 2, MINIMUM_SIZE = 240, LABEL_ROOM = 26;
    private static final double INNER_RATIO = 0.86;
    private final transient IntFunction<Color> categoryColour;
    private Integer index;
    private Integer threshold;
    private double shown;
    private transient Motion motion;

    /** Creates a dial that asks the supplied function for the colour of an index. */
    public AqiDial(IntFunction<Color> categoryColour) {
        this.categoryColour = categoryColour;
        setLayout(null);
        setOpaque(false);
        getAccessibleContext().setAccessibleName("U.S. AQI dial");
        describe();
    }

    /** Shows an index, animating from the previous one; {@code null} clears the dial. */
    public void setIndex(Integer value) {
        index = value;
        describe();
        if (motion != null) motion.cancel();
        double from = shown;
        double to = value == null ? 0 : Math.min(value, SCALE_MAXIMUM);
        motion = Motion.play(Metrics.MOTION_DATA, progress -> {
            shown = from + (to - from) * Motion.easeOut(progress);
            repaint();
        });
    }

    /** Marks the user's alert threshold on the track; {@code null} hides the mark. */
    public void setThreshold(Integer value) {
        threshold = value;
        describe();
        repaint();
    }

    /** Returns the index currently displayed, or null when no reading is loaded. */
    public Integer index() { return index; }

    /**
     * Places the figure and category inside the ring. The readout is a real component rather than
     * painted text, so the value stays selectable and visible to assistive technology.
     */
    public void setReadout(JComponent readout) {
        removeAll();
        add(readout);
        revalidate();
    }

    /** Places the figure and its unit inside the ring. */
    @Override public void doLayout() {
        if (getComponentCount() == 0) return;
        double size = Math.min(getWidth(), getHeight()) - TRACK_WEIGHT * 2.0 - LABEL_ROOM;
        double inner = size * INNER_RATIO;
        getComponent(0).setBounds((int) Math.round((getWidth() - inner) / 2),
                (int) Math.round((getHeight() - inner) / 2), (int) Math.round(inner), (int) Math.round(inner));
    }

    /** Returns the dial's preferred square size. */
    @Override public Dimension getPreferredSize() { return new Dimension(MINIMUM_SIZE, MINIMUM_SIZE); }

    /** Returns the smallest size at which the dial stays readable. */
    @Override public Dimension getMinimumSize() { return new Dimension(MINIMUM_SIZE, MINIMUM_SIZE); }

    @Override protected javax.accessibility.AccessibleRole role() { return javax.accessibility.AccessibleRole.PROGRESS_BAR; }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            double size = Math.min(getWidth(), getHeight()) - TRACK_WEIGHT - LABEL_ROOM;
            if (size <= 0) return;
            double x = (getWidth() - size) / 2.0;
            double y = (getHeight() - size) / 2.0;
            paintTrack(g, x, y, size);
            paintSweep(g, x, y, size);
            paintThreshold(g, x, y, size);
        } finally {
            g.dispose();
        }
    }

    private void paintTrack(Graphics2D g, double x, double y, double size) {
        g.setStroke(new BasicStroke(TRACK_WEIGHT, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        double lower = 0;
        for (AirCategory category : AirCategory.values()) {
            double upper = Math.min(category.upperBound(), SCALE_MAXIMUM);
            double from = START_ANGLE - fraction(lower) * SWEEP_ANGLE;
            double to = START_ANGLE - fraction(upper) * SWEEP_ANGLE;
            g.setColor(Paints.alpha(categoryColour.apply((int) upper), Skin.theme().isDark() ? 46 : 38));
            g.draw(new Arc2D.Double(x, y, size, size, to + GAP_DEGREES / 2.0,
                    from - to - GAP_DEGREES, Arc2D.OPEN));
            lower = upper;
        }
    }

    private void paintSweep(Graphics2D g, double x, double y, double size) {
        if (index == null || shown <= 0) return;
        Color colour = categoryColour.apply((int) Math.round(shown));
        double extent = fraction(shown) * SWEEP_ANGLE;
        g.setStroke(new BasicStroke(TRACK_WEIGHT, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Paints.alpha(colour, 70));
        g.draw(new Arc2D.Double(x - 3, y - 3, size + 6, size + 6, START_ANGLE, -extent, Arc2D.OPEN));
        g.setColor(colour);
        g.draw(new Arc2D.Double(x, y, size, size, START_ANGLE, -extent, Arc2D.OPEN));
        paintHead(g, x, y, size, extent, colour);
    }

    private void paintHead(Graphics2D g, double x, double y, double size, double extent, Color colour) {
        double angle = Math.toRadians(START_ANGLE - extent);
        double radius = size / 2;
        double headX = x + radius + Math.cos(angle) * radius;
        double headY = y + radius - Math.sin(angle) * radius;
        double dot = TRACK_WEIGHT * 0.52;
        g.setColor(Skin.color(Tone.SURFACE));
        g.fill(new Ellipse2D.Double(headX - dot, headY - dot, dot * 2, dot * 2));
        g.setStroke(new BasicStroke(3f));
        g.setColor(colour);
        g.draw(new Ellipse2D.Double(headX - dot, headY - dot, dot * 2, dot * 2));
    }

    private void paintThreshold(Graphics2D g, double x, double y, double size) {
        if (threshold == null) return;
        double angle = Math.toRadians(START_ANGLE - fraction(threshold) * SWEEP_ANGLE);
        double outer = size / 2 + TRACK_WEIGHT * 0.78;
        double inner = size / 2 - TRACK_WEIGHT * 0.78;
        double centreX = x + size / 2;
        double centreY = y + size / 2;
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Skin.color(Tone.INK_MUTED));
        g.draw(new java.awt.geom.Line2D.Double(centreX + Math.cos(angle) * inner, centreY - Math.sin(angle) * inner,
                centreX + Math.cos(angle) * outer, centreY - Math.sin(angle) * outer));
        paintThresholdLabel(g, centreX + Math.cos(angle) * (outer + 11), centreY - Math.sin(angle) * (outer + 11));
    }

    private void paintThresholdLabel(Graphics2D g, double x, double y) {
        var font = Type.number(Type.MICRO);
        var metrics = g.getFontMetrics(font);
        String text = "alert " + threshold;
        g.setFont(font);
        g.setColor(Skin.color(Tone.INK_FAINT));
        g.drawString(text, (float) (x - metrics.stringWidth(text) / 2.0), (float) (y + metrics.getAscent() / 2.0));
    }

    /**
     * Position of an index on the track, from 0 at the start to 1 at the end. The dial shares
     * {@link BandScale#position(int)} so the ring and the scale beneath it cannot disagree about
     * where a value sits.
     */
    public static double fraction(double aqi) {
        return BandScale.position((int) Math.round(Math.max(0, aqi)));
    }

    private void describe() {
        String reading = index == null ? "No verified reading" : "U.S. AQI " + index;
        String limit = threshold == null ? "" : ", alert threshold " + threshold;
        getAccessibleContext().setAccessibleDescription(reading + limit);
    }
}
