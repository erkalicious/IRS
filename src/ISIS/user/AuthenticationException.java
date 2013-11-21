package ISIS.user;

/**
 * @author michaelm
 */
public class AuthenticationException extends Exception {
	public enum exceptionType {
		
		USERNAME, PASSWORD, ACTIVE, OTHER
	}
	
	private static final long	serialVersionUID	= 1L;
	
	public exceptionType		type;
	
	public AuthenticationException(String message, exceptionType type) {
		super(message);
		this.type = type;
	}
}
