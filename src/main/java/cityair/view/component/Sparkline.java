package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JComponent;

/**
 * A miniature trace of the values collected in this session, drawn without axes.
 *
 * <p>A sparkline answers one question, "which way is this going", and leaves exact values to the
 * table beside it. Fewer than two samples cannot show a direction, so the component says so in
 * words instead of drawing a line that would imply a trend it does not have.</p>
 */
public final class Sparkline extends AuroraComponent {
    private static final int HEIGHT = 34, DOT = 5, INSET = 3;
    private transient List<Integer> values = List.of();
    private transient Supplier<Color> colour = () -> Skin.color(Tone.ACCENT);

    /** Creates an empty sparkline. */
    public Sparkline() {
        setPreferredSize(new Dimension(90, HEIGHT));
        getAccessibleContext().setAccessibleName("Session trend");
    }

    /** Replaces the plotted values, oldest first, and the colour of the trace. */
    public void setValues(List<Integer> samples, Supplier<Color> tint) {
        values = List.copyOf(samples);
        colour = tint;
        getAccessibleContext().setAccessibleDescription(describe());
        setVisible(values.size() >= 2);
        repaint();
    }

    /** Returns a sentence describing the trace, which is also its accessible description. */
    public String describe() {
        if (values.size() < 2) return "Not enough samples to show a direction";
        int change = values.get(values.size() - 1) - values.get(0);
        String direction = change == 0 ? "unchanged" : change > 0 ? "rising" : "falling";
        return values.size() + " session samples, " + direction + " by " + Math.abs(change) + " index points";
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            if (values.size() < 2) {
                paintPlaceholder(g);
                return;
            }
            paintTrace(g);
        } finally {
            g.dispose();
        }
    }

    private void paintPlaceholder(Graphics2D g) {
        g.setStroke(new BasicStroke(Metrics.STROKE_HAIRLINE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10f, new float[]{3f, 4f}, 0f));
        g.setColor(Skin.color(Tone.STROKE_STRONG));
        g.drawLine(INSET, getHeight() / 2, getWidth() - INSET, getHeight() / 2);
    }

    private void paintTrace(Graphics2D g) {
        int highest = values.stream().mapToInt(Integer::intValue).max().orElse(1);
        int lowest = values.stream().mapToInt(Integer::intValue).min().orElse(0);
        double span = Math.max(1, highest - lowest);
        var line = new Path2D.Double();
        double x = INSET;
        double step = (getWidth() - INSET * 2.0) / (values.size() - 1);
        for (int i = 0; i < values.size(); i++) {
            double y = yFor(values.get(i), lowest, span);
            if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
            x += step;
        }
        paintFill(g, line);
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(colour.get());
        g.draw(line);
        double lastY = yFor(values.get(values.size() - 1), lowest, span);
        g.fill(new Ellipse2D.Double(x - step - DOT / 2.0, lastY - DOT / 2.0, DOT, DOT));
    }

    private void paintFill(Graphics2D g, Path2D.Double line) {
        var area = (Path2D.Double) line.clone();
        area.lineTo(getWidth() - INSET, getHeight());
        area.lineTo(INSET, getHeight());
        area.closePath();
        g.setPaint(Paints.vertical(0, getHeight(), Paints.alpha(colour.get(), 60), Paints.alpha(colour.get(), 0)));
        g.fill(area);
    }

    private double yFor(int value, int lowest, double span) {
        double usable = getHeight() - INSET * 2.0 - DOT;
        return getHeight() - INSET - DOT / 2.0 - (value - lowest) / span * usable;
    }
}
