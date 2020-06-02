package edu.asupoly.ser422.grocery;

import javax.servlet.http.HttpServletResponse;

/**
	Custom Exception class. To be used when an unkown key is provided in the JSON file on disk.
*/
@SuppressWarnings("serial")
public class BadParameterException extends MyHttpException { 
    public BadParameterException(String errorMessage) {
        super(HttpServletResponse.SC_BAD_REQUEST, errorMessage);
    }
}