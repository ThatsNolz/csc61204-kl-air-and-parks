package cityair.view.design;

/**
 * Implemented by a component that keeps colour state outside its paint method, such as a Swing
 * control whose foreground is set once at construction.
 *
 * <p>Custom Aurora painting reads {@link Skin} while painting, so a repaint is enough for it. This
 * contract covers the rest: {@link Skin#refresh(java.awt.Component)} walks the component tree after
 * a theme change and calls {@link #applyTheme()} on every implementor it finds, which keeps theme
 * switching free of listener registration and the leaks that come with it.</p>
 */
public interface Themed {
    /** Re-reads every colour this component stores from the active theme. */
    void applyTheme();
}
