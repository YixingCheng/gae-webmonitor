package io.github.lorenzosaino.webmonitor.entities;

import java.util.Date;

/**
 * Class representing an instance of a Web object
 */
public class WebObjectInstance {

	private String uri = null;
	private String content = null;
	private String contentType = null;
	private Date timestamp = null;
	private int statusCode = 0;
	
	/**
	 * Constructor
	 * 
	 * @param content The string representing the content of the object
	 * @param contentType The MIME type of the object
	 * @param date The date the content was retrieved
	 */
	public WebObjectInstance(String uri, String content, String contentType,
			Date timestamp, int statusCode) {
		
		if(uri == null || content == null || timestamp == null) {
			throw new IllegalArgumentException("content, timestamp and uri parameters cannot be null");
		}
		this.uri = uri;
		this.content = content;
		this.contentType = contentType;
		this.timestamp = timestamp;
		this.statusCode = statusCode;
	}
	
	
	/**
	 * Get the URI
	 * 
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Set the URI
	 * 
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}


	/**
	 * Get the content
	 * 
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	
	/**
	 * Set the content
	 * 
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}
	
	/**
	 * Get the content type
	 * 
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Set the content type
	 * 
	 * @param contentType the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Get the timestamp the content was retrieved
	 * 
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Set the timestamp the content was retrieved
	 * 
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Get the status code
	 * 
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Set the status code
	 * 
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
}
