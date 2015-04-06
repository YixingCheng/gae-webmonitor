package io.github.lorenzosaino.webmonitor.services;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * User notification service
 */
public class NotificationService {

	private String senderName = null;
	private String senderEmail = null;
	
	/**
	 * Constructor
	 * 
	 * @param senderName
	 * @param senderEmail
	 */
	public NotificationService(String senderName, String senderEmail) {
		this.senderName = senderName;
		this.senderEmail = senderEmail;
	}
	
	/**
	 * Notify a user about a change in a Web object it subscribed to
	 * 
	 * @param email User email address
	 * @param uri URI of the page that changed
	 */
	public void notifyUser(String email, String uri) {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(true);
        
        String msgBody = "The webpage " + uri + " has changed";

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(this.senderEmail, this.senderName));
            msg.addRecipient(Message.RecipientType.TO,
                             new InternetAddress(email));
            msg.setSubject("Web page " + uri + " has changed");
            msg.setText(msgBody);
            Transport.send(msg);
        } catch (MessagingException|UnsupportedEncodingException e) {
        	throw new IllegalArgumentException(e.getMessage());
        } 
	}
	
}
