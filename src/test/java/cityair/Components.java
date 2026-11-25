package cityair;

import java.awt.Component;
import java.awt.Container;

/**
 * Finds a component by the name the application gave it.
 *
 * <p>Every control the tests drive is named in the source for accessibility first; this helper
 * lets the tests and the evidence harnesses reach exactly the component a user would, without
 * depending on the order or nesting of the layout.</p>
 */
public final class Components {
    private Components() {}

    /** Returns the first component in the tree with this name, or null when there is none. */
    public static Component find(Component root, String name) {
        if (name.equals(root.getName())) return root;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = find(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Returns the named component, failing with a clear message when it is absent. */
    public static Component require(Component root, String name) {
        Component found = find(root, name);
        if (found == null) throw new AssertionError("No component named " + name);
        return found;
    }
}
