package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * The application's table: quiet horizontal rules, no vertical lines, a flat header and
 * even-width figures in numeric columns.
 *
 * <p>Dense rows are where a design most often becomes unreadable, so the row height, the header
 * treatment and the selection wash all come from the same tokens as the rest of the interface
 * rather than from the Swing defaults.</p>
 */
public final class DataTable extends JTable implements Themed {
    private static final int DEFAULT_VISIBLE_ROWS = 6;
    private final int[] numericColumns;
    private int visibleRows = DEFAULT_VISIBLE_ROWS;

    /** Creates a read-only table whose listed columns are drawn as measurements. */
    public DataTable(DefaultTableModel model, int... numericColumns) {
        super(Objects.requireNonNull(model));
        this.numericColumns = numericColumns.clone();
        setRowHeight(Metrics.ROW_HEIGHT);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setIntercellSpacing(new java.awt.Dimension(0, 0));
        setFillsViewportHeight(true);
        setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setFont(Type.text(Type.LABEL));
        setDefaultRenderer(Object.class, new CellRenderer());
        styleHeader(getTableHeader());
        applyTheme();
    }

    /** Creates a read-only model; cells are never editable in this application. */
    public static DefaultTableModel model(String... headers) {
        return new DefaultTableModel(headers, 0) {
            /** Returns false: every table in this application is read-only. */
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    /**
     * Sets how many rows the table asks for when it is placed in a scroll pane. Swing's default
     * answer is a fixed 450 by 400 box, which leaves a short table floating in empty space.
     */
    public DataTable visibleRows(int rows) {
        visibleRows = Math.max(1, rows);
        return this;
    }

    /** Returns the viewport size this content asks a scroll pane for. */
    @Override public java.awt.Dimension getPreferredScrollableViewportSize() {
        int rows = Math.min(Math.max(getRowCount(), 1), visibleRows);
        return new java.awt.Dimension(super.getPreferredSize().width, rows * getRowHeight());
    }

    /** Sets the preferred width of each column, in order. */
    public void widths(int... widths) {
        for (int i = 0; i < widths.length && i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Installs a renderer on one column, for category colour or any other cell treatment. */
    public void renderer(int column, TableCellRenderer renderer) {
        getColumnModel().getColumn(column).setCellRenderer(renderer);
    }

    /** Returns true when the given column is drawn with the measurement font. */
    public boolean isNumeric(int column) {
        for (int candidate : numericColumns) {
            if (candidate == column) return true;
        }
        return false;
    }

    /** Re-reads the table's surface, grid, selection and header colours. */
    @Override public void applyTheme() {
        setBackground(Skin.color(Tone.SURFACE));
        setForeground(Skin.color(Tone.INK));
        setGridColor(Skin.color(Tone.STROKE));
        setSelectionBackground(Skin.color(Tone.ACCENT_SOFT));
        setSelectionForeground(Skin.color(Tone.INK));
        if (getTableHeader() != null) styleHeader(getTableHeader());
        repaint();
    }

    /** Applies row striping to a prepared cell without disturbing a selection. */
    @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component cell = super.prepareRenderer(renderer, row, column);
        if (cell instanceof JComponent component && !isRowSelected(row)) {
            component.setBackground(row % 2 == 0 ? Skin.color(Tone.SURFACE) : stripe());
        }
        return cell;
    }

    private static Color stripe() {
        return Paints.mix(Skin.color(Tone.SURFACE), Skin.color(Tone.SURFACE_SUNKEN), 0.6);
    }

    private void styleHeader(JTableHeader header) {
        header.setReorderingAllowed(false);
        header.setFont(Type.caption());
        header.setBackground(Skin.color(Tone.SURFACE_SUNKEN));
        header.setForeground(Skin.color(Tone.INK_MUTED));
        header.setPreferredSize(new java.awt.Dimension(0, Metrics.HEADER_HEIGHT));
        header.setDefaultRenderer(new HeaderRenderer());
    }

    /** Draws the header: a flat band with a single rule beneath it. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        /** Prepares this renderer for one cell and returns it. */
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                 boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            // The header keeps the author's capitalisation: uppercasing would turn the micro sign
            // in a unit such as µg/m³ into a Greek capital mu.
            setFont(Type.caption());
            setForeground(Skin.color(Tone.INK_MUTED));
            setBackground(Skin.color(Tone.SURFACE_SUNKEN));
            setOpaque(true);
            setBorder(new EmptyBorder(0, Metrics.SPACE_3, 0, Metrics.SPACE_3));
            return this;
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            var g = (Graphics2D) graphics.create();
            try {
                g.setColor(Skin.color(Tone.STROKE));
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            } finally {
                g.dispose();
            }
        }
    }

    /** Draws one cell, choosing the measurement font for numeric columns. */
    private final class CellRenderer extends DefaultTableCellRenderer {
        /** Prepares this renderer for one cell and returns it. */
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                 boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            boolean numeric = isNumeric(column);
            setFont(numeric ? Type.number(Type.LABEL) : Type.text(Type.LABEL));
            setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEADING);
            setForeground(Skin.color(selected ? Tone.INK : textTone(value)));
            setBorder(new EmptyBorder(0, Metrics.SPACE_3, 0, Metrics.SPACE_3));
            setOpaque(true);
            return this;
        }

