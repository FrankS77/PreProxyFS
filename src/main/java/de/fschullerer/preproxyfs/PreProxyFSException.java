package de.fschullerer.preproxyfs;

/**
 * This is a custom unchecked exception.
 *
 * @author Frank Schullerer
 */
public class PreProxyFSException extends RuntimeException {

    /** Serializable. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param message Message with exception.
     */
    public PreProxyFSException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message Message for exception.
     * @param exception The exception.
     */
    public PreProxyFSException(String message, Throwable exception) {
        super(message, exception);
    }
}
