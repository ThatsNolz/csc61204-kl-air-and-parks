package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * The surface every screen is composed from: a rounded panel with a hairline border, optional
 * elevation and an optional accent edge that carries state or category colour.
 *
 * <p>A card can be made interactive, in which case it reacts to the pointer, takes keyboard focus,
 * draws a visible focus ring and activates with Enter or Space, so a clickable card is never a
 * mouse-only control.</p>
 */
public class Card extends JPanel implements Themed {
    private static final int SHADOW_ROOM = 4, ACCENT_WIDTH = 4;
    private int radius = Metrics.RADIUS_CARD;
    private int elevation = 1;
    private transient Supplier<Color> accent;
    private transient Supplier<Color> fill = () -> Skin.color(Tone.SURFACE);
    private boolean selected;
    private boolean hovered;
    private double lift;
    private transient Motion liftMotion;

    /** Creates a padded card using the given layout for its content. */
    public Card(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(padding(Metrics.SPACE_5));
    }

    /** Creates a card whose content is stacked vertically with the standard gap. */
    public Card() { this(new BorderLayout(0, Metrics.SPACE_3)); }

    /** Creates a card with a caption above its content. */
    public static Card titled(String caption, JComponent content) {
        var card = new Card(new BorderLayout(0, Metrics.SPACE_3));
        card.add(Text.caption(caption), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** Creates a card with a caption, a trailing control on the caption row, and content. */
    public static Card titled(String caption, JComponent trailing, JComponent content) {
        var header = Panels.row(Metrics.SPACE_3);
        header.add(Text.caption(caption), BorderLayout.CENTER);
        header.add(trailing, BorderLayout.EAST);
        var card = new Card(new BorderLayout(0, Metrics.SPACE_3));
        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** Sets the inner padding of the card. */
    public final Card padded(int space) {
        setBorder(padding(space));
        return this;
    }

    /** Sets the corner radius, for heroes and compact chips that differ from the default. */
    public final Card radius(int value) {
        radius = value;
        return this;
    }

    /** Sets the shadow depth; zero draws a flat card. */
    public final Card elevation(int level) {
        elevation = level;
        return this;
    }

    /** Draws a coloured edge on the left of the card, resolved at paint time. */
    public final Card accent(Supplier<Color> colour) {
        accent = colour;
        return this;
    }

    /** Overrides the card fill, for wells and status surfaces. */
    public final Card fill(Supplier<Color> colour) {
        fill = Objects.requireNonNull(colour);
        return this;
    }

    /** Makes the card activate the given action by click, Enter or Space. */
    public final Card interactive(String accessibleName, Runnable action) {
        Objects.requireNonNull(action);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        getAccessibleContext().setAccessibleName(accessibleName);
        addMouseListener(pointer(action));
        addFocusListener(new FocusAdapter() {
            /** Repaints so the focus ring appears. */
            @Override public void focusGained(FocusEvent e) { repaint(); }
            /** Repaints so the focus ring disappears. */
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
        bindActivation(action);
        return this;
    }

    /** Marks the card as the chosen one in a group; selection is also shown by its accent edge. */
    public final void setSelected(boolean value) {
        if (selected == value) return;
        selected = value;
        repaint();
    }

    /** Returns true when this card is the chosen one in its group. */
    public final boolean isSelected() { return selected; }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var shape = shape();
            if (elevation > 0) Paints.shadow(g, shape, Skin.color(Tone.SHADOW), elevation + 2, elevation + 1);
            g.setColor(fill.get());
            g.fill(shape);
            paintAccent(g, shape);
            paintOutline(g, shape);
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    private RoundRectangle2D shape() {
        double inset = elevation > 0 ? SHADOW_ROOM : 1;
        return Paints.round(inset, inset - lift, getWidth() - inset * 2, getHeight() - inset * 2, radius);
    }

    private void paintAccent(Graphics2D g, RoundRectangle2D shape) {
        if (accent == null) return;
        var strip = new Area(shape);
        strip.intersect(new Area(new java.awt.geom.Rectangle2D.Double(
                shape.getX(), shape.getY(), ACCENT_WIDTH, shape.getHeight())));
        g.setColor(accent.get());
        g.fill(strip);
    }

    private void paintOutline(Graphics2D g, RoundRectangle2D shape) {
        boolean focused = isFocusOwner();
        g.setStroke(new BasicStroke(focused || selected ? Metrics.STROKE_EMPHASIS : Metrics.STROKE_HAIRLINE));
        g.setColor(outlineColour(focused));
        g.draw(shape);
    }

    private Color outlineColour(boolean focused) {
        if (focused) return Skin.color(Tone.FOCUS);
        if (selected) return Skin.color(Tone.ACCENT);
        return hovered ? Skin.color(Tone.STROKE_STRONG) : Skin.color(Tone.STROKE);
    }

    private MouseAdapter pointer(Runnable action) {
        return new MouseAdapter() {
            /** Activates the control the pointer was released on. */
            @Override public void mouseClicked(MouseEvent event) { action.run(); }
            /** Lifts the card slightly while the pointer is over it. */
            @Override public void mouseEntered(MouseEvent event) { setHovered(true); }
            /** Clears the hovered state when the pointer leaves. */
            @Override public void mouseExited(MouseEvent event) { setHovered(false); }
        };
    }

    private void bindActivation(Runnable action) {
        var activate = new AbstractAction() {
            /** Runs the action this control is bound to. */
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        };
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activateCard");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "activateCard");
        getActionMap().put("activateCard", activate);
    }

    private void setHovered(boolean value) {
        hovered = value;
        if (liftMotion != null) liftMotion.cancel();
        double from = lift;
        double to = value ? 2 : 0;
        liftMotion = Motion.play(Metrics.MOTION_FAST, progress -> {
            lift = from + (to - from) * Motion.easeOut(progress);
            repaint();
        });
    }

    private static EmptyBorder padding(int space) {
        return new EmptyBorder(space + SHADOW_ROOM, space + SHADOW_ROOM, space + SHADOW_ROOM, space + SHADOW_ROOM);
    }
}
