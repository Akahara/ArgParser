package fr.wonder.argparser.utils;

/**
 * Unreachable exceptions may be used as a replacement for assertions.
 * 
 * <p>They differ from assertions only in their use cases.
 */
public class UnreachableException extends Error {

	private static final long serialVersionUID = 333946789414690579L;
	
	public UnreachableException() {
        super();
    }

    public UnreachableException(String message) {
        super(message);
    }

    public UnreachableException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnreachableException(Throwable cause) {
        super(cause);
    }

    protected UnreachableException(
				String message,
				Throwable cause,
                boolean enableSuppression,
                boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
