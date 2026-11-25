package cityair.strategy;

/**
 * The audiences offered in the view, each bound to its own {@link AdviceStrategy} object.
 * Adding an audience means adding one strategy class and one constant; callers are unchanged.
 */
public enum AdviceProfile {
    GENERAL("General public", new GeneralPublicAdvice()),
    CHILDREN("Children", new ChildrenAdvice()),
    OLDER_ADULTS("Older adults", new OlderAdultsAdvice());

    private final String label;
    private final AdviceStrategy strategy;

    AdviceProfile(String label, AdviceStrategy strategy) {
        this.label = label;
        this.strategy = strategy;
    }

    /** Returns the strategy for this audience. */
    public AdviceStrategy strategy() { return strategy; }

    /** Returns the label used by the view. */
    @Override public String toString() { return label; }
}
