package cityair.view.design;

/**
 * Semantic colour roles of the Aurora design system. Components ask for a role, never for a
 * literal colour, so a theme can be redefined in one place and every screen follows.
 *
 * <p>Each {@link AuroraTheme} must define every role; {@code ThemeTest} enforces that.</p>
 */
public enum Tone {
    /** Window background at the top of its vertical gradient. */
    CANVAS,
    /** Window background at the bottom of its vertical gradient. */
    CANVAS_EDGE,
    /** Default card and panel fill. */
    SURFACE,
    /** Fill of a surface that sits above another surface, such as a popup or a hovered card. */
    SURFACE_RAISED,
    /** Recessed fill used for wells, table bodies and chart plots. */
    SURFACE_SUNKEN,
    /** Hairline border of cards, inputs and tables. */
    STROKE,
    /** Border used when an element must read as separate rather than merely enclosed. */
    STROKE_STRONG,
    /** Primary text. */
    INK,
    /** Secondary text: captions, units and supporting sentences. */
    INK_MUTED,
    /** Tertiary text: disabled labels and axis ticks. */
    INK_FAINT,
    /** Text drawn on a strongly coloured fill. */
    INK_INVERSE,
    /** Brand accent used for primary actions and the active selection. */
    ACCENT,
    /** Brand accent under the pointer. */
    ACCENT_HOVER,
    /** Brand accent while pressed. */
    ACCENT_PRESSED,
    /** Low-intensity accent wash for selected rows and quiet highlights. */
    ACCENT_SOFT,
    /** Text and icons drawn on {@link #ACCENT}. */
    ON_ACCENT,
    /** Keyboard focus ring. */
    FOCUS,
    /** Status colour for a good or improving condition. */
    POSITIVE,
    /** Status colour for a condition that deserves attention. */
    CAUTION,
    /** Status colour for a breached threshold or a failed operation. */
    DANGER,
    /** Low-intensity danger wash behind alert banners. */
    DANGER_SOFT,
    /** Status colour for neutral, informational context. */
    INFO,
    /** Navigation rail background at the top of its gradient. */
    RAIL,
    /** Navigation rail background at the bottom of its gradient. */
    RAIL_EDGE,
    /** Navigation rail text and icons. */
    RAIL_INK,
    /** Navigation rail text for inactive items and section labels. */
    RAIL_INK_MUTED,
    /** Fill behind the active navigation item. */
    RAIL_ACTIVE,
    /** Drop-shadow colour; its alpha is scaled per elevation level. */
    SHADOW,
    /** Atmospheric glow tint layered over the canvas. */
    GLOW,
    /** Placeholder fill of a skeleton loading block. */
    SKELETON,
    /** Chart grid lines and axis rules. */
    GRID,
    /** Tooltip and popover background. */
    TOOLTIP,
    /** Tooltip and popover text. */
    TOOLTIP_INK,
    /** Dimming layer painted behind a modal surface. */
    SCRIM
}
