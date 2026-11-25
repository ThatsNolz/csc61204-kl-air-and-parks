package cityair.view.design;

/**
 * Non-colour design tokens: one spacing scale, one radius scale, fixed control heights and the
 * motion durations used by every animation. Screens compose these constants instead of inventing
 * their own numbers, which keeps rhythm and proportion consistent across the application.
 */
public final class Metrics {
    /** 4 px. Gap between a label and the value it describes. */
    public static final int SPACE_1 = 4;
    /** 8 px. Gap inside a compact control. */
    public static final int SPACE_2 = 8;
    /** 12 px. Default gap between related components. */
    public static final int SPACE_3 = 12;
    /** 16 px. Padding inside a compact card. */
    public static final int SPACE_4 = 16;
    /** 20 px. Padding inside a standard card. */
    public static final int SPACE_5 = 20;
    /** 24 px. Gap between cards. */
    public static final int SPACE_6 = 24;
    /** 32 px. Gap between the sections of a screen. */
    public static final int SPACE_7 = 32;
    /** 40 px. Outer padding of a screen. */
    public static final int SPACE_8 = 40;

    /** Corner radius of chips, badges and small indicators. */
    public static final int RADIUS_SMALL = 7;
    /** Corner radius of buttons, inputs and list rows. */
    public static final int RADIUS_CONTROL = 10;
    /** Corner radius of cards. */
    public static final int RADIUS_CARD = 16;
    /** Corner radius of the hero surfaces that anchor a screen. */
    public static final int RADIUS_HERO = 22;
    /** Radius large enough to render any supported height as a pill. */
    public static final int RADIUS_PILL = 999;

    /** Height of buttons, combo boxes and single-line inputs. */
    public static final int CONTROL_HEIGHT = 38;
    /** Height of a compact control used inside dense toolbars. */
    public static final int CONTROL_HEIGHT_COMPACT = 30;
    /** Side of a square icon button. */
    public static final int ICON_BUTTON = 36;
    /** Height of one table row. */
    public static final int ROW_HEIGHT = 34;
    /** Height of a table header. */
    public static final int HEADER_HEIGHT = 36;
    /** Width of the expanded navigation rail. */
    public static final int RAIL_WIDTH = 236;
    /** Width of the navigation rail when collapsed to icons. */
    public static final int RAIL_WIDTH_COLLAPSED = 68;
    /** Height of the application top bar. */
    public static final int TOP_BAR_HEIGHT = 68;
    /** Height of the status bar. */
    public static final int STATUS_BAR_HEIGHT = 40;

    /** Hairline used for borders and grid lines. */
    public static final float STROKE_HAIRLINE = 1f;
    /** Stroke of emphasised outlines and focus rings. */
    public static final float STROKE_EMPHASIS = 2f;
    /** Stroke of a plotted data series. */
    public static final float STROKE_SERIES = 2.4f;

    /** Pointer feedback, in milliseconds. */
    public static final int MOTION_FAST = 130;
    /** Selection and state changes, in milliseconds. */
    public static final int MOTION_BASE = 200;
    /** Content entrances, in milliseconds. */
    public static final int MOTION_SLOW = 320;
    /** Data visualisation transitions, in milliseconds. */
    public static final int MOTION_DATA = 620;
    /** Frame interval of every animation, giving about 60 frames per second. */
    public static final int FRAME_MILLIS = 16;

    /** Smallest usable window size; the shell enforces it so no screen can be clipped. */
    public static final int MIN_WINDOW_WIDTH = 980;
    /** Smallest usable window height. */
    public static final int MIN_WINDOW_HEIGHT = 660;
    /** Preferred window width on first launch. */
    public static final int WINDOW_WIDTH = 1340;
    /** Preferred window height on first launch. */
    public static final int WINDOW_HEIGHT = 860;
    /** Window width below which the navigation rail collapses to icons automatically. */
    public static final int RAIL_COLLAPSE_WIDTH = 1140;
    /** Window width below which multi-column sections stack vertically. */
    public static final int STACK_WIDTH = 1040;

    private Metrics() {}
}
