package cityair.view.design;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The two complete Aurora themes. Every {@link Tone} is authored separately for each theme rather
 * than derived by inverting the other, so contrast, hierarchy and status meaning stay intentional
 * on both backgrounds.
 */
public enum AuroraTheme {
    /** Deep atmospheric interface for low-light and control-room use. */
    DARK("Aurora Dark", true, darkTheme()),
    /** Bright neutral interface for daylight, projection and printing. */
    LIGHT("Aurora Light", false, lightTheme());

    private final String label;
    private final boolean dark;
    private final Map<Tone, Color> colours;

    AuroraTheme(String label, boolean dark, Map<Tone, Color> colours) {
        this.label = label;
        this.dark = dark;
        this.colours = colours;
    }

    /** Returns the colour assigned to a semantic role in this theme. */
    public Color color(Tone tone) {
        Color value = colours.get(Objects.requireNonNull(tone));
        if (value == null) throw new IllegalStateException(label + " does not define " + tone);
        return value;
    }

    /** Returns true when this theme paints light text on dark surfaces. */
    public boolean isDark() { return dark; }

    /** Returns the theme name shown in the settings screen. */
    @Override public String toString() { return label; }

    private static Map<Tone, Color> darkTheme() {
        var map = new EnumMap<Tone, Color>(Tone.class);
        darkSurfaces(map);
        darkText(map);
        darkAccents(map);
        darkStatus(map);
        darkChrome(map);
        return map;
    }

    private static void darkSurfaces(Map<Tone, Color> map) {
        map.put(Tone.CANVAS, rgb(0x0A101B));
        map.put(Tone.CANVAS_EDGE, rgb(0x05090F));
        map.put(Tone.SURFACE, rgb(0x121A28));
        map.put(Tone.SURFACE_RAISED, rgb(0x1A2435));
        map.put(Tone.SURFACE_SUNKEN, rgb(0x0D1420));
        map.put(Tone.STROKE, rgb(0x223047));
        map.put(Tone.STROKE_STRONG, rgb(0x33455F));
    }

    private static void darkText(Map<Tone, Color> map) {
        map.put(Tone.INK, rgb(0xEAF0F8));
        map.put(Tone.INK_MUTED, rgb(0xA3B4C9));
        map.put(Tone.INK_FAINT, rgb(0x7B8EA8));
        map.put(Tone.INK_INVERSE, rgb(0x05100A));
    }

    private static void darkAccents(Map<Tone, Color> map) {
        map.put(Tone.ACCENT, rgb(0x2BC79C));
        map.put(Tone.ACCENT_HOVER, rgb(0x3CDBAF));
        map.put(Tone.ACCENT_PRESSED, rgb(0x1EA480));
        map.put(Tone.ACCENT_SOFT, rgba(0x2BC79C, 40));
        map.put(Tone.ON_ACCENT, rgb(0x04150F));
        map.put(Tone.FOCUS, rgb(0x7BE8CC));
    }

    private static void darkStatus(Map<Tone, Color> map) {
        map.put(Tone.POSITIVE, rgb(0x46D29B));
        map.put(Tone.CAUTION, rgb(0xEBBE5C));
        map.put(Tone.DANGER, rgb(0xFF6B72));
        map.put(Tone.DANGER_SOFT, rgba(0xFF6B72, 38));
        map.put(Tone.INFO, rgb(0x62B0FF));
    }

    private static void darkChrome(Map<Tone, Color> map) {
        map.put(Tone.RAIL, rgb(0x0C1322));
        map.put(Tone.RAIL_EDGE, rgb(0x080D18));
        map.put(Tone.RAIL_INK, rgb(0xE6EDF7));
        map.put(Tone.RAIL_INK_MUTED, rgb(0x8698B0));
        map.put(Tone.RAIL_ACTIVE, rgb(0x162130));
        map.put(Tone.SHADOW, rgba(0x000000, 150));
        map.put(Tone.GLOW, rgba(0x2BC79C, 26));
        map.put(Tone.SKELETON, rgb(0x1C2637));
        map.put(Tone.GRID, rgba(0xFFFFFF, 16));
        map.put(Tone.TOOLTIP, rgb(0x1E2A3C));
        map.put(Tone.TOOLTIP_INK, rgb(0xEAF0F8));
        map.put(Tone.SCRIM, rgba(0x05090F, 190));
    }

