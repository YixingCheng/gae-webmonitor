package io.github.lorenzosaino.webmonitor.services;

import io.github.lorenzosaino.webmonitor.entities.WebObjectInstance;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Text;

/**
 * Data store access service
 */
public class DataStoreService {
	
	private static final String USER = "User";
	private static final String OBJECT = "Object";
	private static final String SUBSCRIPTION = "Subscription";
	private static final String OBJECT_INSTANCE = "ObjectInstance";

	private DatastoreService datastoreService = null;
	private static DataStoreService instance = null;
	
	private static final Logger log = 
			Logger.getLogger(DataStoreService.class.getName());

	/**
	 * Constructor
	 */
	private DataStoreService() {
		datastoreService = DatastoreServiceFactory.getDatastoreService();
	};

	/**
	 * Get instance of the datastore service
	 * 
	 * @return an instance of the datastore service
	 */
	public static DataStoreService getInstance() {
		if (instance == null) {
			instance = new DataStoreService();
		}
		return instance;
	}

	/**
	 * Add a new object
	 * 
	 * @param uri The URI of the object
	 */
	public void addObject(String uri) {
		if (isObjectRegistered(uri)) {
			return;
		}
		Entity webObject = new Entity(OBJECT);
		webObject.setProperty("uri", uri);
		datastoreService.put(webObject);
	}

	/**
	 * Remove an object from the datastore
	 * 
	 * @param uri The URI
	 */
	public void removeObject(String uri) {
		if (!isObjectRegistered(uri)) {
			return;
		}
		// Remove all WebObjectInstance entries associated
		Filter uriFilter = new FilterPredicate("uri", FilterOperator.EQUAL, uri);
		Query instanceQuery = new Query(OBJECT_INSTANCE)
				.setFilter(uriFilter)
				.addSort("timestamp", SortDirection.DESCENDING);
		List<Entity> instances = datastoreService
				.prepare(instanceQuery)
				.asList(FetchOptions.Builder.withDefaults());
		List<Key> keys = new ArrayList<Key>();
		for (Entity e : instances) {
			keys.add(e.getKey());
		}
		datastoreService.delete(keys);
		
		// Remove actual WebObject entry
		Query objectQuery = new Query(OBJECT).setFilter(uriFilter);
		Entity object = datastoreService.prepare(objectQuery).asSingleEntity();
		datastoreService.delete(object.getKey());
	}

	/**
	 * Check whether a specific object is in the datastore
	 * 
	 * @param uri The URI of the object
	 * @return true if present, false otherwise
	 */
	public boolean isObjectRegistered(String uri) {
		Filter uriFilter = new FilterPredicate("uri", FilterOperator.EQUAL, uri);
		Query query = new Query(OBJECT).setFilter(uriFilter);
		Entity object = datastoreService.prepare(query).asSingleEntity();
		return (object != null);
	}

	/**
	 * Get all objects registered
	 * 
	 * @return The list of all objects registered
	 */
	public List<String> getAllRegisteredObjects() {
		Query query = new Query(OBJECT);
		List<Entity> instances = datastoreService.prepare(query).asList(
				FetchOptions.Builder.withDefaults());
		List<String> registeredUri = new ArrayList<String>();
		for (Entity e : instances) {
			registeredUri.add((String) e.getProperty("uri"));
		}
		return registeredUri;
	}

	/**
	 * Add object instance
	 * 
	 * @param instance The object instance
	 */
	public void addObjectInstance(WebObjectInstance instance) {
		String uri = instance.getUri();
		// Also insert object if not present yet
		if (!isObjectRegistered(uri)) {
			addObject(uri);
		}
		Entity webObjectInstance = new Entity(OBJECT_INSTANCE);
		webObjectInstance.setProperty("uri", instance.getUri());
		webObjectInstance.setProperty("content",
				new Text(instance.getContent()));
		webObjectInstance.setProperty("contentType", instance.getContentType());
		webObjectInstance.setProperty("timestamp", instance.getTimestamp());
		webObjectInstance.setProperty("statusCode", instance.getStatusCode());
		datastoreService.put(webObjectInstance);

	}

