package cityair.view.component;

import cityair.view.design.Themed;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/**
 * Base class for the application's custom-painted components.
 *
 * <p>A bare {@link JComponent} has no accessible context, so a component that paints itself is
 * invisible to a screen reader until it supplies one. Every Aurora component that draws rather
 * than composes extends this class, which gives it a context, a role and the ability to carry a
 * name and a description. That is what lets a dial, a chart or a badge state its value in words
 * even though the value itself is painted.</p>
 */
public abstract class AuroraComponent extends JComponent implements Themed {
    /** Creates a transparent component that paints its own surface. */
    protected AuroraComponent() {
        setOpaque(false);
    }

    /**
     * The role assistive technology should report. The default suits a component that presents a
     * value; a component that can be operated should override it.
     */
    protected AccessibleRole role() { return AccessibleRole.LABEL; }

    /** Repaints the component, which is enough for anything that reads its colours while painting. */
    @Override public void applyTheme() { repaint(); }

    /** Returns this component's accessible context, creating it on first use. */
    @Override public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleAuroraComponent();
        return accessibleContext;
    }

    /** Reports the component's role, name and description to assistive technology. */
    private final class AccessibleAuroraComponent extends AccessibleJComponent {
        /** Returns the role assistive technology should report for this component. */
        @Override public AccessibleRole getAccessibleRole() { return role(); }
    }
}
