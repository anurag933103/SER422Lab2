package edu.asupoly.ser422.renderers;

import edu.asupoly.ser422.grocery.Constants;
import edu.asupoly.ser422.grocery.MyServerException;

public final class MyLab1RendererFactory {

	private MyLab1RendererFactory() {
		// No state
	}

	// Factory method
	public static MyLab1Renderer getRenderer(String contentType, String url) throws MyServerException {
		if (contentType.indexOf(Constants.CONTENT_HTML) != -1) {
			return new HTMLRenderer(url);
		} else if (contentType.indexOf(Constants.CONTENT_TEXT) != -1) {
			return new TextRenderer(url);
		} else if (contentType.indexOf(Constants.CONTENT_JSON) != -1) {
			return new JSONRenderer(url);
		}
		// if we are still here then we've lost track fo the content type
		// should never happen, but let's be safe
		throw new MyServerException("Trying to render to unknown content type " + contentType);
	}
}