	/**
	 * Update the timestamp of an object instance
	 * 
	 * @param uri The URI of the Web object instance
	 * @param oldTimestamp The old timestamp
	 * @param newTimestamp The new timestamp
	 */
	public void updateObjectInstanceTimestamp(String uri, Date oldTimestamp,
			Date newTimestamp) {
		Filter uriFilter = new FilterPredicate("uri", FilterOperator.EQUAL, uri);
		Filter timestampFilter = new FilterPredicate("timestamp", FilterOperator.EQUAL, oldTimestamp);
		Query query = new Query(OBJECT_INSTANCE)
			.setFilter(uriFilter)
			.setFilter(timestampFilter);
		Entity instance = datastoreService.prepare(query).asSingleEntity();
		if (instance == null) {
			throw new IllegalArgumentException(
					"No record with matching URI and timestamp was found");
		}
		instance.setProperty("timestamp", newTimestamp);
		datastoreService.put(instance);
	}

	/**
	 * Get all instances of an object present in the data store
	 * 
	 * @param uri The URI of the Web object
	 * @return The list of Web instances
	 */
	public List<WebObjectInstance> getAllObjectInstances(String uri) {
		Filter uriFilter = new FilterPredicate("uri", FilterOperator.EQUAL, uri);
		Query query = new Query(OBJECT_INSTANCE)
				.setFilter(uriFilter)
				.addSort("timestamp", SortDirection.DESCENDING);

		List<Entity> instances = datastoreService.prepare(query)
				.asList(FetchOptions.Builder.withDefaults());
		List<WebObjectInstance> instanceList = new ArrayList<WebObjectInstance>();
		for (Entity e : instances) {
			String content = ((Text) e.getProperty("content")).getValue();
			String contentType = (String) e.getProperty("contentType");
			int statusCode = ((Integer) e.getProperty("statusCode")).intValue();
			Date timestamp = (Date) e.getProperty("timestamp");
			instanceList.add(new WebObjectInstance(uri, content, contentType,
					timestamp, statusCode));
		}
		return instanceList;
	}

	/**
	 * Get the most recent instance of a web object available
	 * 
	 * @param uri The URI of the Web object
	 * @return The instance of the Web object
	 */
	public WebObjectInstance getMostRecentObjectInstance(String uri) {
		Filter uriFilter = new FilterPredicate("uri", FilterOperator.EQUAL, uri);
		Query query = new Query(OBJECT_INSTANCE)
				.setFilter(uriFilter)
				.addSort("timestamp", SortDirection.DESCENDING);
		List<Entity> instances = datastoreService.prepare(query).asList(
				FetchOptions.Builder.withDefaults());
		if (instances == null || instances.isEmpty()) {
			return null;
		}
		Entity mostRecentInstance = instances.get(0);
		String content = ((Text) mostRecentInstance.getProperty("content"))
				.getValue();
		String contentType = (String) mostRecentInstance
				.getProperty("contentType");
		Date timestamp = (Date) mostRecentInstance.getProperty("timestamp");
		int statusCode = ((Long) mostRecentInstance.getProperty("statusCode"))
				.intValue();
		log.info("retrieved the most recent object instance from datastore");
		return new WebObjectInstance(uri, content, contentType, timestamp,
				statusCode);
	}

	/**
	 * Get the list of objects a user is subscribed to
	 * 
	 * @param email The user email address
	 * @return The list of objects' URIs 
	 */
	public List<String> getObjectsSubscribed(String email) {
		Filter emailFilter = new FilterPredicate("email", FilterOperator.EQUAL, email);
		Query query = new Query(SUBSCRIPTION).setFilter(emailFilter);
		List<Entity> entity = datastoreService.prepare(query)
				.asList(FetchOptions.Builder.withDefaults());
		List<String> uri = new ArrayList<String>();
		for (Entity e : entity) {
			uri.add((String) e.getProperty("uri"));
		}
		return uri;
	}

