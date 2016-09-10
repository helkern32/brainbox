package utility;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;


@Configuration
@PropertySource("classpath:../application-config.properties")
public class Sender {

	private static Logger log = Logger.getLogger("file");
	private static Properties props;
	private static Session session;
	
	@Autowired
	private Environment env;
	
	@PostConstruct
	public void init() {
		props = getProperties();
		session = getSession();
	}
	
	@Bean
	public Session getSession() {
		log.info("Sender.getSession: get mail session");
		return Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(env.getProperty("mail.address"), env.getProperty("mail.password"));
			}
		});
	}

	@Bean
	public Properties getProperties() {
		log.info("Sender.getProperties: get mail properties");
		props = new Properties();
		props.put("mail.smtp.auth", env.getProperty("mail.smtp.auth"));
		props.put("mail.smtp.starttls.enable", env.getProperty("mail.smtp.starttls.enable"));
		props.put("mail.smtp.host", env.getProperty("mail.smtp.host"));
		props.put("mail.smtp.port", env.getProperty("mail.smtp.port"));
		props.put("mail.address", env.getProperty("mail.address"));
		return props;
	}

	public static void sendEmail(File file, String receiver) throws IOException {
		log.info("Sender.sendEmail: send e-book to device");
		try {
			System.out.println(file.getAbsolutePath());
			
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(props.getProperty("mail.address")));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver));
			String fileName = file.getAbsolutePath();
			fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.length());
			message.setSubject(fileName);
			
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText("");
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			messageBodyPart = new MimeBodyPart();
			String path = file.getAbsolutePath();
			DataSource source = new FileDataSource(path);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(fileName);
			multipart.addBodyPart(messageBodyPart);
			message.setContent(multipart);
			Transport.send(message);
			log.info("Sender.sendEmail: ebook was sent on email successfully");
		} catch (AddressException e) {
			log.error("Sender.sendEmail: catch AddressException while sending comment on email, full stack trace follows:", e);
			e.printStackTrace();
		} catch (MessagingException e) {
			log.error("Sender.sendEmail: catch MessagingException while sending comment on email, full stack trace follows:", e);
			e.printStackTrace();
		}
	}
}
