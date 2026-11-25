package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JComponent;

/**
 * A compact status pill: a coloured dot, a short label and a tinted background.
 *
 * <p>The label always states the status in words, so the dot and the tint reinforce the meaning
 * rather than carry it. That keeps the badge readable for a colour-blind reader and in a printed,
 * greyscale copy of the report.</p>
 */
public final class Badge extends AuroraComponent {
    private static final int PADDING_X = 10, PADDING_Y = 5, DOT = 7, DOT_GAP = 7;
    private String text;
    private transient Supplier<Color> colour;
    private boolean showDot = true;

    /** Creates a badge whose colour follows a semantic role. */
    public Badge(String text, Tone tone) {
        this(text, () -> Skin.color(tone));
    }

    /** Creates a badge whose colour is resolved at paint time, such as an air-quality category. */
    public Badge(String text, Supplier<Color> colour) {
        this.text = Objects.requireNonNull(text);
        this.colour = Objects.requireNonNull(colour);
        setFont(Type.strong(Type.CAPTION));
        getAccessibleContext().setAccessibleName(text);
    }

    /** Replaces the label and the colour together, so the two can never disagree. */
    public void set(String value, Supplier<Color> tint) {
        text = Objects.requireNonNull(value);
        colour = Objects.requireNonNull(tint);
        getAccessibleContext().setAccessibleName(value);
        revalidate();
        repaint();
    }

    /** Hides the leading dot, for badges that sit inside an already coloured surface. */
    public Badge withoutDot() {
        showDot = false;
        return this;
    }

    /** Returns the current label, which the integration tests read as the status in words. */
    public String text() { return text; }

    /** Returns the pill's size: its label plus padding and the dot. */
    @Override public Dimension getPreferredSize() {
        var metrics = getFontMetrics(getFont());
        int dot = showDot ? DOT + DOT_GAP : 0;
        return new Dimension(metrics.stringWidth(text) + dot + PADDING_X * 2,
                metrics.getHeight() + PADDING_Y * 2);
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            Color tint = colour.get();
            g.setColor(Paints.alpha(tint, Skin.theme().isDark() ? 42 : 30));
            g.fill(Paints.round(0, 0, getWidth(), getHeight(), Metrics.RADIUS_PILL));
            var metrics = g.getFontMetrics(getFont());
            int x = PADDING_X + paintDot(g, tint);
            g.setFont(getFont());
            g.setColor(readable(tint));
            g.drawString(text, x, getHeight() / 2 + (metrics.getAscent() - metrics.getDescent()) / 2);
        } finally {
            g.dispose();
        }
    }

    /** Draws the status dot when the pill shows one, and returns the width it takes from the label. */
    private int paintDot(Graphics2D g, Color tint) {
        if (!showDot) return 0;
        g.setColor(tint);
        g.fill(new Ellipse2D.Double(PADDING_X, (getHeight() - DOT) / 2.0, DOT, DOT));
        return DOT + DOT_GAP;
    }

    private static Color readable(Color tint) {
        Color ink = Skin.color(Tone.INK);
        return Skin.theme().isDark() ? Paints.mix(ink, tint, 0.55) : Paints.mix(ink, tint, 0.35);
    }
}
