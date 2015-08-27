package io.github.lorenzosaino.webmonitor;

import io.github.lorenzosaino.webmonitor.entities.WebObjectInstance;
import io.github.lorenzosaino.webmonitor.services.DataStoreService;
import io.github.lorenzosaino.webmonitor.services.NotificationService;
import io.github.lorenzosaino.webmonitor.services.ObjectRetrievalService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web monitor servlet
 */
public class WebMonitorServlet extends HttpServlet {
	
	private static final long serialVersionUID = -3884556235221420301L;
	private static final Logger log = 
			Logger.getLogger(WebMonitorServlet.class.getName());
	
	/* Instances of services needed */
	private DataStoreService datastore = null;
	private NotificationService notifier = null;
	private ObjectRetrievalService retriever = null;
	
	private int maxRetrievalAttempts = 2;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		/* Get all config parameters */
		String userAgent = config.getInitParameter("retriever.userAgent");
		String senderName = config.getInitParameter("notifier.senderName");
		String senderEmail = config.getInitParameter("notifier.senderEmail");
		
		/* Instantiate all services required */
		this.datastore = DataStoreService.getInstance();
		this.notifier = new NotificationService(senderName, senderEmail);
		this.retriever = new ObjectRetrievalService(userAgent);
		
		
		datastore.addUser("waldenlaker@hotmail.com");
		datastore.addUser("r3000.mitbbs@gmail.com");
		try {
		    Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
    
		datastore.addObject("http://dealsea.com/view/bsd.dell.com");
		
		try {
		    Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}

		datastore.addSubscription("waldenlaker@hotmail.com", "http://dealsea.com/view/bsd.dell.com");
		datastore.addSubscription("r3000.mitbbs@gmail.com", "http://dealsea.com/view/bsd.dell.com");
		try {
		    Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		log.info("Start Web tracker polling servlet");
		run();
	}
	
	
	/**
	 * Run the Web monitor poller
	 */
	private void run() {
		int retrievalAttempts = 0;
		WebObjectInstance oldInstance = null, newInstance = null;
		List<String> registeredUriList = null;
		List<String> subscriberList = null;
		
		registeredUriList = datastore.getAllRegisteredObjects();
		for (String uri : registeredUriList) {
			retrievalAttempts = 0;
			do {
				try {
					newInstance = retriever.retrieveObject(uri);
					break;
				} catch (Exception e) {
					retrievalAttempts++;
					log.warning("I/O issue while trying to retrieve object " 
							+ uri +	" at attempt " + (retrievalAttempts) +
							"/" + maxRetrievalAttempts);
				}
			} while(retrievalAttempts < maxRetrievalAttempts);
			if(retrievalAttempts == maxRetrievalAttempts) {
				/* Skip this URI */
				log.warning("Could not retrieve object " 
						+ uri +	" because of repeated I/O errors");
				continue;
			}
			oldInstance = datastore.getMostRecentObjectInstance(uri);
			if(oldInstance == null) {
				/*
				 * This occurs only when the application is launched for the
				 * this is the first polling since the page has been added.
				 * Just add the object instance without sending any notification
				 */
				datastore.addObjectInstance(newInstance);
				continue;
			}
            
			boolean areObjectsEqual = compareInstances(oldInstance, newInstance);

			if(areObjectsEqual) {
				datastore.updateObjectInstanceTimestamp(oldInstance.getUri(), 
						oldInstance.getTimestamp(), newInstance.getTimestamp());
				log.info("There is no change on " + oldInstance.getUri() + "!");
				continue;
			}
			
			List<String> updates = getDifference(newInstance.getContent(), oldInstance.getContent());
			datastore.addObjectInstance(newInstance);
			subscriberList = datastore.getSubscribers(uri);
			for (String email: subscriberList) {
				try {
					notifier.notifyUser(email, uri, updates);
				} catch (IllegalArgumentException e) {
					log.warning("Could not notify user " + email + " about " +
							"changes to " + uri + ". Error: " + e.getMessage());
				}
			}
		}		
	}

	
	/**
	 * Compare two Web object instances.
	 * 
	 * Check status codes, check when content may be null, two null contents 
	 * may mean the page didn't change anyway.
	 * 
	 * @param a One instance
	 * @param b The other instance
	 * @return true if the two instances match, false otherwise
	 */
	
	private static boolean compareInstances(WebObjectInstance a, WebObjectInstance b) {

		String aContentType = a.getContentType();
		String bContentType = b.getContentType();
		
		int aStatusCode = a.getStatusCode();
		int bStatusCode = b.getStatusCode();
		
		ArrayList<String> aContent = (ArrayList<String>) a.getContent();
		ArrayList<String> bContent = (ArrayList<String>) b.getContent();
		
		if(aStatusCode != bStatusCode) {
			return false;
		}
		if(aContentType != null && bContentType != null && 
				!aContentType.equals(bContentType)) {
			return false;
		}
		if(aContent == null && bContent != null) {
			return false;
		}
		if(aContent != null && bContent == null) {
			return false;
		}
		return equalLists(aContent, bContent);
	}
	
	//auxiliary function to compare two List of strings 
	private static boolean equalLists(List<String> one, List<String> two){     
	    if (one == null && two == null){
	        return true;
	    }

	    if((one == null && two != null) 
	      || one != null && two == null
	      || one.size() != two.size()){
	        return false;
	    }

	    //to avoid messing the order of the lists we will use a copy
	    //as noted in comments by A. R. S.
	    one = new ArrayList<String>(one); 
	    two = new ArrayList<String>(two);   

	    Collections.sort(one);
	    Collections.sort(two);      
	    return one.equals(two);
	}
	
	private static List<String> getDifference(List<String> newObj, List<String> oldObj){
		List<String> diff = new ArrayList<String>();
		for(String deal: newObj){
			if(!oldObj.contains(deal)){
				diff.add(deal);
			}
		}
		
		return diff;
	}
	
}
