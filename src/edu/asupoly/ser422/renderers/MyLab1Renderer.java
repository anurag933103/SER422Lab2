package edu.asupoly.ser422.renderers;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.asupoly.ser422.grocery.Constants;

/**
 * This is the Strategy pattern. Implementations must determine how to render based on contentType
 * @return
 */
public interface MyLab1Renderer {
		public void renderResponse(List<String> whatToRender, PrintWriter out);
		public void renderResponse(JSONObject JSONToRender, PrintWriter out);
}

// I generally hate to embedd multiple classes in a file but this will help you see the difference in strategy implementations
class HTMLRenderer implements MyLab1Renderer {
	private static final String headHTML = "<html>\n<head>\n<title>Grocery List</title>\n</head>\n<body>\n";
	private static final String endHTML  = "\n</body>\n</html>";
	private String _url = null;
	
	@Override
	public void renderResponse(List<String> whatToRender, PrintWriter out) {
		out.println(headHTML);  // yeah a templating engine would clean this up, but who cares about aesthetics on Lab 1?
		// We really don't know what is in "whatTorender" other than a list of compiled content
		// So unintelligently write it out in a sequence of p tags
		for (String s : whatToRender) {
			out.println("<p>\n" + s + "\n</p>\n");
		}
		if(_url != null && !_url.isEmpty()){
			out.println("<a href=\""+_url+"\">Add More</a></br>");
		}else{
			out.println("<p>Something went wrong! Referer URL not found</p></br>");
		}
		out.println(endHTML);
		out.flush();
		out.close();
	}
	
	protected HTMLRenderer(String url) {
		_url = url;
	}

	@Override
	public void renderResponse(JSONObject JSONToRender, PrintWriter out) {
		String responseString = HTMLRenderer.jsonToEmbeddedText(JSONToRender);
		responseString = responseString.replaceAll("<NEWLINE>", "");
		responseString = responseString.replaceAll("<TAB>", "");
		out.println(responseString);
		out.flush();
		out.close();
	}

	/**
	Create a token embedded String representation of the JSON respone.
	This embedded representation is used by HTML and Text renderers
	@param respone. First parameter. The JSON respone for the get request.
	@return String. The embedded string representation of the JSON response.
	 */
	static String jsonToEmbeddedText(JSONObject response) {
		StringBuilder responseString = new StringBuilder();
		responseString.append(headHTML);

		Boolean hasErrored = (Boolean) response.get(Constants.JR_ERROR);
		if(hasErrored) {
			responseString.append("<span>" + response.get(Constants.JR_ERROR_MSG) + "</span></br><NEWLINE>");
			responseString.append("</br><NEWLINE>");
		} else {
			try {
				JSONArray filterMessages = (JSONArray) response.get(Constants.JR_FILTER_MESSAGES);
				for(int i = 0; filterMessages != null && i < filterMessages.length(); i++){
					responseString.append("<span>" + filterMessages.get(i) + "</span></br><NEWLINE>");
				}
				responseString.append("</br><NEWLINE>");
			} catch (JSONException ex) {
				// we swallow because we leave it null to skip the next processing loop
			}

			try {
				JSONObject groceryValues = (JSONObject) response.get(Constants.JR_GROCERY_LIST);
				JSONArray groceryValuesHeaders = (JSONArray) groceryValues.get(Constants.JR_GROCERY_LIST_HEADERS);
				responseString.append("<table>");
				responseString.append("<tr>");
				for(int i = 0; groceryValuesHeaders != null && i < groceryValuesHeaders.length(); i++){				
					responseString.append("<th>" + groceryValuesHeaders.get(i) + "</th><TAB>");
				}
				responseString.append("</tr><NEWLINE>");
				JSONArray groceryValuesRows = (JSONArray) groceryValues.get(Constants.JR_GROCERY_LIST_ROWS);
				for(int i = 0; groceryValuesRows != null && i < groceryValuesRows.length(); i++){
					responseString.append("<tr>");
					JSONArray groceryValuesRowValues = (JSONArray) groceryValuesRows.get(i);
					for (int j = 0; groceryValuesRowValues != null && j < groceryValuesRowValues.length(); j++){
						responseString.append("<td>" + groceryValuesRowValues.get(j) + "</td><TAB>");
					}
					responseString.append("</tr><NEWLINE>");
				}	
				responseString.append("</table><NEWLINE>");
			} catch (JSONException ex) {
				// we swallow because we leave it null to skip the next processing loop
			}
		}
		try {
			responseString.append("<span>Add More: <a href=\"" + response.get(Constants.JR_HOME) + "\">here</a></span><NEWLINE>");
		} catch (JSONException ex) {
			// put in a defualt message
			responseString.append("<span>Unable to get referrer, return home: <a href=\"" + "/" + "\">here</a></span><NEWLINE>");
		}
		responseString.append(endHTML);
		return responseString.toString();
	}
}

class TextRenderer implements MyLab1Renderer {
	private String _url = null;
	
	@Override
	public void renderResponse(List<String> whatToRender, PrintWriter out) {
		for (String s : whatToRender) {
			out.println("\n" + s + "\n");
		}
		if (_url != null && !_url.isEmpty()) {
			out.println("Return to the form by copy pasting this into your address bar: " + _url);
		} else{
			out.println("Something went wrong! Referer URL not found");
		}
		out.flush();
		out.close();
	}
	
	protected TextRenderer(String url) {
		_url = url;
	}
	
	/**
	Replace the specials token in the embedded string response to get a 
	pure text respone.
	@param respone. First parameter. The JSON respone for the get request.
	@return String. The plain text string representation of the response.
	 */
	public void renderResponse(JSONObject response, PrintWriter out) {
		String responseString = HTMLRenderer.jsonToEmbeddedText(response);
		List<String> htmlTokens = Arrays.asList("html", "body", "head", "title", "table", "tr", "th", "td", "br", "span", "p");
		for(String token: htmlTokens){
			responseString = responseString.replaceAll("<" + token + ">", "");
			responseString = responseString.replaceAll("</" + token + ">", "");
		}
		// special case for 'a' tag
		responseString = responseString.replaceAll("<a href=\"", "");
		responseString = responseString.replaceAll("\">here</a>", "");

		// replace newlines and tabs
		responseString = responseString.replaceAll("<NEWLINE>", "\n");
		responseString = responseString.replaceAll("<TAB>", "\t");
		out.println(responseString);
		out.flush();
		out.close();
	}
}
class JSONRenderer implements MyLab1Renderer {
	private String _url = null;
	
	@Override
	public void renderResponse(List<String> whatToRender, PrintWriter out) {
		// all we have is a list of strings to embed as a simple JSON array
		out.print("[ \"landing page" + _url + "\"");
		for (String s : whatToRender) {
			out.print(",");
			out.println("\"" + s + "\"");
		}
		out.println(" ]");
		
		out.flush();
		out.close();
	}
	protected JSONRenderer(String url) {
		_url = url;
	}
	
	@Override
	public void renderResponse(JSONObject JSONToRender, PrintWriter out) {
		out.println(JSONToRender.toString());
		out.flush();
		out.close();
	}
}