        private Tone textTone(Object value) {
            String text = String.valueOf(value);
            return Formatting.DASH.equals(text) || Formatting.NOT_LOADED.equals(text) ? Tone.INK_FAINT : Tone.INK;
        }
    }

    /** Returns a renderer that draws a label with a tinted pill behind it. */
    public static TableCellRenderer pill(java.util.function.Function<Object, Color> tint) {
        return new PillRenderer(tint);
    }

    /** Draws a category cell as a tinted pill with its name written inside. */
    private static final class PillRenderer extends DefaultTableCellRenderer {
        private final transient java.util.function.Function<Object, Color> tint;

        PillRenderer(java.util.function.Function<Object, Color> tint) { this.tint = tint; }

        /** Prepares this renderer for one cell and returns it. */
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                 boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setFont(Type.strong(Type.CAPTION));
            setForeground(Skin.color(Tone.INK));
            setHorizontalAlignment(SwingConstants.LEADING);
            setBorder(new EmptyBorder(0, Metrics.SPACE_3, 0, Metrics.SPACE_3));
            setOpaque(false);
            putClientProperty("pill", tint.apply(value));
            setForeground(Skin.color(tint.apply(value) == null ? Tone.INK_FAINT : Tone.INK));
            return this;
        }

        @Override protected void paintComponent(Graphics graphics) {
            var colour = (Color) getClientProperty("pill");
            if (colour != null) paintPill(graphics, colour);
            super.paintComponent(graphics);
        }

        private void paintPill(Graphics graphics, Color colour) {
            var g = Paints.quality((Graphics2D) graphics.create());
            try {
                int inset = Metrics.SPACE_1 + 2;
                var metrics = g.getFontMetrics(getFont());
                int width = metrics.stringWidth(getText()) + Metrics.SPACE_4;
                g.setColor(Paints.alpha(colour, Skin.theme().isDark() ? 52 : 40));
                g.fill(Paints.round(Metrics.SPACE_2, inset, width, getHeight() - inset * 2.0, Metrics.RADIUS_PILL));
                g.setColor(colour);
                g.fill(Paints.round(Metrics.SPACE_2, inset, 3, getHeight() - inset * 2.0, Metrics.RADIUS_PILL));
            } finally {
                g.dispose();
            }
        }
    }

    /** Returns a label configured as an empty-table placeholder for the surrounding card. */
    public static JLabel placeholder(String message) {
        var label = new AuroraLabel(message, Type.text(Type.LABEL), Tone.INK_FAINT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(new EmptyBorder(Metrics.SPACE_6, 0, Metrics.SPACE_6, 0));
        return label;
    }
}
