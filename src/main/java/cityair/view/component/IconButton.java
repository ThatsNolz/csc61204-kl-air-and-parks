package cityair.view.component;

import cityair.view.design.Glyph;
import cityair.view.design.Icons;
import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import javax.swing.JButton;

/**
 * A square button that shows one {@link Glyph}. Because an icon alone is not an accessible label,
 * the constructor requires a description, which becomes both the tooltip and the accessible name.
 */
public final class IconButton extends JButton implements Themed {
    private transient Glyph glyph;
    private final transient Tone tone;

    /** Creates an icon button with the required spoken and hover description. */
    public IconButton(Glyph glyph, String description, Tone tone) {
        this.glyph = Objects.requireNonNull(glyph);
        this.tone = Objects.requireNonNull(tone);
        setToolTipText(description);
        getAccessibleContext().setAccessibleName(description);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(Metrics.ICON_BUTTON, Metrics.ICON_BUTTON));
    }

    /** Replaces the icon, for controls that toggle between two meanings. */
    public void setGlyph(Glyph value, String description) {
        glyph = Objects.requireNonNull(value);
        setToolTipText(description);
        getAccessibleContext().setAccessibleName(description);
        repaint();
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var shape = Paints.round(1, 1, getWidth() - 2, getHeight() - 2, Metrics.RADIUS_CONTROL);
            if (getModel().isRollover() || getModel().isPressed()) {
                g.setColor(Skin.color(getModel().isPressed() ? Tone.ACCENT_SOFT : Tone.SURFACE_RAISED));
                g.fill(shape);
            }
            if (isFocusOwner()) {
                g.setStroke(new BasicStroke(Metrics.STROKE_EMPHASIS));
                g.setColor(Skin.color(Tone.FOCUS));
                g.draw(shape);
            }
            g.setColor(Skin.color(isEnabled() ? tone : Tone.INK_FAINT));
            glyph.paint(g, (getWidth() - Icons.MEDIUM) / 2.0, (getHeight() - Icons.MEDIUM) / 2.0, Icons.MEDIUM);
        } finally {
            g.dispose();
        }
    }
}
