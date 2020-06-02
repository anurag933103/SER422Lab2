package edu.asupoly.ser422.grocery;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javafx.util.Pair;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.asupoly.ser422.renderers.MyLab1Renderer;
import edu.asupoly.ser422.renderers.MyLab1RendererFactory;

import java.util.logging.Logger;


/** 
	Servlet for handling CRUD operations on the "/groceries" resource.
 */
@SuppressWarnings("serial")
public class GroceryListViewerServlet extends HttpServlet {
	private static Logger log = Logger.getLogger(GroceryListViewerServlet.class.getName());
	private static String _filename = null;
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
		}
		_refererURL = ServletHelper.getResourcePath(Constants.LANDING_PAGE, getServletContext());	
		log.info("refererURL: " + _refererURL);
	}

	/**
		This method handles the 'GET' HTTP requests.
		@param request. First parameter, represents the HTTP request to get the resource.
		@param response. Seond parameter, represents the server's response.
		@return void.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException	{

		int responseCode = HttpServletResponse.SC_OK;  // by default assuming success
		String responseContentType = ServletHelper.getResponseContentType(request.getHeader("Accept"));
		MyLab1Renderer renderer = null;
		Boolean hasErrored = Boolean.FALSE; // assume no errors ;)
		JSONObject responseJSON = new JSONObject(); // by default we will build JSON here
		// Note if we get an error case we blow this object away and use a new one to encapsulate the error

		// This needs to be there always
		responseJSON.put(Constants.JR_HOME, _refererURL);
		
		// Step 1. Process request headers. In this case Task 3 asks for an Accept header
		if (responseContentType == null) {
			log.info("Viewer: No content type!");
			// didn't find a type we could use as a return type in the Accept header, return a 406
			responseCode = HttpServletResponse.SC_NOT_ACCEPTABLE;
			responseJSON = new JSONObject();
			responseJSON.put(Constants.JR_ERROR, Boolean.TRUE);
			responseJSON.put(Constants.JR_ERROR_MSG,
							 "This application understands " + Constants.CONTENT_HTML + ", " + Constants.CONTENT_TEXT + ", or " + Constants.CONTENT_JSON);
			// Set the response content type to HTML and pass back as such
			responseContentType = Constants.CONTENT_HTML;
			try {
				renderer = MyLab1RendererFactory.getRenderer(responseContentType, _refererURL);
			} catch (Throwable t) {
				// we have a big problem. We don't know how to render. Barf
				throw new ServletException(t.getMessage());
			}
			response.setStatus(responseCode);
			renderer.renderResponse(responseJSON, response.getWriter());
			return;
		}
		log.info("Viewer: Content type is " + responseContentType);

		// Setting this up now. Once we know a response type we can use a renderer for it, even for errors.
		response.setContentType(responseContentType);
		try {
			renderer = MyLab1RendererFactory.getRenderer(responseContentType, _refererURL);	
		} catch (Throwable t) {
			// we have a big problem. We don't know how to render. Barf
			throw new ServletException(t.getMessage());
		}
		
		// load the grocery list from the JSON file
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(_filename);
		Pair<Pair<Boolean, String>, GroceryList> loadBundle = ServletHelper.loadBootstrapFile(is);
		Pair<Boolean, String> loadStatus = loadBundle.getKey();
		GroceryList groceryListObj = loadBundle.getValue();
		is.close();
		// if no error occured during the load
		hasErrored = loadStatus.getKey();

		if (hasErrored) {
			log.info("Viewer: Error trying to read in the json input file");
			responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			responseJSON = new JSONObject();
			responseJSON.put(Constants.JR_ERROR, hasErrored);
			responseJSON.put(Constants.JR_ERROR_MSG, loadStatus.getValue());
			response.setStatus(responseCode);
			renderer.renderResponse(responseJSON, response.getWriter());
			return;
		}

		// default message to be displayed
		String message = "No filters applied on grocery list.";
		log.info("request parameter test for id :" + request.getParameter("id"));

		List<String> filterMessages = new ArrayList<String>();
		Map<String, GroceryItem> groceryList = groceryListObj.getGroceryList();

		// Step 2. Process request parameters and request payload
		// Step 3. Perform processing (business logic). This could be refactored elsewhere
		Enumeration<String> parameters = request.getParameterNames();
		// filter grocery list
		try {
			while(parameters.hasMoreElements()){
				String itemName = (String)parameters.nextElement();
				log.info("item name for filter is :" + itemName);
				String itemValue = request.getParameter(itemName);
				if(!itemValue.equals("")) {
					Pair <String, Map<String, GroceryItem> > pairObj = filterGroceryList(groceryList, itemName, itemValue);
					filterMessages.add(pairObj.getKey());
					groceryList = pairObj.getValue();	
				}
			}
		} catch (MyHttpException ex) {
			log.info("Viewer: exception trying to filter a grocery list");
			responseCode = ex.getResponseCode();
			responseJSON = new JSONObject();
			responseJSON.put(Constants.JR_ERROR, Boolean.TRUE);
			responseJSON.put(Constants.JR_ERROR_MSG, ex.getMessage());
			response.setStatus(responseCode);
			renderer.renderResponse(responseJSON, response.getWriter());
			return;
		}

		// Step 4. Assemble response payload
		if(filterMessages.isEmpty()){	
			filterMessages.add(message);
		} 
		// Use the declared json object as we are good
		for(String filterMessage: filterMessages) {
			responseJSON.append(Constants.JR_FILTER_MESSAGES, filterMessage);
		}
		responseJSON.put(Constants.JR_GROCERY_LIST, getGroceryListJSON(groceryList));
		responseJSON.put(Constants.JR_ERROR, hasErrored);
		responseJSON.put(Constants.JR_ERROR_MSG, "No error messages");
		
		// Step 5. Set response headers (if not done so already)
		// render content based on the requested content type
		response.setStatus(responseCode);
		// content type set at top so any rendering done for errors has the right header
		
		// Step 6. Write out results. At this point we should know our our content type, our response code, and our payload.
		renderer.renderResponse(responseJSON, response.getWriter());
	}

	/**
		This method handles the 'POST' HTTP requests to the '/groceries' URL.

		@param request. First parameter, represents the HTTP request to get the resource.
		@param response. Seond parameter, represents the server's response.
		@return void.
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "POST not supported by this servlet");
	}

	/**
		Filter the grocery list based on the filters provided.
		@param groceryList. First parameter. A map comtaining grocery items mapped to their names.
		@param filterName. Second parameter. String name of the filter.
		@param filterValue. Third parameter. String value of the filter.
		@return Pair<String, Map<String, GroceryItem> > A 'Pair' containg status message for the operation and the 
		filtered grocery-list.
	 */
	public Pair<String, Map<String, GroceryItem> > filterGroceryList(Map<String, GroceryItem> groceryList, String filterName, String filterValue)
			throws BadParameterException {

		Map<String, GroceryItem> filteredGroceryList = new Hashtable<String, GroceryItem>();
		String message = "Successfully filtered on: " + filterName + " for value: " + filterValue;

		for(Map.Entry<String, GroceryItem> entry : groceryList.entrySet()){
			GroceryItem groceryItem = entry.getValue();
			if(filterName.equals(Constants.AISLE_KEY)){
				int filterValueInt = 0;
				try {
					filterValueInt = Integer.parseInt(filterValue);
				} catch (NumberFormatException e) {
					throw new BadParameterException("Aisle value must be an integer!");
				}
				if(filterValueInt < 0)
					throw new BadParameterException("Aisle value cannot be negative!");
				if(groceryItem.getAisle() == filterValueInt){
					filteredGroceryList.put(entry.getKey(), groceryItem);
				}
			} else if (filterName.equals(Constants.CUSTOM_KEY)){
				if (groceryItem.getCustom().indexOf(filterValue) != -1){
					filteredGroceryList.put(entry.getKey(), groceryItem);
				}
			}
		}

		return new Pair<String, Map<String, GroceryItem> >(message, filteredGroceryList);
	}

	/**
		Get a list grocery items as a JSON Object
		@param groceryList. First parameter. A map comtaining grocery items mapped to their names.
		@return JSONObject. The object containing the grocery list
	 */
	public JSONObject getGroceryListJSON(Map<String, GroceryItem> groceryList){
		JSONObject groceryValues = new JSONObject();

		JSONArray headers = new JSONArray();
		headers.put("Product Name");
		headers.put("Brand Name");
		headers.put("Aisle Number");
		headers.put("Quantity");
		headers.put("Diet Type");

		groceryValues.put(Constants.JR_GROCERY_LIST_HEADERS, headers);

		JSONArray rows = new JSONArray();
		if(groceryList.size() == 0){
			JSONArray data = new JSONArray();
			data.put("No Items!");
			rows.put(data);
		} else{
			for(Map.Entry<String, GroceryItem> entry : groceryList.entrySet()){
				GroceryItem groceryItem = entry.getValue();
				JSONArray data = new JSONArray();
				data.put(groceryItem.getPname());
				data.put(groceryItem.getBname());
				data.put(groceryItem.translateToString(groceryItem.getAisle()));
				data.put(Integer.toString(groceryItem.getQty()));
				data.put(groceryItem.getCustom());
				rows.put(data);
			}
		}
		groceryValues.put(Constants.JR_GROCERY_LIST_ROWS, rows);
		return groceryValues;
	}
}



