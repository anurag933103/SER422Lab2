package edu.asupoly.ser422.grocery;

import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class MyServerException extends MyHttpException {
	public MyServerException(String errorMessage) {
	        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
	}
}
