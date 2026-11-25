package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Skin;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JComponent;

/**
 * One measurement: its name, its value with units, an optional reference comparison and an
 * optional sparkline of the samples collected in this session.
 *
 * <p>The tile shows only what the provider actually returned. When a pollutant is missing from the
 * response it says so in words rather than drawing a zero, because a zero is a measurement and an
 * absent field is not.</p>
 */
public final class MetricTile extends Card {
    private static final int MINIMUM_WIDTH = 190, TILE_HEIGHT = 132;
    private final AuroraLabel value = Text.value(Formatting.ABSENT);
    private final AuroraLabel reference = Text.micro(" ");
    private final Sparkline spark = new Sparkline();

    /** Creates a tile for the named measurement, labelled for assistive technology. */
    public MetricTile(String caption, String accessibleName) {
        super(new BorderLayout(0, Metrics.SPACE_2));
        padded(Metrics.SPACE_4);
        add(Text.caption(caption), BorderLayout.NORTH);
        var body = Panels.column(Metrics.SPACE_1);
        body.add(value, BorderLayout.NORTH);
        body.add(reference, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        add(spark, BorderLayout.SOUTH);
        Text.described(this, accessibleName + ".tile", caption);
        Text.described(value, accessibleName, caption);
    }

    /** Sets the headline measurement and the supporting reference line. */
    public void set(String measurement, String context) {
        value.setText(measurement);
        reference.setText(context == null || context.isBlank() ? " " : context);
        getAccessibleContext().setAccessibleDescription(measurement + ". " + reference.getText());
    }

    /** Replaces the session trace drawn at the foot of the tile. */
    public void setTrend(List<Integer> samples, Supplier<Color> tint) {
        spark.setValues(samples, tint);
    }

    /** Hides the trace for measurements that are not collected over time. */
    public MetricTile withoutTrend() {
        spark.setVisible(false);
        return this;
    }

    /** Returns the value label, so the integration tests can read the rendered measurement. */
    public AuroraLabel valueLabel() { return value; }

    /** Keeps a stable tile width while letting the content decide the height it needs. */
    @Override public Dimension getPreferredSize() {
        return new Dimension(MINIMUM_WIDTH, Math.max(TILE_HEIGHT - 34, super.getPreferredSize().height));
    }

    /** Re-reads the tile's colours, including its value and reference lines. */
    @Override public void applyTheme() {
        super.applyTheme();
        value.applyTheme();
        reference.applyTheme();
    }

    /** Smallest width a tile may be given before the grid wraps it onto another row. */
    public static int minimumWidth() { return MINIMUM_WIDTH; }

    /** Marks the tile as showing no verified value. */
    public void clear() {
        value.setForeground(Skin.color(Tone.INK_FAINT));
        value.setFont(Type.text(Type.VALUE - 4));
        set(Formatting.ABSENT, " ");
        spark.setValues(List.of(), () -> Skin.color(Tone.INK_FAINT));
    }

    /** Marks the tile as showing a verified value. */
    public void present() {
        value.setForeground(Skin.color(Tone.INK));
        value.setFont(Type.numberStrong(Type.VALUE));
    }
}
