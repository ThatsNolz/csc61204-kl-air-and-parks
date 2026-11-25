package cityair.view.component;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * A grid that chooses its own column count from the width it is given: tiles keep a readable
 * minimum width and wrap onto another row instead of being squeezed.
 *
 * <p>This is how the metric tiles move from four columns on a wide window to two and then one as
 * the window narrows, with no absolute positioning and no breakpoint logic inside the screens.</p>
 */
public final class FlowGrid implements LayoutManager {
    private final int minimumTileWidth;
    private final int maximumColumns;
    private final int gap;

    /** Lays out children at least {@code minimumTileWidth} wide, up to {@code maximumColumns}. */
    public FlowGrid(int minimumTileWidth, int maximumColumns, int gap) {
        this.minimumTileWidth = Math.max(1, minimumTileWidth);
        this.maximumColumns = Math.max(1, maximumColumns);
        this.gap = gap;
    }

    /** Returns the number of columns that fit in the given width. */
    public int columnsFor(int width) {
        int fitting = (width + gap) / (minimumTileWidth + gap);
        return Math.max(1, Math.min(maximumColumns, fitting));
    }

    /** Ignored: this layout positions children by their order, not by a constraint. */
    @Override public void addLayoutComponent(String name, Component component) { }

    /** Ignored: this layout holds no per-child state. */
    @Override public void removeLayoutComponent(Component component) { }

    /** Returns the size the children need at the current column count. */
    @Override public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            var insets = parent.getInsets();
            int available = Math.max(parent.getWidth() - insets.left - insets.right, minimumTileWidth);
            int columns = columnsFor(available);
            int rows = rowCount(parent.getComponentCount(), columns);
            int height = rows * tallestChild(parent) + Math.max(0, rows - 1) * gap;
            return new Dimension(insets.left + insets.right + columns * minimumTileWidth + (columns - 1) * gap,
                    insets.top + insets.bottom + height);
        }
    }

    /** Returns the size below which a tile would be narrower than its minimum. */
    @Override public Dimension minimumLayoutSize(Container parent) {
        var insets = parent.getInsets();
        return new Dimension(insets.left + insets.right + minimumTileWidth,
                preferredLayoutSize(parent).height);
    }

    /** Places every child in the grid the current width allows. */
    @Override public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int available = parent.getWidth() - insets.left - insets.right;
            int columns = columnsFor(available);
            int cell = (available - (columns - 1) * gap) / columns;
            int height = tallestChild(parent);
            placeChildren(parent, insets, columns, cell, height);
        }
    }

    private void placeChildren(Container parent, Insets insets, int columns, int cell, int height) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            int column = i % columns;
            int row = i / columns;
            parent.getComponent(i).setBounds(insets.left + column * (cell + gap),
                    insets.top + row * (height + gap), cell, height);
        }
    }

    private static int rowCount(int children, int columns) {
        return (children + columns - 1) / columns;
    }

    private static int tallestChild(Container parent) {
        int tallest = 0;
        for (Component child : parent.getComponents()) {
            tallest = Math.max(tallest, child.getPreferredSize().height);
        }
        return tallest;
    }
}