	/**
	 * Get a list of subscribed users for a given object
	 * 
	 * @param uri The URI of the object
	 * @return The list of subscribed users
	 */
	public List<String> getSubscribers(String uri) {
		Filter uriFilter = new FilterPredicate("uri", FilterOperator.EQUAL, uri);
		Query query = new Query(SUBSCRIPTION).setFilter(uriFilter);
		List<Entity> entity = datastoreService.prepare(query).asList(
				FetchOptions.Builder.withDefaults());
		List<String> email = new ArrayList<String>();
		for (Entity e : entity) {
			email.add((String) e.getProperty("email"));
		}
		return email;
	}

	/**
	 * Add an object subscription
	 * 
	 * @param email The email of the user
	 * @param uri The URI of the object
	 */
	public void addSubscription(String email, String uri) {
		if (!isUserRegistered(email)) {
			throw new IllegalArgumentException("The user is not registered");
		}
		if (isUserSubscribed(email, uri)) {
			throw new IllegalArgumentException("The user is already subscribed");
		}
		Entity webObjectSubscription = new Entity(SUBSCRIPTION);
		webObjectSubscription.setProperty("uri", uri);
		webObjectSubscription.setProperty("email", email);
		datastoreService.put(webObjectSubscription);

	}

	/** 
	 * Remove an object subscription
	 * 
	 * @param email The user email
	 * @param uri The Web object URI
	 */
	public void removeSubscription(String email, String uri) {
		if (!isUserSubscribed(email, uri)) {
			throw new IllegalArgumentException("The user is not subscribed");
		}
		Filter uriFilter = new FilterPredicate("uri", FilterOperator.EQUAL, uri);
		Filter emailFilter = new FilterPredicate("email", FilterOperator.EQUAL, email);
		Query query = new Query(SUBSCRIPTION)
				.setFilter(emailFilter)
				.setFilter(uriFilter);
		Entity subscription = datastoreService.prepare(query).asSingleEntity();
		datastoreService.delete(subscription.getKey());
	}

	/**
	 * Check whether a user is subscribed to a Web object
	 * 
	 * @param email The user email address
	 * @param uri The object URI
	 * 
	 * @return true if the user is subscribed, false otherwise
	 */
	public boolean isUserSubscribed(String email, String uri) {
		Filter uriFilter = new FilterPredicate("uri", FilterOperator.EQUAL, uri);
		Filter emailFilter = new FilterPredicate("email", FilterOperator.EQUAL, email);
		Query query = new Query(SUBSCRIPTION)
				.setFilter(emailFilter)
				.setFilter(uriFilter);
		Entity subscription = datastoreService.prepare(query).asSingleEntity();
		return (subscription != null);

	}

	/**
	 * Check if a user is registered
	 * 
	 * @param email The user email address
	 * 
	 * @return true if the user is registered, false otherwise
	 */
	public boolean isUserRegistered(String email) {
		Filter emailFilter = new FilterPredicate("email", FilterOperator.EQUAL, email);
		Query query = new Query(USER).setFilter(emailFilter);
		Entity user = datastoreService.prepare(query).asSingleEntity();
		return (user != null);
	}

	/**
	 * Add a user
	 * 
	 * @param email the user email address
	 */
	public void addUser(String email) {
		if (isUserRegistered(email)) {
			throw new IllegalArgumentException("User already registered");
		}
		Entity user = new Entity(USER);
		user.setProperty("email", email);
		datastoreService.put(user);
	}
	
	/**
	 * Remove a user
	 * 
	 * @param email the user email address
	 */
	public void removeUser(String email) {
		if (!isUserRegistered(email)) {
			throw new IllegalArgumentException("User not registered");
		}
		Filter emailFilter = new FilterPredicate("email", FilterOperator.EQUAL, email);
		Query query = new Query(USER).setFilter(emailFilter);
		Entity user = datastoreService.prepare(query).asSingleEntity();
		datastoreService.delete(user.getKey());
	}

}
