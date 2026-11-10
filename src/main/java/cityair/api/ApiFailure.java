package cityair.api;

/** Typed failure with an end-user-safe message. */
public final class ApiFailure extends Exception {
    /** Categories retained for logs and integration evidence. */
    public enum Kind { NOT_FOUND, SERVER, RATE_LIMIT, HTTP, TIMEOUT, NETWORK, INVALID }
    private final Kind kind;

    /** Builds a failure with a safe message and optional technical cause. */
    public ApiFailure(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    /** Returns the reason code for this failure. */
    public Kind kind() { return kind; }
}
