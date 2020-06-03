package edu.asupoly.ser422.grocery;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import edu.asupoly.ser422.renderers.MyLab1Renderer;
import edu.asupoly.ser422.renderers.MyLab1RendererFactory;
import javafx.util.Pair;

@SuppressWarnings("serial")
public class GroceryListChangeServlet extends HttpServlet {
	private static Logger log = Logger.getLogger(GroceryListChangeServlet.class.getName());
	private static String _filename = null;
	private static String _outFileName = null;
	private static String _refererURL = null;
	
	/**
       Method to initialize servlet behaviour, environment and config.
       @param config. First parameter. Servlet config.
       @return void.
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		_filename = config.getInitParameter(Constants.INIT_FILENAME_PROP);
		log.info("_filename: " + _filename);
		if (_filename == null || _filename.length() == 0) {
			throw new ServletException();
		} else {
			_outFileName = getServletContext().getRealPath("/WEB-INF/classes/" + _filename);
			log.info("_outFileName: " + _outFileName);
		}
		_refererURL = ServletHelper.getResourcePath(Constants.LANDING_PAGE, getServletContext());
		
		log.info("refererURL: " + _refererURL);
	}
	
	
	/**
    This method handles the 'PUT' HTTP requests.

    @param request. First parameter, represents the HTTP request to get the resource.
    @param response. Second parameter, represents the server's response.
    @return void.
	 */
	public void doPut(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException	{
		PrintWriter out = response.getWriter();
		// we need 3 things to to compose our response: a code, a payload, and a content-type header
		int responseCode = HttpServletResponse.SC_CREATED;  // by default assuming success
		ArrayList<String> responsePayload = new ArrayList<String>();
		String responseContentType = ServletHelper.getResponseContentType(request.getHeader("Accept"));
//		String apiVersion = null;
//		apiVersion = (String)request.getHeader("version");
//
//		if (apiVersion.isEmpty() || apiVersion.equals("v1")) {
//			out.println("reached");
//		}
		MyLab1Renderer renderer = null;
		
		// Step 1. Process request headers. In this case Task 3 asks for an Accept header
		if (responseContentType == null ){
			log.info("Adder: No content type!");
			// didn't find a type we could use as a return type in the Accept header, return a 406
			responseCode = HttpServletResponse.SC_NOT_ACCEPTABLE;
			responsePayload.add("This application understands " + Constants.CONTENT_HTML + ", " + Constants.CONTENT_TEXT + ", or " + Constants.CONTENT_JSON);
			// Set the response content type to HTML and pass back as such
			responseContentType = Constants.CONTENT_HTML;
			try {
				renderer = MyLab1RendererFactory.getRenderer(responseContentType, _refererURL);
			} catch (Throwable t) {
				// we have a big problem. We don't know how to render.
				throw new ServletException(t.getMessage());
			}
			response.setStatus(responseCode);
			renderer.renderResponse(responsePayload, response.getWriter());
			return;
		}
		
		// Setting this up now. Once we know a response type we can use a renderer for it, even for errors.
		response.setContentType(responseContentType);
		try {
			renderer = MyLab1RendererFactory.getRenderer(responseContentType, _refererURL);	
		} catch (Throwable t) {
			// we have a big problem. We don't know how to render. Barf
			throw new ServletException(t.getMessage());
		}
		
		// Step 2. Process request parameters and request payload
		// Step 3. Perform processing (business logic). This could be refactored elsewhere
//		String requestData = request.getReader().lines().collect(Collectors.joining());
		String requestData = request.getReader().lines().collect(Collectors.joining());
		
		JSONObject obj = new JSONObject(requestData);
//		JSONArray groceryItemList = new JSONArray();
		GroceryItem groceryItem = null;
		try {
				groceryItem = GroceryItem.getGroceryItemObjFromBlueprint(obj);
		} catch (MyHttpException ex) {
			log.info("Adder: exception trying to get a grocery item from blueprint");
			responseCode = ex.getResponseCode();
			responsePayload.add(ex.getMessage());
			response.setStatus(responseCode);
			renderer.renderResponse(responsePayload, response.getWriter());
			return;
		}		
		// Step 4. Assemble response payload
		
		// At this point the groceryItem has to be valid so we can safely move forward and
		// set up the grocery list from the persistent store and then add to it. Put these
		// together in code as we need to synchronize the read then write to prevent lost updates
		// XXX synch block needed
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(_filename);
		Pair<Pair<Boolean, String>, GroceryList> loadBundle = ServletHelper.loadBootstrapFile(is);
		Pair<Boolean, String> loadStatus = loadBundle.getKey();
		
		
		
		GroceryList groceryListObj = loadBundle.getValue();
		Boolean hasErrored = loadStatus.getKey();
		is.close();
		if (hasErrored) {
			log.info("Adder: Error trying to read in the json input file");
			responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			responsePayload.add(loadStatus.getValue());
			response.setStatus(responseCode);
			renderer.renderResponse(responsePayload, response.getWriter());
			return;
		}
		
		
		String id = ServletHelper.retrieveID(request);
		Map<String,GroceryItem> groceryList=groceryListObj.getGroceryList();
		if(groceryList.containsKey(id)) {
			groceryItem.setId(id);
			groceryList.put(id, groceryItem);
			groceryListObj.addToGroceryList(id, groceryItem, _outFileName);
			responsePayload.add("Successfully updated: " + groceryItem.getId() + " to the list");
		}
		else {
			responseCode = HttpServletResponse.SC_BAD_REQUEST;
			responsePayload.add("Id mentioned: " + groceryItem.getId() + "is incorrect");
		}
		
		// Step 5. Set response headers (if not done so already)
		response.setStatus(responseCode);
		// content type set at top so any rendering done for errors has the right header
		
		// Step 6. Write out results. At this point we should know our our content type, our response code, and our payload.
		renderer.renderResponse(responsePayload, response.getWriter());
	}
	
	/**
    This method handles the 'DELETE' HTTP requests.

    @param request. First parameter, represents the HTTP request to get the resource.
    @param response. Second parameter, represents the server's response.
    @return void.
	 */
	public void doDelete(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException	{
		PrintWriter out = response.getWriter();
		// we need 3 things to to compose our response: a code, a payload, and a content-type header
		int responseCode = HttpServletResponse.SC_CREATED;  // by default assuming success
		ArrayList<String> responsePayload = new ArrayList<String>();
		String responseContentType = ServletHelper.getResponseContentType(request.getHeader("Accept"));
//		String apiVersion = null;
//		apiVersion = (String)request.getHeader("version");
//
//		if (apiVersion.isEmpty() || apiVersion.equals("v1")) {
//			out.println("reached");
//		}
		MyLab1Renderer renderer = null;
		
		// Step 1. Process request headers. In this case Task 3 asks for an Accept header
		if (responseContentType == null ){
			log.info("Adder: No content type!");
			// didn't find a type we could use as a return type in the Accept header, return a 406
			responseCode = HttpServletResponse.SC_NOT_ACCEPTABLE;
			responsePayload.add("This application understands " + Constants.CONTENT_HTML + ", " + Constants.CONTENT_TEXT + ", or " + Constants.CONTENT_JSON);
			// Set the response content type to HTML and pass back as such
			responseContentType = Constants.CONTENT_HTML;
			try {
				renderer = MyLab1RendererFactory.getRenderer(responseContentType, _refererURL);
			} catch (Throwable t) {
				// we have a big problem. We don't know how to render.
				throw new ServletException(t.getMessage());
			}
			response.setStatus(responseCode);
			renderer.renderResponse(responsePayload, response.getWriter());
			return;
		}
		
		// Setting this up now. Once we know a response type we can use a renderer for it, even for errors.
		response.setContentType(responseContentType);
		try {
			renderer = MyLab1RendererFactory.getRenderer(responseContentType, _refererURL);	
		} catch (Throwable t) {
			// we have a big problem. We don't know how to render. Barf
			throw new ServletException(t.getMessage());
		}
		
		// Step 2. Process request parameters and request payload
		// Step 3. Perform processing (business logic). This could be refactored elsewhere
	
		// Step 4. Assemble response payload
		// At this point the groceryItem has to be valid so we can safely move forward and
		// set up the grocery list from the persistent store and then add to it. Put these
		// together in code as we need to synchronize the read then write to prevent lost updates
		// XXX synch block needed
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(_filename);
		Pair<Pair<Boolean, String>, GroceryList> loadBundle = ServletHelper.loadBootstrapFile(is);
		Pair<Boolean, String> loadStatus = loadBundle.getKey();
		
		
		
		GroceryList groceryListObj = loadBundle.getValue();
		Boolean hasErrored = loadStatus.getKey();
		is.close();
		if (hasErrored) {
			log.info("Adder: Error trying to read in the json input file");
			responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			responsePayload.add(loadStatus.getValue());
			response.setStatus(responseCode);
			renderer.renderResponse(responsePayload, response.getWriter());
			return;
		}
		
		String id = ServletHelper.retrieveID(request);
		Map<String,GroceryItem> groceryList=groceryListObj.getGroceryList();
		if(groceryList.containsKey(id)) {
			groceryList.remove(id);
			groceryListObj.saveToFile(groceryList, _outFileName);
			responsePayload.add("Successfully deleted product id: " + id + " from the list");
		}
		else {
			responseCode = HttpServletResponse.SC_BAD_REQUEST;
			responsePayload.add("Id mentioned: " + id + "is incorrect");
		}

		// Step 5. Set response headers (if not done so already)
		response.setStatus(responseCode);
		// content type set at top so any rendering done for errors has the right header
		
		// Step 6. Write out results. At this point we should know our our content type, our response code, and our payload.
		renderer.renderResponse(responsePayload, response.getWriter());
	}

	/**
	This method handles the 'GET' HTTP requests to the '/groceries' URL.

	@param request. First parameter, represents the HTTP request to get the resource.
	@param response. Second parameter, represents the server's response.
	@return void.
 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "POST not supported by this servlet");
	}
	
	
	/**
	This method handles the 'POST' HTTP requests to the '/groceries' URL.

	@param request. First parameter, represents the HTTP request to get the resource.
	@param response. Second parameter, represents the server's response.
	@return void.
 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "POST not supported by this servlet");
	}

	/**
	This method handles the 'Patch' HTTP requests to the '/groceries' URL.

	@param request. First parameter, represents the HTTP request to get the resource.
	@param response. Second parameter, represents the server's response.
	@return void.
 */
	public void doPatch(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "POST not supported by this servlet");
	}		
	
}
