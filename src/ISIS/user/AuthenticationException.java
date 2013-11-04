/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ISIS.user;

/**
 *
 * @author michaelm
 */
public class AuthenticationException extends Exception {

    public enum exceptionType {

	USERNAME, PASSWORD, ACTIVE, OTHER
    }
    public exceptionType type;

    public AuthenticationException(String message, exceptionType type) {
	super(message);
	this.type = type;
    }
}