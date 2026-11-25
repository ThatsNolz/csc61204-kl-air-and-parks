package cityair.view.design;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * The active appearance of the application: which {@link AuroraTheme} is in use and whether motion
 * is reduced.
 *
 * <p>Appearance is deliberately global, like {@link UIManager}, because a single window hosts every
 * screen. Nothing caches a colour: custom components read {@link #color(Tone)} while painting, and
 * the few controls that store colour state implement {@link Themed}. Switching a theme is therefore
 * one assignment followed by {@link #refresh(Component)}, with no listener registry to leak.</p>
 *
 * <p>All methods must be called on the event dispatch thread.</p>
 */
public final class Skin {
    private static AuroraTheme theme = AuroraTheme.DARK;
    private static boolean reducedMotion;

    private Skin() {}

    /** Returns the active theme. */
    public static AuroraTheme theme() { return theme; }

    /** Returns the colour of a semantic role in the active theme. */
    public static Color color(Tone tone) { return theme.color(tone); }

    /** Returns the active colour with a replacement alpha, for washes, glows and shadows. */
    public static Color color(Tone tone, int alpha) { return Paints.alpha(theme.color(tone), alpha); }

    /** Selects the theme used by every later paint. Call {@link #refresh(Component)} afterwards. */
    public static void setTheme(AuroraTheme value) {
        theme = Objects.requireNonNull(value);
        installDefaults();
    }

    /** Returns true when animations should be skipped and final states applied immediately. */
    public static boolean reducedMotion() { return reducedMotion; }

    /** Turns extended motion on or off for the whole application. */
    public static void setReducedMotion(boolean value) { reducedMotion = value; }

    /** Returns the duration an animation should use, which is zero when motion is reduced. */
    public static int duration(int milliseconds) { return reducedMotion ? 0 : milliseconds; }

    /**
     * Re-applies the theme to a component tree: every {@link Themed} descendant re-reads its
     * colours and the whole tree repaints.
     */
    public static void refresh(Component root) {
        if (root instanceof Themed themed) themed.applyTheme();
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) refresh(child);
        }
        if (root instanceof JComponent component) component.revalidate();
        root.repaint();
    }

    /** Writes the shared Swing defaults that cannot be reached by painting, such as tooltips. */
    public static void installDefaults() {
        UIManager.put("ToolTip.background", color(Tone.TOOLTIP));
        UIManager.put("ToolTip.foreground", color(Tone.TOOLTIP_INK));
        UIManager.put("ToolTip.font", Type.text(Type.LABEL));
        UIManager.put("Panel.background", color(Tone.SURFACE));
        UIManager.put("OptionPane.background", color(Tone.SURFACE));
        UIManager.put("OptionPane.messageForeground", color(Tone.INK));
        UIManager.put("ComboBox.background", color(Tone.SURFACE_RAISED));
        UIManager.put("ComboBox.foreground", color(Tone.INK));
        UIManager.put("ComboBox.selectionBackground", color(Tone.ACCENT_SOFT));
        UIManager.put("ComboBox.selectionForeground", color(Tone.INK));
        UIManager.put("TextField.caretForeground", color(Tone.ACCENT));
        UIManager.put("TextArea.caretForeground", color(Tone.ACCENT));
    }
}
