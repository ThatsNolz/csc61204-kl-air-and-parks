package cityair.view;

import cityair.view.component.AuroraLabel;
import cityair.view.component.AuroraComponent;
import cityair.view.component.Card;
import cityair.view.component.Panels;
import cityair.view.component.SegmentedControl;
import cityair.view.component.Text;
import cityair.view.component.ToggleSwitch;
import cityair.view.design.AuroraTheme;
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
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Settings: appearance, motion and where this application keeps its data.
 *
 * <p>Both themes are offered as a live preview rather than as two words, because a theme is a
 * visual decision and should be made by looking. Reduced motion is a real setting rather than a
 * claim: every animation in the application asks {@link Skin#duration(int)} for its length and
 * receives zero when this switch is on.</p>
 */
final class SettingsPane extends JPanel implements Themed {
    private static final int PREVIEW_WIDTH = 190, PREVIEW_HEIGHT = 108;
    private final SegmentedControl<AuroraTheme> theme =
            new SegmentedControl<>(List.of(AuroraTheme.values()), AuroraTheme::toString);
    private final ToggleSwitch reducedMotion = Text.named(new ToggleSwitch("Reduce motion"), "reducedMotion");
    private final ToggleSwitch collapsedRail = Text.named(new ToggleSwitch("Keep the navigation rail collapsed"),
            "collapseRail");
    private final AuroraLabel storage = Text.micro("");

    SettingsPane() {
        super(new BorderLayout(Metrics.SPACE_5, 0));
        setOpaque(false);
        Text.named(theme, "themeControl");
        add(Panels.stack(Metrics.SPACE_5, appearance(), motion(), data()), BorderLayout.NORTH);
    }

    /** Reports a theme choice. */
    void onTheme(Consumer<AuroraTheme> action) { theme.onChange(action); }

    /** Reports a change to the reduced-motion setting. */
    void onReducedMotion(Consumer<Boolean> action) {
        reducedMotion.addActionListener(event -> action.accept(reducedMotion.isSelected()));
    }

    /** Reports a change to the rail preference. */
    void onRailCollapsed(Consumer<Boolean> action) {
        collapsedRail.addActionListener(event -> action.accept(collapsedRail.isSelected()));
    }

    /** Applies saved settings without reporting them as user changes. */
    void restore(AuroraTheme savedTheme, boolean motionReduced, boolean railCollapsed, Path dataFolder) {
        theme.select(savedTheme);
        reducedMotion.setSelectedQuietly(motionReduced);
        collapsedRail.setSelectedQuietly(railCollapsed);
        storage.setText("Preferences and reports are stored in " + dataFolder);
    }

    private JComponent appearance() {
        var previews = Panels.columns(AuroraTheme.values().length, Metrics.SPACE_4);
        for (AuroraTheme option : AuroraTheme.values()) previews.add(Panels.leading(preview(option)));
        var body = Panels.column(Metrics.SPACE_4);
        body.add(Panels.leading(theme), BorderLayout.NORTH);
        body.add(previews, BorderLayout.CENTER);
        body.add(Text.note("Aurora Dark suits low light and long monitoring sessions. Aurora Light suits "
                + "daylight, projection and printing. The choice is saved on this device."), BorderLayout.SOUTH);
        return Card.titled("Appearance", body);
    }

    private JComponent preview(AuroraTheme option) {
        var swatch = new ThemePreview(option);
        var column = Panels.column(Metrics.SPACE_2);
        column.add(swatch, BorderLayout.CENTER);
        column.add(new AuroraLabel(option.toString(), Type.strong(Type.LABEL), Tone.INK), BorderLayout.SOUTH);
        return column;
    }

    private JComponent motion() {
        var body = Panels.column(Metrics.SPACE_3);
        body.add(reducedMotion, BorderLayout.NORTH);
        body.add(Text.note("Turns off the dial sweep, the sliding navigation indicator, hover transitions and "
                + "the loading shimmer. Values and states still change immediately; only the movement between "
                + "them is removed."), BorderLayout.CENTER);
        body.add(collapsedRail, BorderLayout.SOUTH);
        return Card.titled("Motion and layout", body);
    }

    private JComponent data() {
        var body = Panels.column(Metrics.SPACE_2);
        body.add(storage, BorderLayout.NORTH);
        body.add(Text.note("Nothing is uploaded. Air-quality requests go to the public Open-Meteo endpoint and "
                + "carry only the coordinates of the selected pilot area. Citizen reports never leave this "
                + "device and are not sent to any authority."), BorderLayout.CENTER);
        return Card.titled("Data on this device", body);
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }

    /** A miniature of one theme: its canvas, a card, an accent and a category colour. */
    private static final class ThemePreview extends AuroraComponent {
        private final transient AuroraTheme option;

        ThemePreview(AuroraTheme option) {
            this.option = option;
            getAccessibleContext().setAccessibleName(option + " preview");
            setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        }

        @Override protected void paintComponent(Graphics graphics) {
            var g = Paints.quality((Graphics2D) graphics.create());
            try {
                boolean active = Skin.theme() == option;
                g.setPaint(Paints.vertical(0, getHeight(), option.color(Tone.CANVAS), option.color(Tone.CANVAS_EDGE)));
                g.fill(Paints.round(0, 0, getWidth(), getHeight(), Metrics.RADIUS_CONTROL));
                paintContent(g);
                g.setStroke(new java.awt.BasicStroke(active ? Metrics.STROKE_EMPHASIS : Metrics.STROKE_HAIRLINE));
                g.setColor(active ? Skin.color(Tone.ACCENT) : option.color(Tone.STROKE));
                g.draw(Paints.round(0.5, 0.5, getWidth() - 1, getHeight() - 1, Metrics.RADIUS_CONTROL));
            } finally {
                g.dispose();
            }
        }

        private void paintContent(Graphics2D g) {
            g.setColor(option.color(Tone.RAIL));
            g.fill(Paints.round(0, 0, 34, getHeight(), Metrics.RADIUS_CONTROL));
            g.setColor(option.color(Tone.SURFACE));
            g.fill(Paints.round(44, 14, getWidth() - 58.0, getHeight() - 28.0, Metrics.RADIUS_SMALL));
            g.setColor(option.color(Tone.ACCENT));
            g.fill(Paints.round(54, 26, 54, 8, Metrics.RADIUS_PILL));
            g.setColor(option.color(Tone.INK_MUTED));
            g.fill(Paints.round(54, 44, getWidth() - 86.0, 6, Metrics.RADIUS_PILL));
            g.setColor(option.color(Tone.CAUTION));
            g.fill(Paints.round(54, 60, 30, 6, Metrics.RADIUS_PILL));
        }
    }
}
