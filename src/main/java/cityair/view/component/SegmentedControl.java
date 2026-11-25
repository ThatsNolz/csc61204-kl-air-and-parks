package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * A row of mutually exclusive choices with an indicator that slides to the selected option.
 *
 * <p>Used where a drop-down would hide the alternatives: the options are short, few and worth
 * comparing at a glance. Arrow keys move the selection, so the control is fully usable from the
 * keyboard, and the selected option is named for assistive technology on every change.</p>
 *
 * @param <T> the type of the options offered
 */
public final class SegmentedControl<T> extends AuroraComponent {
    private static final int PADDING = 14, INSET = 3;
    private final transient List<T> options;
    private final transient Function<T, String> naming;
    private transient Consumer<T> listener = value -> { };
    private int selected;
    private int hovered = -1;
    private double indicator;
    private transient Motion motion;

    /** Creates a control over the given options, labelled by {@code naming}. */
    public SegmentedControl(List<T> options, Function<T, String> naming) {
        this.options = List.copyOf(options);
        this.naming = Objects.requireNonNull(naming);
        if (this.options.isEmpty()) throw new IllegalArgumentException("A segmented control needs options.");
        setFont(Type.strong(Type.LABEL));
        setFocusable(true);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        addMouseListener(pointer());
        addMouseMotionListener(pointer());
        bindKeys();
        describe();
    }

    /** Reports the option the user chose. */
    public void onChange(Consumer<T> action) { listener = Objects.requireNonNull(action); }

    /** Returns the selected option. */
    public T selected() { return options.get(selected); }

    /** Selects an option without reporting a change, used when restoring a saved choice. */
    public void select(T value) {
        int index = options.indexOf(value);
        if (index < 0 || index == selected) return;
        selected = index;
        indicator = index;
        describe();
        repaint();
    }

    /** Returns the width every option needs at full size. */
    @Override public Dimension getPreferredSize() {
        var metrics = getFontMetrics(getFont());
        int widest = options.stream().mapToInt(o -> metrics.stringWidth(naming.apply(o))).max().orElse(0);
        return new Dimension((widest + PADDING * 2) * options.size() + INSET * 2, Metrics.CONTROL_HEIGHT);
    }

    /** The narrowest the control can be before its labels would be clipped. */
    @Override public Dimension getMinimumSize() {
        var metrics = getFontMetrics(getFont());
        int widest = options.stream().mapToInt(o -> metrics.stringWidth(naming.apply(o))).max().orElse(0);
        return new Dimension((widest + Metrics.SPACE_2 * 2) * options.size() + INSET * 2, Metrics.CONTROL_HEIGHT);
    }

    /** Lets the control stretch horizontally but never vertically. */
    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, Metrics.CONTROL_HEIGHT);
    }

    @Override protected javax.accessibility.AccessibleRole role() { return javax.accessibility.AccessibleRole.PAGE_TAB_LIST; }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var track = Paints.round(0.5, 0.5, getWidth() - 1, getHeight() - 1, Metrics.RADIUS_CONTROL);
            g.setColor(Skin.color(Tone.SURFACE_SUNKEN));
            g.fill(track);
            g.setStroke(new BasicStroke(Metrics.STROKE_HAIRLINE));
            g.setColor(Skin.color(Tone.STROKE));
            g.draw(track);
            paintIndicator(g);
            paintLabels(g);
        } finally {
            g.dispose();
        }
    }

    private void paintIndicator(Graphics2D g) {
        double width = segmentWidth();
        var shape = Paints.round(INSET + indicator * width, INSET, width, getHeight() - INSET * 2.0,
                Metrics.RADIUS_CONTROL - 2);
        Paints.shadow(g, shape, Skin.color(Tone.SHADOW), 2, 1);
        g.setColor(Skin.color(Tone.SURFACE_RAISED));
        g.fill(shape);
        g.setStroke(new BasicStroke(isFocusOwner() ? Metrics.STROKE_EMPHASIS : Metrics.STROKE_HAIRLINE));
        g.setColor(Skin.color(isFocusOwner() ? Tone.FOCUS : Tone.ACCENT));
        g.draw(shape);
    }

    private void paintLabels(Graphics2D g) {
        var metrics = g.getFontMetrics(getFont());
        double width = segmentWidth();
        int baseline = getHeight() / 2 + (metrics.getAscent() - metrics.getDescent()) / 2;
        g.setFont(getFont());
        for (int i = 0; i < options.size(); i++) {
            String label = naming.apply(options.get(i));
            int x = (int) (INSET + i * width + (width - metrics.stringWidth(label)) / 2);
            g.setColor(labelColour(i));
            g.drawString(label, x, baseline);
        }
    }

    private java.awt.Color labelColour(int index) {
        if (index == selected) return Skin.color(Tone.INK);
        return Skin.color(index == hovered ? Tone.INK : Tone.INK_MUTED);
    }

    private double segmentWidth() { return (getWidth() - INSET * 2.0) / options.size(); }

    private MouseAdapter pointer() {
        return new MouseAdapter() {
            /** Takes focus, so the keyboard can continue what the pointer started. */
            @Override public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                choose(indexAt(event.getX()));
            }

            /** Tracks the pointer so the component under it can show its hovered state. */
            @Override public void mouseMoved(MouseEvent event) { setHovered(indexAt(event.getX())); }

            /** Clears the hovered state when the pointer leaves. */
            @Override public void mouseExited(MouseEvent event) { setHovered(-1); }
        };
    }

    private int indexAt(int x) {
        int index = (int) ((x - INSET) / segmentWidth());
        return Math.max(0, Math.min(options.size() - 1, index));
    }

    private void setHovered(int index) {
        if (hovered == index) return;
        hovered = index;
        repaint();
    }

    private void bindKeys() {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previous");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
        getActionMap().put("previous", step(-1));
        getActionMap().put("next", step(1));
    }

    private AbstractAction step(int delta) {
        return new AbstractAction() {
            /** Runs the action this control is bound to. */
            @Override public void actionPerformed(ActionEvent event) {
                choose(Math.max(0, Math.min(options.size() - 1, selected + delta)));
            }
        };
    }

    private void choose(int index) {
        if (index == selected) return;
        selected = index;
        describe();
        slideTo(index);
        listener.accept(options.get(index));
    }

    private void slideTo(int index) {
        if (motion != null) motion.cancel();
        double from = indicator;
        motion = Motion.play(Metrics.MOTION_BASE, progress -> {
            indicator = from + (index - from) * Motion.easeOut(progress);
            repaint();
        });
    }

    private void describe() {
        getAccessibleContext().setAccessibleDescription("Selected: " + naming.apply(options.get(selected)));
    }
}
