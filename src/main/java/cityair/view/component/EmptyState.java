package cityair.view.component;

import cityair.view.design.Glyph;
import cityair.view.design.Icons;
import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * What a screen shows when it has nothing to display yet, when a filter matches nothing, or when
 * a request failed.
 *
 * <p>An empty state always says what is missing, why, and what the reader can do next, so these
 * moments stay as considered as the populated interface.</p>
 */
public final class EmptyState extends JPanel implements Themed {
    private static final int HALO = 56;
    private final transient Glyph glyph;
    private final transient Tone tint;

    /** Creates a neutral empty state with a title and an explanation. */
    public static EmptyState of(Glyph glyph, String title, String message) {
        return new EmptyState(glyph, title, message, Tone.INK_MUTED, null);
    }

    /** Creates an empty state that offers one recovery action. */
    public static EmptyState withAction(Glyph glyph, String title, String message, JComponent action) {
        return new EmptyState(glyph, title, message, Tone.INK_MUTED, action);
    }

    /** Creates an empty state that reports a failure, tinted with the danger role. */
    public static EmptyState failure(String title, String message, JComponent action) {
        return new EmptyState(Glyph.WARNING, title, message, Tone.DANGER, action);
    }

    private EmptyState(Glyph glyph, String title, String message, Tone tint, JComponent action) {
        super(new BorderLayout());
        this.glyph = Objects.requireNonNull(glyph);
        this.tint = Objects.requireNonNull(tint);
        setOpaque(false);
        setBorder(new EmptyBorder(Metrics.SPACE_6, Metrics.SPACE_5, Metrics.SPACE_6, Metrics.SPACE_5));
        add(content(title, message, action), BorderLayout.CENTER);
    }

    private JPanel content(String title, String message, JComponent action) {
        var heading = Text.heading(title);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        var explanation = new Paragraph(message, Tone.INK_MUTED, Type.LABEL);
        explanation.setAlignmentX(CENTER_ALIGNMENT);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        var stack = Panels.stack(Metrics.SPACE_3, halo(), heading, explanation);
        if (action != null) {
            stack.add(Panels.gap(Metrics.SPACE_4));
            action.setAlignmentX(CENTER_ALIGNMENT);
            stack.add(action);
        }
        return Panels.top(stack);
    }

    private JComponent halo() {
        var halo = new Halo();
        halo.setAlignmentX(CENTER_ALIGNMENT);
        return halo;
    }

    /** The tinted circle the glyph sits in: the only painted part of an empty state. */
    private final class Halo extends JComponent {
        /** Returns the size of the circular glyph halo. */
        @Override public Dimension getPreferredSize() { return new Dimension(HALO, HALO); }

        @Override protected void paintComponent(Graphics graphics) {
            var g = Paints.quality((Graphics2D) graphics.create());
            try {
                g.setColor(Paints.alpha(Skin.color(tint), 28));
                g.fill(new Ellipse2D.Double(0, 0, HALO, HALO));
                g.setColor(Skin.color(tint));
                glyph.paint(g, (HALO - Icons.LARGE) / 2.0, (HALO - Icons.LARGE) / 2.0, Icons.LARGE);
            } finally {
                g.dispose();
            }
        }
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }
}
