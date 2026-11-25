package cityair.view.design;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The Aurora type system: one resolved text family, one resolved numeric family and a fixed scale.
 * Measurements use the numeric family because its digits are even in width, so a changing value
 * never makes a table column or a gauge readout shift sideways.
 */
public final class Type {
    /** Size of the figure at the centre of the air-quality dial. */
    public static final float DISPLAY = 58f;
    /** Size of a screen title. */
    public static final float TITLE = 25f;
    /** Size of a card or section heading. */
    public static final float HEADING = 17f;
    /** Size of an emphasised value inside a metric tile. */
    public static final float VALUE = 23f;
    /** Size of ordinary body text. */
    public static final float BODY = 14f;
    /** Size of control labels and table cells. */
    public static final float LABEL = 13f;
    /** Size of uppercase captions above values. */
    public static final float CAPTION = 11f;
    /** Size of axis ticks and footnotes. */
    public static final float MICRO = 10.5f;
    /** Letter spacing applied to uppercase captions, as a fraction of the font size. */
    public static final float CAPTION_TRACKING = 0.09f;

    private static final String TEXT_FAMILY = resolve("Segoe UI Variable Text", "Segoe UI", "Inter",
            "SF Pro Text", "Helvetica Neue", "Arial");
    private static final String STRONG_FAMILY = resolve("Segoe UI Semibold", "Segoe UI", "Inter",
            "Helvetica Neue", "Arial");
    private static final String NUMBER_FAMILY = resolve("Cascadia Mono", "Consolas", "JetBrains Mono",
            "SF Mono", "Menlo", Font.MONOSPACED);

    private Type() {}

    /** Regular text at the requested size. */
    public static Font text(float size) { return new Font(TEXT_FAMILY, Font.PLAIN, 1).deriveFont(size); }

    /** Emphasised text, using a semibold family when the platform provides one. */
    public static Font strong(float size) {
        int style = STRONG_FAMILY.endsWith("Semibold") ? Font.PLAIN : Font.BOLD;
        return new Font(STRONG_FAMILY, style, 1).deriveFont(size);
    }

    /** Bold text for the few places that must outweigh {@link #strong(float)}. */
    public static Font bold(float size) { return new Font(TEXT_FAMILY, Font.BOLD, 1).deriveFont(size); }

    /** Even-width figures for measurements, timestamps and table numbers. */
    public static Font number(float size) { return new Font(NUMBER_FAMILY, Font.PLAIN, 1).deriveFont(size); }

    /** Emphasised even-width figures, used for headline measurements. */
    public static Font numberStrong(float size) { return new Font(NUMBER_FAMILY, Font.BOLD, 1).deriveFont(size); }

    /** Tracked uppercase caption used above values and inside card headers. */
    public static Font caption() {
        return strong(CAPTION).deriveFont(Map.of(TextAttribute.TRACKING, CAPTION_TRACKING));
    }

    /** Returns the resolved text family, so tests and diagnostics can report it. */
    public static String textFamily() { return TEXT_FAMILY; }

    /** Returns the resolved measurement family. */
    public static String numberFamily() { return NUMBER_FAMILY; }

    private static String resolve(String... preferred) {
        List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        return Arrays.stream(preferred).filter(installed::contains).findFirst().orElse(Font.SANS_SERIF);
    }
}
