package cityair.view.chart;

import cityair.model.District;
import cityair.view.component.AuroraComponent;
import cityair.view.design.Metrics;
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
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javax.swing.JComponent;

/**
 * Where the pilot areas sit relative to one another, drawn from their reference coordinates.
 *
 * <p>This is deliberately not a map. The application has no licensed basemap, no boundaries and no
 * street data, and drawing a shape that resembled Kuala Lumpur would claim a precision the
 * coordinates do not carry. What it does show is true: each area is placed by its own latitude and
 * longitude on an equal-distance projection, with a scale bar derived from the standard length of
 * a degree, so the distances between the markers are real and can be measured.</p>
 */
public final class AreaPlot extends AuroraComponent {
    /** Kilometres in one degree of latitude, WGS84 mean. */
    public static final double KM_PER_DEGREE_LATITUDE = 110.574;
    /** Kilometres in one degree of longitude at the equator, WGS84 mean. */
    public static final double KM_PER_DEGREE_LONGITUDE = 111.320;
    private static final int MARGIN = 54, MARKER = 13, SCALE_BAR_KM = 1, MINIMUM_HEIGHT = 230;
    private static final int LABEL_LINE = 14, LABEL_HEIGHT = 32;
    private final transient IntFunction<Color> categoryColour;
    private final transient Map<District, Integer> values = new EnumMap<>(District.class);
    private final transient Map<District, Point> placed = new EnumMap<>(District.class);
    private transient District selected;
    private transient District hovered;
    private final transient java.util.List<Integer> labelRows = new java.util.ArrayList<>();
    private transient Consumer<District> chooser = area -> { };

    /** Creates the plot with the view's category palette. */
    public AreaPlot(IntFunction<Color> categoryColour) {
        this.categoryColour = categoryColour;
        setOpaque(false);
        setPreferredSize(new Dimension(320, MINIMUM_HEIGHT));
        getAccessibleContext().setAccessibleName("Relative position of the pilot areas");
        addMouseListener(pointer());
        addMouseMotionListener(pointer());
    }

    /** Reports the area whose marker was clicked. */
    public void onChoose(Consumer<District> action) { chooser = java.util.Objects.requireNonNull(action); }

    /** Updates the markers with each area's latest verified index and the current selection. */
    public void setValues(Map<District, Integer> indices, District current) {
        values.clear();
        values.putAll(indices);
        selected = current;
        describe();
        repaint();
    }

    /** Distance in kilometres between two pilot areas, from their reference coordinates. */
    public static double distanceKm(District from, District to) {
        double meanLatitude = Math.toRadians((from.latitude() + to.latitude()) / 2);
        double north = (to.latitude() - from.latitude()) * KM_PER_DEGREE_LATITUDE;
        double east = (to.longitude() - from.longitude()) * KM_PER_DEGREE_LONGITUDE * Math.cos(meanLatitude);
        return Math.hypot(north, east);
    }

