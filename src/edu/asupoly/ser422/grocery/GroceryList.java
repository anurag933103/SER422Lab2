package edu.asupoly.ser422.grocery;

import java.util.*;
import java.util.logging.Logger;
import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.json.JSONException;

public class GroceryList {
	private Map<String, GroceryItem> groceryList = new Hashtable<String, GroceryItem>();
	private static Logger log = Logger.getLogger(GroceryList.class.getName());

	public String toString() {
		StringBuilder sb = new StringBuilder("Grocery List:\n");
		for(Map.Entry<String, GroceryItem> entry : groceryList.entrySet()) {
			sb.append("\tKey: " + entry.getKey() + "\tvalue: " + entry.getValue());
		}
		return sb.toString();
	}
	
	public Map<String, GroceryItem> getGroceryList(){
		return this.groceryList;
	}

	/**
		Add the given GroceryItem to the grocery list. The product name of the GroceryItem is
		provided as the key.
		@param key. The key for the grocery list mapping.
		@param groceryItem. The GroceryItem to be added.
		@param filename. File to be used (written to) to persist the added item.
		@return void
	 */
	public void addToGroceryList(String key, GroceryItem groceryItem, String filename){
		log.info("Adding product with " + key + " to grocery list: " + groceryItem);
		this.groceryList.put(key, groceryItem);
		try{
			this.saveToFile(this.groceryList, filename);
		} catch(Exception ex) {
			this.groceryList.remove(key);  // remove key is save to file fails;
		}
	}

	/**
		Returns the total number of items in the grocery list.
		@return int. the total number of items in the grocery list.
	 */
	public int getTotalItems(){
		return this.groceryList.size();
	}

	/**
		Load the grocery list from the provided stream.
		@param is. InputStream from which to read and load the data from.
		@return void.
	 */
	public void loadFromFile(InputStream is) throws MyServerException {
		JSONTokener jTokener = new JSONTokener(is);
		JSONArray jArray = null;
		try{
			jArray = new JSONArray(jTokener);

			for (int i = 0; i < jArray.length(); i ++){
				log.info("Processing JSON object from file: " + jArray.getJSONObject(i));
				GroceryItem groceryItem = GroceryItem.getGroceryItemObjFromBlueprint(jArray.getJSONObject(i));	
				this.groceryList.put(groceryItem.getId(), groceryItem);	
			}
		} catch (Throwable ex){
			log.info("loadFromFile threw a Throwable!");
			throw new MyServerException("InputStream might be empty!");
		}
	}

	/**
		Save the provided grocery list to the give file.
		@param groceryList. A mapping of product name to product details.
		@param filename. Name of the file to write the grocery list to.
		@return void.
	 */
	public void saveToFile(Map<String, GroceryItem> groceryList, String filename) throws IOException, JSONException {
		PrintWriter printWriter = new PrintWriter(new FileOutputStream(filename));
		JSONArray jArray = new JSONArray();
		log.info("Saving bootstrap file with: " + this.getTotalItems() + "records.");
		for(GroceryItem groceryItem: groceryList.values()){
			JSONObject jObject = groceryItem.getJSONObject();
			jArray.put(jObject);			
		}
		printWriter.print(jArray.toString());
		printWriter.close();
	}

	/**
		Convert the grocery list Map to a JSONArray object.
		@param groceryList. A mapping of product name to product details.
		@return JSONArray. The input mapping as a JSONArray object.
	 */
	@SuppressWarnings("unused")
	private JSONArray toJSONArray(Map<String, GroceryItem> groceryList) {
		JSONArray jArray = new JSONArray();
		for(Map.Entry<String, GroceryItem> entry : groceryList.entrySet()){
			JSONObject jObject = new JSONObject(entry.getValue());
			jArray.put(jObject);
		}
		return jArray;
	}
}