    private static Map<Tone, Color> lightTheme() {
        var map = new EnumMap<Tone, Color>(Tone.class);
        lightSurfaces(map);
        lightText(map);
        lightAccents(map);
        lightStatus(map);
        lightChrome(map);
        return map;
    }

    private static void lightSurfaces(Map<Tone, Color> map) {
        map.put(Tone.CANVAS, rgb(0xF5F7FB));
        map.put(Tone.CANVAS_EDGE, rgb(0xEBEFF6));
        map.put(Tone.SURFACE, rgb(0xFFFFFF));
        map.put(Tone.SURFACE_RAISED, rgb(0xFFFFFF));
        map.put(Tone.SURFACE_SUNKEN, rgb(0xF1F4F9));
        map.put(Tone.STROKE, rgb(0xDEE5EF));
        map.put(Tone.STROKE_STRONG, rgb(0xC2CDDD));
    }

    private static void lightText(Map<Tone, Color> map) {
        map.put(Tone.INK, rgb(0x0F1A2A));
        map.put(Tone.INK_MUTED, rgb(0x55657C));
        map.put(Tone.INK_FAINT, rgb(0x76859B));
        map.put(Tone.INK_INVERSE, rgb(0xFFFFFF));
    }

    private static void lightAccents(Map<Tone, Color> map) {
        map.put(Tone.ACCENT, rgb(0x0E8F6E));
        map.put(Tone.ACCENT_HOVER, rgb(0x0C7D60));
        map.put(Tone.ACCENT_PRESSED, rgb(0x09664E));
        map.put(Tone.ACCENT_SOFT, rgba(0x0E8F6E, 28));
        map.put(Tone.ON_ACCENT, rgb(0xFFFFFF));
        map.put(Tone.FOCUS, rgb(0x0B6E55));
    }

    private static void lightStatus(Map<Tone, Color> map) {
        map.put(Tone.POSITIVE, rgb(0x0F8257));
        map.put(Tone.CAUTION, rgb(0x8A5B00));
        map.put(Tone.DANGER, rgb(0xB3242C));
        map.put(Tone.DANGER_SOFT, rgba(0xB3242C, 24));
        map.put(Tone.INFO, rgb(0x155FC0));
    }

    private static void lightChrome(Map<Tone, Color> map) {
        map.put(Tone.RAIL, rgb(0xFFFFFF));
        map.put(Tone.RAIL_EDGE, rgb(0xF7F9FC));
        map.put(Tone.RAIL_INK, rgb(0x16233A));
        map.put(Tone.RAIL_INK_MUTED, rgb(0x5C6B82));
        map.put(Tone.RAIL_ACTIVE, rgb(0xEAF4F0));
        map.put(Tone.SHADOW, rgba(0x1B2A44, 38));
        map.put(Tone.GLOW, rgba(0x0E8F6E, 20));
        map.put(Tone.SKELETON, rgb(0xE7ECF4));
        map.put(Tone.GRID, rgba(0x16233A, 20));
        map.put(Tone.TOOLTIP, rgb(0x16233A));
        map.put(Tone.TOOLTIP_INK, rgb(0xF7FAFF));
        map.put(Tone.SCRIM, rgba(0x16233A, 120));
    }

    private static Color rgb(int value) { return new Color(value, false); }

    private static Color rgba(int value, int alpha) {
        return new Color((value & 0xFFFFFF) | (alpha << 24), true);
    }
}
