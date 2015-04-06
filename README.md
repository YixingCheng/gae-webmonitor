# Web monitor for Google AppEngine

A simple Web monitor that periodically visits a set of URLs specified by the 
user. If a visited URL changed since last visit, it emails the user notifying
it of the change.

Note that this project is just a stub. It does not contain test cases and
potential errors arising when accessing the datastore are not handled.
Also datastore entries need to be inserted manually.

## Setup Instructions
1. Import the project into Eclipse.
1. Make sure the App Engine SDK jars are present in the `war/WEB-INF/lib`
   directory, either by adding them by hand, or having Eclipse do it. An easy
   way to do this in Eclipse is to unset and reset whether or not the project
   uses Google App Engine.
1. Update the value of `application` in `appengine-web.xml` to the app ID you
   have registered in the App Engine admin console and would like to use to host
   your instance of this sample.
1. Configure sender email address, sender name and tracker user agent in
   `web.xml`
1. Set the polling period in `cron.xml`. The default is 1 hour.
1. Run the application, and ensure it's running by visiting your local server's
   address (by default [http://localhost:8888/](https://localhost:8888/)).
1. Deploy your application.
1. Manually the populate datastore using Google Developers Console
   (see datastore indexes information below)

## Set up datastore indexes
To set up the datastore it is necessary to populate it with:
 * Users: e-mail addresses to which page changes notifications are sent
 * Objects: list of URIs monitored for changes
 * Object subscriptions: recording which users are subscribed to which
   page

Start by inserting the email addresses of the users to be notified in the 
`User` index, then the URIs to monitor in the `Object` index and finally
the specific user/object mapping (in the form of email/URI mapping) in the
`Subscription` index.

Note that the `DataStoreService` class already includes many methods to
perform operations on the data store that can be used to extend this
application. 