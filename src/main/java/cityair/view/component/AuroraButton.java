package cityair.view.component;

import cityair.view.design.Glyph;
import cityair.view.design.Icons;
import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import javax.swing.JButton;

/**
 * The application's button, drawn with Java2D so that fill, border, radius, focus ring and the
 * pressed state all follow the design tokens.
 *
 * <p>Every interaction state required by the design system is implemented: default, hover, pressed,
 * focused, disabled and loading. The loading state keeps the button's width so a toolbar does not
 * jump while a request is in flight.</p>
 */
public final class AuroraButton extends JButton implements Themed {
    /** Visual weight of a button, which also states how prominent its action is. */
    public enum Kind {
        /** The main action of a screen or dialog. */
        PRIMARY,
        /** A supporting action of equal validity but lower prominence. */
        SECONDARY,
        /** A low-emphasis action that must not compete with surrounding content. */
        GHOST
    }

    private static final int PADDING_X = 18, ICON_GAP = 9, SPINNER_SIZE = 15;
    private final transient Kind kind;
    private final transient Glyph glyph;
    private double hover;
    private boolean loading;
    private transient Motion hoverMotion;
    private transient Motion spinner;
    private double spinAngle;

    /** Creates a button with a label and no icon. */
    public AuroraButton(String text, Kind kind) { this(text, kind, null); }

    /** Creates a button with a leading icon. */
    public AuroraButton(String text, Kind kind, Glyph glyph) {
        super(text);
        this.kind = Objects.requireNonNull(kind);
        this.glyph = glyph;
        setFont(Type.strong(Type.LABEL));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setOpaque(false);
        setMargin(new Insets(0, 0, 0, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getModel().addChangeListener(event -> trackHover());
        applyTheme();
    }

    /**
     * Shows or hides the in-button progress indicator and disables the button while it runs.
     * Nothing is delayed to display it: the caller turns it on when work starts and off when the
     * result arrives.
     */
    public void setLoading(boolean value) {
        if (loading == value) return;
        loading = value;
        setEnabled(!value);
        if (spinner != null) spinner.cancel();
        if (value) runSpinner();
        repaint();
    }

    /** Returns true while the button is showing progress. */
    public boolean isLoading() { return loading; }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() {
        setForeground(textColour());
        repaint();
    }

    /** Returns the width the label, icon and padding need. */
    @Override public Dimension getPreferredSize() {
        var metrics = getFontMetrics(getFont());
        int text = metrics.stringWidth(getText());
        int icon = glyph == null ? 0 : Icons.MEDIUM + ICON_GAP;
        int spin = loading ? SPINNER_SIZE + ICON_GAP : 0;
        return new Dimension(text + icon + spin + PADDING_X * 2, Metrics.CONTROL_HEIGHT);
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var shape = Paints.round(0.5, 0.5, getWidth() - 1, getHeight() - 1, Metrics.RADIUS_CONTROL);
            paintSurface(g, shape);
            paintFocus(g, shape);
            paintContent(g);
        } finally {
            g.dispose();
        }
    }

    private void paintSurface(Graphics2D g, RoundRectangle2D shape) {
        if (kind == Kind.PRIMARY && isEnabled()) {
            Paints.shadow(g, shape, Skin.color(Tone.SHADOW), 3, 2);
        }
        Color fill = fillColour();
        if (fill != null) {
            g.setColor(fill);
            g.fill(shape);
        }
        if (kind == Kind.SECONDARY) {
            g.setStroke(new BasicStroke(Metrics.STROKE_HAIRLINE));
            g.setColor(Skin.color(isEnabled() && hover > 0 ? Tone.STROKE_STRONG : Tone.STROKE));
            g.draw(shape);
        }
    }

    private void paintFocus(Graphics2D g, RoundRectangle2D shape) {
        if (!isFocusOwner()) return;
        g.setStroke(new BasicStroke(Metrics.STROKE_EMPHASIS));
        g.setColor(Skin.color(Tone.FOCUS));
        g.draw(Paints.round(2, 2, getWidth() - 4, getHeight() - 4, Metrics.RADIUS_CONTROL - 1));
    }

    private void paintContent(Graphics2D g) {
        var metrics = g.getFontMetrics(getFont());
        int textWidth = metrics.stringWidth(getText());
        int leading = (glyph == null ? 0 : Icons.MEDIUM + ICON_GAP) + (loading ? SPINNER_SIZE + ICON_GAP : 0);
        int x = (getWidth() - textWidth - leading) / 2;
        int middle = getHeight() / 2;
        g.setColor(textColour());
        if (loading) {
            Spinners.paint(g, x, middle - SPINNER_SIZE / 2, SPINNER_SIZE, spinAngle, textColour());
            x += SPINNER_SIZE + ICON_GAP;
        } else if (glyph != null) {
            glyph.paint(g, x, middle - Icons.MEDIUM / 2.0, Icons.MEDIUM);
            x += Icons.MEDIUM + ICON_GAP;
        }
        g.setFont(getFont());
        g.drawString(getText(), x, middle + (metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private Color fillColour() {
        if (!isEnabled()) return kind == Kind.GHOST ? null : Skin.color(Tone.SURFACE_SUNKEN);
        return switch (kind) {
            case PRIMARY -> accentFill();
            case SECONDARY -> Paints.mix(Skin.color(Tone.SURFACE_RAISED), Skin.color(Tone.ACCENT_SOFT), hover * 0.5);
            case GHOST -> hover > 0 ? Skin.color(Tone.ACCENT_SOFT) : null;
        };
    }

    private Color accentFill() {
        if (getModel().isPressed()) return Skin.color(Tone.ACCENT_PRESSED);
        return Paints.mix(Skin.color(Tone.ACCENT), Skin.color(Tone.ACCENT_HOVER), hover);
    }

    private Color textColour() {
        if (!isEnabled()) return Skin.color(Tone.INK_FAINT);
        return kind == Kind.PRIMARY ? Skin.color(Tone.ON_ACCENT) : Skin.color(Tone.INK);
    }

    private void trackHover() {
        double target = getModel().isRollover() && isEnabled() ? 1 : 0;
        if (Math.abs(target - hover) < 0.01) return;
        if (hoverMotion != null) hoverMotion.cancel();
        double from = hover;
        hoverMotion = Motion.play(Metrics.MOTION_FAST, progress -> {
            hover = from + (target - from) * Motion.easeOut(progress);
            repaint();
        });
    }

    private void runSpinner() {
        spinner = Motion.play(Integer.MAX_VALUE, progress -> {
            spinAngle = (spinAngle + 9) % 360;
            repaint();
        });
    }
}
