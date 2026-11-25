package cityair.view.component;

import cityair.view.design.Metrics;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Transparent layout containers. Keeping these in one place stops screens from repeating the same
 * four lines of panel setup and guarantees that every gap comes from the spacing scale.
 */
public final class Panels {
    private Panels() {}

    /** Transparent panel with the given layout. */
    public static JPanel of(LayoutManager layout) {
        var panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** Horizontal arrangement with leading, centre and trailing positions. */
    public static JPanel row(int gap) { return of(new BorderLayout(gap, 0)); }

    /** Vertical arrangement with top, centre and bottom positions. */
    public static JPanel column(int gap) { return of(new BorderLayout(0, gap)); }

    /** Equal-width columns. */
    public static JPanel columns(int count, int gap) { return of(new GridLayout(1, count, gap, 0)); }

    /** Equal-height rows. */
    public static JPanel rows(int gap) { return of(new GridLayout(0, 1, 0, gap)); }

    /**
     * Stacks components top to bottom at their preferred heights, leaving spare space below.
     *
     * <p>Each child is wrapped so that it keeps its preferred height. Without that, a box layout
     * hands spare vertical space to any child whose maximum height is unbounded, which is the
     * default for a panel, and the stack silently stretches its contents apart.</p>
     */
    public static JPanel stack(int gap, Component... children) {
        var panel = of(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < children.length; i++) {
            if (i > 0) panel.add(Box.createVerticalStrut(gap));
            panel.add(rigid(children[i]));
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    /** Wraps a component so a vertical stack cannot stretch it beyond its preferred height. */
    public static JPanel rigid(Component child) {
        var panel = new FixedHeight();
        panel.setOpaque(false);
        panel.add(child, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    /** Wraps a component so it keeps its preferred size instead of filling the cell. */
    public static JPanel leading(Component child) {
        var panel = of(new BorderLayout());
        panel.add(child, BorderLayout.WEST);
        return panel;
    }

    /** Wraps a component so it keeps its preferred height at the top of the available space. */
    public static JPanel top(Component child) {
        var panel = of(new BorderLayout());
        panel.add(child, BorderLayout.NORTH);
        return panel;
    }

    /** Adds outer spacing around a component without changing its own padding. */
    public static JPanel inset(Component child, int space) {
        var panel = of(new BorderLayout());
        panel.setBorder(new EmptyBorder(space, space, space, space));
        panel.add(child, BorderLayout.CENTER);
        return panel;
    }

    /** Centres a component at its preferred size inside the space available. */
    public static JPanel centred(Component child) {
        var panel = of(new java.awt.GridBagLayout());
        panel.add(child, new java.awt.GridBagConstraints());
        return panel;
    }

    /**
     * Fixes a component's width while leaving its height to its content.
     *
     * <p>Setting a preferred size of {@code (width, 0)} is the usual shortcut for a fixed-width
     * column, and it quietly tells every enclosing layout that the component needs no height at
     * all. This wrapper asks the child how tall it wants to be and only overrides the width.</p>
     */
    public static JPanel fixedWidth(Component child, int width) {
        var panel = new FixedWidth(width);
        panel.setOpaque(false);
        panel.add(child, BorderLayout.CENTER);
        return panel;
    }

    /** A panel that reports a set width and its content's own height. */
    private static final class FixedWidth extends JPanel {
        private final int width;

        FixedWidth(int width) {
            super(new BorderLayout());
            this.width = width;
        }

        /** Returns the fixed width with the content's own height. */
        @Override public java.awt.Dimension getPreferredSize() {
            return new java.awt.Dimension(width, super.getPreferredSize().height);
        }

        /** Returns the fixed width with the content's own minimum height. */
        @Override public java.awt.Dimension getMinimumSize() {
            return new java.awt.Dimension(width, super.getMinimumSize().height);
        }
    }

    /** A panel that offers its preferred height as its maximum, for use inside a vertical stack. */
    private static final class FixedHeight extends JPanel {
        FixedHeight() { super(new BorderLayout()); }

        /** Offers the content's preferred height as the maximum. */
        @Override public java.awt.Dimension getMaximumSize() {
            return new java.awt.Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** Fixed empty space, used to separate groups inside a stack. */
    public static Component gap(int height) { return Box.createVerticalStrut(height); }

    /** Standard gap between sections of a screen. */
    public static Component sectionGap() { return Box.createVerticalStrut(Metrics.SPACE_6); }
}
