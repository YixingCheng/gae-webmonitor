package io.github.lorenzosaino.webmonitor.services;

import io.github.lorenzosaino.webmonitor.entities.WebObjectInstance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Object retrieval service
 */
public class ObjectRetrievalService {

	private String userAgent = null;

	/**
	 * Constructor
	 * 
	 * @param userAgent The user agent to be used for issuing HTTP requests
	 */
	public ObjectRetrievalService(String userAgent) {
		this.userAgent = userAgent;
	}
	
	/**
	 * Retrieve an object
	 * 
	 * @param uri The URI of the object
	 * 
	 * @return The instance of the object retrieved
	 * 
	 * @throws IOException if the object cannot be retrieved due to I/O problems
	 */
	public WebObjectInstance retrieveObject(String uri) throws IOException {		
		
		String content = null;
		String contentType = null;
		int statusCode = 0;
		Date timestamp = null;
		
		HttpURLConnection connection = 
				(HttpURLConnection) new URL(uri).openConnection();
		connection.setRequestProperty("User-Agent", this.userAgent);
		connection.setRequestProperty("Connection", "close");
		connection.setInstanceFollowRedirects(true); // it is already the default
		InputStream in = connection.getInputStream();
		contentType = connection.getContentType();
		statusCode  = connection.getResponseCode();
		StringBuilder contentBuilder = new StringBuilder();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));;
	    String currentLine = null;
	    while ((currentLine = reader.readLine()) != null) {
	    	contentBuilder.append(currentLine);
	    }
	    if(in != null) {
	    	in.close();
	    }
	    if (connection != null) {
	    	connection.disconnect();
	    }
		timestamp = new Date();
		content = contentBuilder.toString();
		
		return new WebObjectInstance(uri, content, contentType, 
				timestamp, statusCode);
	}

}