    @Override protected javax.accessibility.AccessibleRole role() { return javax.accessibility.AccessibleRole.CANVAS; }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            if (getWidth() < MARGIN * 2 || getHeight() < MARGIN * 2) return;
            place();
            labelRows.clear();
            paintGrid(g);
            paintLinks(g);
            District[] areas = District.values();
            for (District area : areas) paintMarker(g, area);
            paintScaleBar(g);
        } finally {
            g.dispose();
        }
    }

    private void place() {
        var areas = District.values();
        double minLatitude = Double.MAX_VALUE, maxLatitude = -Double.MAX_VALUE;
        double minEast = Double.MAX_VALUE, maxEast = -Double.MAX_VALUE;
        for (District area : areas) {
            minLatitude = Math.min(minLatitude, area.latitude());
            maxLatitude = Math.max(maxLatitude, area.latitude());
            minEast = Math.min(minEast, east(area));
            maxEast = Math.max(maxEast, east(area));
        }
        double scale = scaleFor(maxEast - minEast, maxLatitude - minLatitude);
        for (District area : areas) {
            double x = centre(getWidth()) + (east(area) - (minEast + maxEast) / 2) * scale;
            double y = centre(getHeight()) - (area.latitude() - (minLatitude + maxLatitude) / 2) * scale;
            placed.put(area, new Point((int) Math.round(x), (int) Math.round(y)));
        }
    }

    private double scaleFor(double spanEast, double spanNorth) {
        double usableWidth = getWidth() - MARGIN * 2.0;
        double usableHeight = getHeight() - MARGIN * 2.0;
        return Math.min(usableWidth / Math.max(spanEast, 1e-9), usableHeight / Math.max(spanNorth, 1e-9));
    }

    private static double centre(int extent) { return extent / 2.0; }

    private static double east(District area) {
        return area.longitude() * Math.cos(Math.toRadians(area.latitude()))
                * KM_PER_DEGREE_LONGITUDE / KM_PER_DEGREE_LATITUDE;
    }

    private void paintGrid(Graphics2D g) {
        g.setStroke(new BasicStroke(Metrics.STROKE_HAIRLINE));
        g.setColor(Skin.color(Tone.GRID));
        for (int i = 1; i < 4; i++) {
            double x = getWidth() * i / 4.0;
            double y = getHeight() * i / 4.0;
            g.draw(new Line2D.Double(x, MARGIN / 2.0, x, getHeight() - MARGIN / 2.0));
            g.draw(new Line2D.Double(MARGIN / 2.0, y, getWidth() - MARGIN / 2.0, y));
        }
    }

    private void paintLinks(Graphics2D g) {
        var areas = District.values();
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{3f, 5f}, 0f));
        g.setColor(Paints.alpha(Skin.color(Tone.INK_FAINT), 90));
        for (int i = 0; i < areas.length; i++) {
            for (int j = i + 1; j < areas.length; j++) {
                Point from = placed.get(areas[i]);
                Point to = placed.get(areas[j]);
                g.draw(new Line2D.Double(from.x, from.y, to.x, to.y));
            }
        }
    }

    private void paintMarker(Graphics2D g, District area) {
        Point at = placed.get(area);
        Integer value = values.get(area);
        Color colour = value == null ? Skin.color(Tone.INK_FAINT) : categoryColour.apply(value);
        double size = area == selected ? MARKER + 5 : MARKER;
        if (area == selected || area == hovered) {
            g.setColor(Paints.alpha(colour, 60));
            g.fill(new Ellipse2D.Double(at.x - size, at.y - size, size * 2, size * 2));
        }
        g.setColor(colour);
        g.fill(new Ellipse2D.Double(at.x - size / 2, at.y - size / 2, size, size));
        g.setStroke(new BasicStroke(2f));
        g.setColor(Skin.color(Tone.SURFACE));
        g.draw(new Ellipse2D.Double(at.x - size / 2, at.y - size / 2, size, size));
        paintMarkerLabel(g, area, at, value);
    }

    /**
     * Writes a marker's name and index beside it.
     *
     * <p>The pilot areas are only a few kilometres apart, so at this size their labels would
     * collide. Each label is therefore placed to the side the marker leans towards and pushed
     * clear of any label already written, which keeps all three readable without moving the
     * markers themselves away from their true positions.</p>
     */
    private void paintMarkerLabel(Graphics2D g, District area, Point at, Integer value) {
        boolean current = area == selected;
        boolean toLeft = at.x > getWidth() / 2;
        g.setFont(current ? Type.strong(Type.LABEL) : Type.text(Type.LABEL));
        var metrics = g.getFontMetrics();
        String name = area.toString();
        int y = clearRow(at.y);
        float x = toLeft ? at.x - MARKER - 6f - metrics.stringWidth(name) : at.x + MARKER + 6f;
        g.setColor(Skin.color(current ? Tone.INK : Tone.INK_MUTED));
        g.drawString(name, x, y);
        String reading = value == null ? "no verified reading" : "U.S. AQI " + value;
        g.setFont(Type.number(Type.MICRO));
        var figures = g.getFontMetrics();
        g.setColor(Skin.color(value == null ? Tone.INK_FAINT : Tone.INK_MUTED));
        g.drawString(reading, toLeft ? at.x - MARKER - 6f - figures.stringWidth(reading) : at.x + MARKER + 6f,
                y + LABEL_LINE);
    }

    /** Returns a baseline at least one label apart from every label already placed this frame. */
    private int clearRow(int preferred) {
        int row = preferred;
        boolean moved = true;
        while (moved) {
            moved = false;
            for (int taken : labelRows) {
                if (Math.abs(taken - row) < LABEL_HEIGHT) {
                    row = taken + LABEL_HEIGHT;
                    moved = true;
                }
            }
        }
        labelRows.add(row);
        return row;
    }

    private void paintScaleBar(Graphics2D g) {
        double pixelsPerKm = pixelsPerKilometre();
        if (pixelsPerKm <= 0) return;
        double length = pixelsPerKm * SCALE_BAR_KM;
        double y = getHeight() - 14.0;
        double x = 14;
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.setColor(Skin.color(Tone.INK_FAINT));
        g.draw(new Line2D.Double(x, y, x + length, y));
        g.draw(new Line2D.Double(x, y - 4, x, y + 4));
        g.draw(new Line2D.Double(x + length, y - 4, x + length, y + 4));
        g.setFont(Type.number(Type.MICRO));
        g.drawString(SCALE_BAR_KM + " km", (float) (x + length + 8), (float) (y + 4));
    }

    private double pixelsPerKilometre() {
        var areas = District.values();
        if (areas.length < 2 || placed.size() < 2) return 0;
        Point first = placed.get(areas[0]);
        Point second = placed.get(areas[1]);
        double pixels = Math.hypot(second.x - first.x, second.y - first.y);
        double kilometres = distanceKm(areas[0], areas[1]);
        return kilometres <= 0 ? 0 : pixels / kilometres;
    }

    private MouseAdapter pointer() {
        return new MouseAdapter() {
            /** Tracks the pointer so the component under it can show its hovered state. */
            @Override public void mouseMoved(MouseEvent event) { setHovered(at(event.getX(), event.getY())); }

            /** Clears the hovered state when the pointer leaves. */
            @Override public void mouseExited(MouseEvent event) { setHovered(null); }

            /** Activates the control the pointer was released on. */
            @Override public void mouseClicked(MouseEvent event) {
                District area = at(event.getX(), event.getY());
                if (area != null) chooser.accept(area);
            }
        };
    }

    private District at(int x, int y) {
        for (var entry : placed.entrySet()) {
            if (entry.getValue().distance(x, y) <= MARKER) return entry.getKey();
        }
        return null;
    }

    private void setHovered(District area) {
        if (hovered == area) return;
        hovered = area;
        setCursor(java.awt.Cursor.getPredefinedCursor(
                area == null ? java.awt.Cursor.DEFAULT_CURSOR : java.awt.Cursor.HAND_CURSOR));
        setToolTipText(area == null ? null : "Show " + area);
        repaint();
    }

    private void describe() {
        var areas = District.values();
        var text = new StringBuilder("Reference coordinates, not a map. ");
        for (District area : areas) {
            text.append(area).append(" at ").append(area.latitude()).append(", ").append(area.longitude()).append(". ");
        }
        if (areas.length >= 2) {
            text.append(String.format(java.util.Locale.ENGLISH, "%s and %s are %.1f km apart.",
                    areas[0], areas[1], distanceKm(areas[0], areas[1])));
        }
        getAccessibleContext().setAccessibleDescription(text.toString());
    }
}
