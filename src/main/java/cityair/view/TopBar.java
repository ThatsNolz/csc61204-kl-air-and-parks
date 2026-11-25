package cityair.view;

import cityair.model.District;
import cityair.view.component.AuroraButton;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Badge;
import cityair.view.component.Dropdown;
import cityair.view.component.IconButton;
import cityair.view.component.Panels;
import cityair.view.component.Text;
import cityair.view.design.AuroraTheme;
import cityair.view.design.Glyph;
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
import java.time.Instant;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The bar across the top of the application: identity on the left, the area being shown in the
 * middle, and the actions that change what is shown on the right.
 *
 * <p>The two refresh actions sit beside the area selector because they act on it, and the data
 * state travels with them: which area, how it was obtained and how long ago, in one line. The
 * theme and rail controls are on the far right, away from the data actions, so a visual
 * preference can never be clicked by someone reaching for a refresh.</p>
 */
final class TopBar extends JPanel implements Themed {
    private final Dropdown<District> district = Text.named(new Dropdown<>(District.values()), "district");
    private final AuroraButton refresh = Text.named(new AuroraButton("Refresh district",
            AuroraButton.Kind.PRIMARY, Glyph.REFRESH), "refresh");
    private final AuroraButton refreshAll = Text.named(new AuroraButton("Compare all areas",
            AuroraButton.Kind.SECONDARY, Glyph.COMPARE), "refreshAll");
    private final IconButton themeToggle = Text.named(new IconButton(Glyph.SUN, "Switch to Aurora Light",
            Tone.INK_MUTED), "themeToggle");
    private final IconButton railToggle = Text.named(new IconButton(Glyph.CHEVRON_LEFT, "Collapse the navigation",
            Tone.INK_MUTED), "railToggle");
    private final Badge state = Text.named(new Badge("Not loaded", Tone.INK_MUTED), "dataState");
    private final AuroraLabel updated = Text.named(Text.micro("No successful update yet"), "lastUpdated");

    TopBar() {
        super(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(0, Metrics.TOP_BAR_HEIGHT));
        add(brand(), BorderLayout.WEST);
        add(centre(), BorderLayout.CENTER);
        add(actions(), BorderLayout.EAST);
    }

    /** The area selector, owned here and shared with the rest of the shell. */
    Dropdown<District> districtSelector() { return district; }

    /** The primary refresh action. */
    AuroraButton refreshButton() { return refresh; }

    /** The action that refreshes every pilot area in turn. */
    AuroraButton compareButton() { return refreshAll; }

    /** The theme switch in the bar; the full control lives in Settings. */
    IconButton themeButton() { return themeToggle; }

    /** The navigation rail collapse control. */
    IconButton railButton() { return railToggle; }

    /** Updates the icon and description of the theme switch for the active theme. */
    void showTheme(AuroraTheme active) {
        boolean dark = active.isDark();
        themeToggle.setGlyph(dark ? Glyph.SUN : Glyph.MOON,
                dark ? "Switch to Aurora Light" : "Switch to Aurora Dark");
    }

    /** Updates the icon and description of the rail control. */
    void showRail(boolean collapsed) {
        railToggle.setGlyph(collapsed ? Glyph.CHEVRON_RIGHT : Glyph.CHEVRON_LEFT,
                collapsed ? "Expand the navigation" : "Collapse the navigation");
    }

    /** Shows how the reading on screen was obtained and when the last success happened. */
    void showState(String label, Tone tone, Instant lastSuccess, Instant now) {
        state.set(label, () -> Skin.color(tone));
        updated.setText(lastSuccess == null
                ? "No successful update yet"
                : "Updated " + Formats.since(lastSuccess, now));
    }

    private JComponent brand() {
        var title = new AuroraLabel("KL Air & Parks", Type.strong(Type.HEADING), Tone.INK);
        var tagline = new AuroraLabel("SDG 11 pilot", Type.text(Type.MICRO),
                Tone.INK_MUTED);
        var text = Panels.stack(2, title, tagline);
        title.setAlignmentX(LEFT_ALIGNMENT);
        tagline.setAlignmentX(LEFT_ALIGNMENT);
        var brand = Panels.row(Metrics.SPACE_3);
        brand.setBorder(new EmptyBorder(0, Metrics.SPACE_5, 0, Metrics.SPACE_5));
        brand.add(Panels.centred(new BrandMark()), BorderLayout.WEST);
        brand.add(Panels.centred(text), BorderLayout.CENTER);
        return brand;
    }

    private JComponent centre() {
        district.setPreferredSize(new Dimension(250, Metrics.CONTROL_HEIGHT));
        var picker = Panels.row(Metrics.SPACE_3);
        picker.add(Text.caption("Pilot area"), BorderLayout.WEST);
        picker.add(Panels.leading(district), BorderLayout.CENTER);
        var status = Panels.row(Metrics.SPACE_2);
        status.add(state, BorderLayout.WEST);
        status.add(updated, BorderLayout.CENTER);
        var row = Panels.row(Metrics.SPACE_5);
        row.add(picker, BorderLayout.WEST);
        row.add(Panels.leading(status), BorderLayout.CENTER);
        return Panels.inset(row, Metrics.SPACE_3);
    }

    private JComponent actions() {
        var buttons = Panels.row(Metrics.SPACE_3);
        buttons.add(refreshAll, BorderLayout.WEST);
        buttons.add(refresh, BorderLayout.CENTER);
        var toggles = Panels.row(Metrics.SPACE_1);
        toggles.add(railToggle, BorderLayout.WEST);
        toggles.add(themeToggle, BorderLayout.EAST);
        var row = Panels.row(Metrics.SPACE_5);
        row.add(buttons, BorderLayout.CENTER);
        row.add(toggles, BorderLayout.EAST);
        return Panels.inset(row, Metrics.SPACE_3);
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            g.setColor(Paints.alpha(Skin.color(Tone.SURFACE), Skin.theme().isDark() ? 170 : 220));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Skin.color(Tone.STROKE));
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }
}
