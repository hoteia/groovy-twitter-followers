
import javax.mail.*
import javax.mail.internet.*
 
public static void sendErrorMail(String body) throws Exception {
	try {
		def context = System.getProperty("context")
		def rootScriptDir = System.getProperty("rootScriptDir")
		def config = new ConfigSlurper().parse(new File(rootScriptDir + 'conf/' + context + '/config.properties').toURL());

		if("on" == config.email.error){
			Properties props = System.getProperties();
			props.put("mail.smtp.starttls.enable", config.conf.smtp.mail.smtp.starttls.enable);
			props.setProperty("mail.smtp.ssl.trust", config.conf.smtp.host);
			props.put("mail.smtp.auth", true);      
			props.put("mail.smtp.port", config.conf.smtp.mail.smtp.port);
		 
			Session session = Session.getDefaultInstance(props, null);
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(config.conf.email.from));
		 
			InternetAddress toAddress = new InternetAddress(config.conf.email.to);
		 
			message.addRecipient(Message.RecipientType.TO, toAddress);
		 
			def today = new Date()
			message.setSubject("Error Groovy for Twitter with context: " + context + " " + today);
			message.setText(body);
		 
			Transport transport = session.getTransport("smtp");
		 
			transport.connect(config.conf.smtp.host, config.conf.smtp.username, config.conf.smtp.password);
		 
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		}
		
	} catch(Exception ex) { 
		ex.printStackTrace(); 
	}
}


public static void sendSuccessMail(String type, int size) throws Exception {
	try {
		def context = System.getProperty("context")
		def rootScriptDir = System.getProperty("rootScriptDir")
		def config = new ConfigSlurper().parse(new File(rootScriptDir + 'conf/' + context + '/config.properties').toURL());

		if("on" == config.email.success){
			Properties props = System.getProperties();
			props.put("mail.smtp.starttls.enable", config.conf.smtp.mail.smtp.starttls.enable);
			props.setProperty("mail.smtp.ssl.trust", config.conf.smtp.host);
			props.put("mail.smtp.auth", true);      
			props.put("mail.smtp.port", config.conf.smtp.mail.smtp.port);
		 
			Session session = Session.getDefaultInstance(props, null);
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(config.conf.email.from));
		 
			InternetAddress toAddress = new InternetAddress(config.conf.email.to);
		 
			message.addRecipient(Message.RecipientType.TO, toAddress);
		 
			def today = new Date()
			message.setSubject("Groovy Twitter success: " + size + " " + type + " with context: " + context + " " + today);
			message.setText("Groovy Twitter success: " + size + " " + type + " with context: " + context);
		 
			Transport transport = session.getTransport("smtp");
		 
			transport.connect(config.conf.smtp.host, config.conf.smtp.username, config.conf.smtp.password);
		 
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		}
		
	} catch(Exception ex) { 
		ex.printStackTrace(); 
	}
}

public static void sendNotificationMail(String notification) throws Exception {
        try {
			def context = System.getProperty("context")
			def rootScriptDir = System.getProperty("rootScriptDir")
			def config = new ConfigSlurper().parse(new File(rootScriptDir + 'conf/' + context + '/config.properties').toURL());

			if("on" == config.email.notification){
				Properties props = System.getProperties();
				props.put("mail.smtp.starttls.enable", config.conf.smtp.mail.smtp.starttls.enable);
				props.setProperty("mail.smtp.ssl.trust", config.conf.smtp.host);
				props.put("mail.smtp.auth", true);
				props.put("mail.smtp.port", config.conf.smtp.mail.smtp.port);

				Session session = Session.getDefaultInstance(props, null);
				MimeMessage message = new MimeMessage(session);
				message.setFrom(new InternetAddress(config.conf.email.from));

				InternetAddress toAddress = new InternetAddress(config.conf.email.to);

				message.addRecipient(Message.RecipientType.TO, toAddress);

				def today = new Date()
				message.setSubject("Groovy for Twitter context: " + context + " : " + notification + " " + today);
				message.setText("Groovy for Twitter context: " + context + " : " + notification);

				Transport transport = session.getTransport("smtp");

				transport.connect(config.conf.smtp.host, config.conf.smtp.username, config.conf.smtp.password);

				transport.sendMessage(message, message.getAllRecipients());
				transport.close();
			}
			
        } catch(Exception ex) {
			ex.printStackTrace();
        }
}

public static void sendSummaryMail(String summary, String followersCount) throws Exception {
        try {
			def context = System.getProperty("context")
			def rootScriptDir = System.getProperty("rootScriptDir")
			def config = new ConfigSlurper().parse(new File(rootScriptDir + 'conf/' + context + '/config.properties').toURL());

			Properties props = System.getProperties();
			props.put("mail.smtp.starttls.enable", config.conf.smtp.mail.smtp.starttls.enable);
			props.setProperty("mail.smtp.ssl.trust", config.conf.smtp.host);
			props.put("mail.smtp.auth", true);
			props.put("mail.smtp.port", config.conf.smtp.mail.smtp.port);

			Session session = Session.getDefaultInstance(props, null);
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(config.conf.email.from));

			InternetAddress toAddress = new InternetAddress(config.conf.email.to);

			message.addRecipient(Message.RecipientType.TO, toAddress);

			def today = new Date()
			message.setSubject("Groovy for Twitter Summay: Followers=" + followersCount + " : context: " + context + " : " + today);
			message.setText(summary);

			Transport transport = session.getTransport("smtp");

			transport.connect(config.conf.smtp.host, config.conf.smtp.username, config.conf.smtp.password);

			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
			
        } catch(Exception ex) {
			ex.printStackTrace();
        }
}