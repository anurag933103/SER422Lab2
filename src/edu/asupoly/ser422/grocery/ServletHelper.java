
package edu.asupoly.ser422.grocery;

import java.net.MalformedURLException;
import javax.servlet.*;
import javax.servlet.http.*;
import javafx.util.Pair;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ServletHelper{
	private static Logger log = Logger.getLogger(ServletHelper.class.getName());
	
	/**
       Get the URL for the 'resource' paramter using the Servlet Context.
       @param resource. First parameter. String that represents the resource.
       @param servletContext. Second parameter. ServletContext object.
       @return String. Return the URL for the resource.
	 */
	public static String getResourcePath(String resource, ServletContext servletContext) {
		String refererURL = null;
		try {
			String contextPath = servletContext.getContextPath();
			String path = servletContext.getResource(resource).getPath();
			String[] pathList = path.split("/");
			String host = pathList[0];
			String[] resourceList = path.split(contextPath);
			String resourceName = resourceList[resourceList.length - 1];
			refererURL = host + contextPath + resourceName;
		} catch (MalformedURLException ex) {
			refererURL = null;
		}
		return refererURL;
	}
	
	/**
       Set content type of the response based on the Accept type.
       @param acceptTypeValue. Value of the Accept header in the HTTP request.
       @return String. String representation of the content type. Null if unacceptable type
	 */
	public static String getResponseContentType(String acceptTypeValue) {
		// If the header is not there use HTML as the default
		if (acceptTypeValue == null) {
			return Constants.CONTENT_HTML;
		} 
		// OK, if we have a value, we first check if it is multi-valued (separated by semicolons)
		// and load an ArrayList with the values
		String[] acceptTypes = acceptTypeValue.split(";");
		
		// What is allowed? CONTENT_HTML, CONTENT_JSON, CONTENT_TEXT, or wildcards, take what comes first
		boolean done = false;
		for (int i = 0; i < acceptTypes.length && !done; i++) {
			// we use indexOf instead of equals since the header value itself may be multivalued
			if (acceptTypes[i].indexOf(Constants.CONTENT_HTML) != -1 || 
					acceptTypes[i].indexOf("*/*") != -1 ||
					acceptTypes[i].indexOf("text/*") != -1) {
				return Constants.CONTENT_HTML;
			} else if (acceptTypes[i].indexOf(Constants.CONTENT_TEXT) != -1) {
				return Constants.CONTENT_TEXT;
			} else if (acceptTypes[i].indexOf(Constants.CONTENT_JSON) != -1 || 
					   acceptTypes[i].indexOf(Constants.CONTENT_JSON2) != -1) {
				return Constants.CONTENT_JSON;  // OK to convert text/json to application/json
			}
		}
		
		// if we haven't returened yet it means we processed all headers without a match, a problem
		return null;
	}

	/**
       Load the grocery list from the JSON file.
       @param is. First Parameter. The input file stream to load the data from.
       @return Pair<Pair<Boolean, String>, GroceryList>. Status of the load operation and error messgae string, if any along
       witht the newly created GroceryList object.
	 */
	public static Pair<Pair<Boolean, String>, GroceryList> loadBootstrapFile(InputStream is){
		String errorMessage = "";
		Boolean hasErrored = false;
		GroceryList groceryListObj = new GroceryList();
		try {
			groceryListObj.loadFromFile(is);
		} catch (Throwable ex) {
			errorMessage = "The server encountered the following error.\n" + ex.getMessage();
			hasErrored = true;
			log.info("Error loading bootstrap file: " + errorMessage);
		}
		Pair<Boolean, String> loadStatus = new Pair<Boolean, String> (hasErrored, errorMessage);
		return new Pair<Pair<Boolean, String>, GroceryList> (loadStatus, groceryListObj);
	}

	public static Map<String, String> getBlueprintFromRequest(HttpServletRequest request) {
		Enumeration<String> parameters = request.getParameterNames();
		Map<String, String> blueprint = new HashMap<String, String>();
		while(parameters.hasMoreElements()){
			String itemName = (String)parameters.nextElement();
			String itemValue = request.getParameter(itemName);

			blueprint.put(itemName, itemValue);
		}
		return blueprint;
	}
	
	public static String getUniqueId() {
		String uniqueID = UUID.randomUUID().toString();
		return uniqueID;
		
	}
}
