package edu.asupoly.ser422.grocery;

import java.util.*; 
import org.json.JSONObject;
import java.util.logging.Logger;

public class GroceryItem {
	private static Logger log = Logger.getLogger(GroceryItem.class.getName());
	int aisle = 999999;  // default value
	int qty;
	String id, pname, custom;
	String bname = " ";  // default value

	public String toString() {
		return "Grocery Item id: " + this.id + "Grocery Item name: " + this.pname + ", brand: " + this.bname + ", quantity: " + this.qty + ", aisle: " + this.aisle + ", custom: " + this.custom;
	}
	
	public void setAisle(int aisle){
		this.aisle = aisle;
	}
	public void setQty(int qty){
		this.qty = qty;
	}
	public void setPname(String pname){
		this.pname = pname;
	}
	public void setBname(String bname){
		this.bname = bname;
	}
	public void setCustom(String custom){
		this.custom = custom;
	}

	public int getAisle(){
		return this.aisle;
	}
	public String getPname(){
		return this.pname;
	}
	public String getBname(){
		return this.bname;
	}
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getQty(){
		return this.qty;
	}
	public String getCustom(){
		return this.custom;
	}

	public String translateToString(int val) {
		if(val == -1)
			return "N/A";
		return Integer.toString(val);
	}

	/**
		Blueprint of the Class object. A mapping of this classes variables and the form data on the landing (index.html) page.
		A separate instance is manufactured each time as this is manipulated in other methods.
		@return Map<String, Boolean>.
	 */
	public static Map<String, Boolean> getGroceryItemBlueprint(){
		Map<String, Boolean> blueprint = new HashMap<String, Boolean>();
		blueprint.put(Constants.AISLE_KEY, false);
		blueprint.put(Constants.BNAME_KEY, false);
		blueprint.put(Constants.QTY_KEY, true);
		blueprint.put(Constants.PNAME_KEY, true);
		blueprint.put(Constants.CUSTOM_KEY, true);
		blueprint.put(Constants.ID_KEY, false);
		return blueprint;
	}

	/**
		Create a GroceryItem object from the given JSONObject blueprint - a key-value mapping of the Class variables and their values.
	 */
	public static GroceryItem getGroceryItemObjFromBlueprint(JSONObject blueprint) throws BadParameterException  {
		Map<String, Boolean> blueprintReq = getGroceryItemBlueprint();
		Map<String, String[]> blueprintFinal = new HashMap<String, String[]>();
		String[] valArray = new String[1];

		for(Map.Entry<String, Boolean> entry : blueprintReq.entrySet()){
			String key = entry.getKey();
			if(blueprint.has(key)){
				Object object = blueprint.get(key);
				if (object instanceof Integer) {
					valArray[0] = Integer.toString((int) object);
				} else {
					valArray[0] = (String) object;
				}
				blueprintFinal.put(key, valArray);
				valArray = new String[1];
			}
		}
		return GroceryItem.getGroceryItemObjFromBlueprint(blueprintFinal);
	}

	/**
		Create a GroceryItem object from the given blueprint - a key-value mapping of the Class variables and their values.
	 */
	public static GroceryItem getGroceryItemObjFromBlueprint(Map<String, String[]> blueprint) throws BadParameterException {

		if (blueprint == null) {
			log.info("GroceryItem: No blueprint");
			throw new BadParameterException("No payload");
		}

		Map<String, Boolean> blueprintReq = getGroceryItemBlueprint();

		// check if all required keys are present with a value
		log.info("Checking for required keys");
		for(Map.Entry<String, Boolean> entry : blueprintReq.entrySet()){
			Boolean required = entry.getValue();
			String key = entry.getKey();
			log.info("Checking for key " + key);
			if(required && 
			   (!blueprint.containsKey(key) || blueprint.get(key) == null || ((String[])blueprint.get(key))[0].trim().length() ==0)) {
				log.info("Required key " + key + " not present or empty");
				log.info(((String[])blueprint.get(key))[0]);
				throw new BadParameterException("Required Key: '" + key + "' not present in sent blueprint");
			}
		}

		log.info("Checking for 'unexpected' keys");
		// check if any 'non-existent' key has been sent
		for(Map.Entry<String, String[]> entry : blueprint.entrySet()){
			String key = entry.getKey();
			if(!blueprintReq.containsKey(key))  // invalid key sent
				throw new BadParameterException("Key: '" + key + "' does not exist for GroceryItem.");
		}
		
		// OK we have a validBlueprint and are good to go

		log.info("Populating 'GroceryItem'.");
		// populate grocery item object from blueprint
		GroceryItem groceryItem = new GroceryItem();
		for(Map.Entry<String, String[]> entry : blueprint.entrySet()){
			String key = entry.getKey();
			String value = entry.getValue()[0];  // we assume single-valued attributes
			log.info("Key: "+ key + " Value: "+ value);
			if(key.equals(Constants.AISLE_KEY)){
				if(!(value.length() == 0)){
					int valueInt = 0;
					try {
						valueInt = Integer.parseInt(value);
					} catch (Throwable ex){
						throw new BadParameterException("Aisle must be an integer");
					}
					if(valueInt <= 0){
						throw new BadParameterException("Aisle value cannot be negative");
					}
					groceryItem.setAisle(valueInt);			
				}
			}
			else if(key.equals(Constants.QTY_KEY)){
				int valueInt = 0;
				try {
					valueInt = Integer.parseInt(value);
				} catch (NumberFormatException ex){
					throw new BadParameterException("Quantity must be an integer");
				}
				if(valueInt <= 0){
					throw new BadParameterException("Quantity value cannot be negative");
				}
				groceryItem.setQty(valueInt);
			}
			else if(key.equals(Constants.PNAME_KEY))
				groceryItem.setPname(value);
			else if(key.equals(Constants.BNAME_KEY))
				groceryItem.setBname(value);
			else if(key.equals(Constants.CUSTOM_KEY))
				groceryItem.setCustom(value);
			else if(key.equals(Constants.ID_KEY))
				groceryItem.setId(value);
		}
		log.info("Done populating grocery items.");
		return groceryItem;
	}

	/**
		JSONObject representation of the Class object.
		@return JSONObject. JSONObject representation of the Class object.
	 */
	public JSONObject getJSONObject(){
		JSONObject jObject = new JSONObject();
		Map<String, Boolean> blueprint = GroceryItem.getGroceryItemBlueprint();
		for(String key: blueprint.keySet()) {
			if(key.equals(Constants.AISLE_KEY))
				jObject.put(Constants.AISLE_KEY, this.getAisle());
			else if(key.equals(Constants.PNAME_KEY))
				jObject.put(Constants.PNAME_KEY, this.getPname());
			else if(key.equals(Constants.BNAME_KEY))
				jObject.put(Constants.BNAME_KEY, this.getBname());
			else if(key.equals(Constants.QTY_KEY))
				jObject.put(Constants.QTY_KEY, this.getQty());
			else if(key.equals(Constants.CUSTOM_KEY))
				jObject.put(Constants.CUSTOM_KEY, this.getCustom());
			else if(key.equals(Constants.ID_KEY))
				jObject.put(Constants.ID_KEY, this.getId());
		}
		return jObject;
	}
}	
