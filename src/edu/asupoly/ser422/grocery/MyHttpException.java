/**
 * 
 */
package edu.asupoly.ser422.grocery;

/**
 * @author kevinagary
 *
 */
class MyHttpException extends Exception {

	private static final long serialVersionUID = 1L;
	protected int _httpResponseCode;
	
	public MyHttpException(int code, String message) {
		super(message);
		_httpResponseCode = code;
	}
	public MyHttpException(int code, String message, Throwable cause) {
		super(message, cause);
		_httpResponseCode = code;
	}
	public MyHttpException(int code, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		_httpResponseCode = code;
	}

	public int getResponseCode() {
		return _httpResponseCode;
	}
}
