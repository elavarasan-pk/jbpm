package com.hi.techpoints.listener;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.jbpm.services.task.deadlines.notifications.impl.email.EmailNotificationListener;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.task.api.TaskModelProvider;
import org.kie.internal.task.api.UserInfo;
import org.kie.internal.task.api.model.EmailNotification;
import org.kie.internal.task.api.model.EmailNotificationHeader;
import org.kie.internal.task.api.model.InternalOrganizationalEntity;
import org.kie.internal.task.api.model.Language;
import org.kie.internal.task.api.model.NotificationEvent;
import org.mvel2.templates.TemplateRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomEmailNotificationListener extends EmailNotificationListener {

	private static final Logger logger = LoggerFactory.getLogger(CustomEmailNotificationListener.class);

	private Session mailSession = null;

	public CustomEmailNotificationListener() {
		final Properties conf = new Properties();
		try {
			conf.load(CustomEmailNotificationListener.class.getResourceAsStream("/custom.email.properties"));

			Authenticator auth = new javax.mail.Authenticator() {
				// override the getPasswordAuthentication method
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(conf.getProperty("mail.username"),
							conf.getProperty("mail.password"));
				}
			};

			mailSession = Session.getInstance(conf, auth);
		} catch (Exception e) {
			logger.debug("CustomEmailNotificationListener: email.properties was not found on classpath");
		}
	}

	@Override
	public void onNotification(NotificationEvent event, UserInfo userInfo) {
        logger.debug("User info implementation {} and mail session {}", userInfo, mailSession);
        if (userInfo == null || mailSession == null) {
            logger.info("Missing mail session or userinfo - skipping email notification listener processing");
            return;
        }
        
        if (event.getNotification() instanceof EmailNotification) {
  
            EmailNotification notification = (EmailNotification) event.getNotification();
            
            Task task = event.getTask();

            // group users into languages
            List<OrganizationalEntity> entities = new ArrayList<>();
            entities.addAll(notification.getBusinessAdministrators());
            entities.addAll(notification.getRecipients());

            Map<String, List<OrganizationalEntity>> users = new HashMap<>();
            for (OrganizationalEntity entity : entities) {
                if (entity instanceof Group) {
                    buildMapByLanguage(users, (Group) entity, userInfo);
                } else {
                    buildMapByLanguage(users, entity, userInfo);
                }
            }

            Map<String, Object> variables = event.getContent();
            Map<? extends Language, ? extends EmailNotificationHeader> headers = notification.getEmailHeaders();

            for (Iterator<Map.Entry<String, List<OrganizationalEntity>>> it = users.entrySet()
                    .iterator(); it.hasNext();) {
                try { 
                    Map.Entry<String, List<OrganizationalEntity>> entry = it.next();
                    Language lang = TaskModelProvider.getFactory().newLanguage();
                    lang.setMapkey(entry.getKey());
                    EmailNotificationHeader header = headers.get(lang);
    
                    Message msg = new MimeMessage(mailSession);
                    Set<String> toAddresses = new HashSet<String>();
                    for (OrganizationalEntity user : entry.getValue()) {
    
                        String emailAddress = getEmailFromOrganizationEntity(userInfo, user);
                        if (emailAddress != null && !emailAddress.isEmpty()) {
                        	if (toAddresses.add(emailAddress)) {
                        	    msg.addRecipients( Message.RecipientType.TO, InternetAddress.parse( emailAddress, false));
                        	}
                        } else {
                        	logger.warn("Email address not found for user '{}'", user.getId());
                        }
                    }
                    
    
                    if (header.getFrom() != null && header.getFrom().trim().length() > 0) {
                    	User user = TaskModelProvider.getFactory().newUser();
                    	((InternalOrganizationalEntity) user).setId(header.getFrom());
                        msg.setFrom( new InternetAddress(userInfo.getEmailForEntity(user)));
                    } else {
                        msg.setFrom( new InternetAddress(mailSession.getProperty("mail.from")));
                    }
    
                    if (header.getReplyTo() != null && header.getReplyTo().trim().length() > 0) {
                    	User user = TaskModelProvider.getFactory().newUser();
                    	((InternalOrganizationalEntity) user).setId(header.getReplyTo());
                        msg.setReplyTo( new InternetAddress[] {  
                                new InternetAddress(userInfo.getEmailForEntity(user))});
                    } else if (mailSession.getProperty("mail.replyto") != null) {
                        msg.setReplyTo( new InternetAddress[] {  new InternetAddress(mailSession.getProperty("mail.replyto"))});
                    }
                    
                    Map<String, Object> vars = new HashMap<String, Object>();
                    vars.put("doc", variables);
                    // add internal items to be able to reference them in templates
                    vars.put("processInstanceId", task.getTaskData().getProcessInstanceId());
                    vars.put("processSessionId", task.getTaskData().getProcessSessionId());
                    vars.put("workItemId", task.getTaskData().getWorkItemId());
                    vars.put("expirationTime", task.getTaskData().getExpirationTime());
                    vars.put("taskId", task.getId());
                    if (task.getPeopleAssignments() != null) {
                        vars.put("owners", task.getPeopleAssignments().getPotentialOwners());
                    }
    
                    String subject = (String) TemplateRuntime.eval(header.getSubject(), vars);
                    String body = (String) TemplateRuntime.eval(header.getBody(), vars);
    
                    if (variables.containsKey("attachments")) {
                        Multipart multipart = new MimeMultipart();
                        // prepare body as first mime body part
                        MimeBodyPart messageBodyPart = new MimeBodyPart();
    
                        messageBodyPart.setDataHandler( new DataHandler( new ByteArrayDataSource( body, "text/html" ) ) );         
                        multipart.addBodyPart(messageBodyPart);
                        
                        List<String> attachments = getAttachements(variables.get("attachments"));
                        for (String attachment : attachments) {
                            MimeBodyPart attachementBodyPart = new MimeBodyPart();
                            URL attachmentUrl = getAttachemntURL(attachment);
                            String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(attachmentUrl.getFile());
                            attachementBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource( attachmentUrl.openStream(), contentType ) ));
                            String fileName = new File(attachmentUrl.getFile()).getName();
                            attachementBodyPart.setFileName(fileName);
                            attachementBodyPart.setContentID("<"+fileName+">");
    
                            multipart.addBodyPart(attachementBodyPart);
                        }
                        // Put parts in message
                        msg.setContent(multipart);
                    } else {
                        msg.setDataHandler( new DataHandler( new ByteArrayDataSource( body, "text/html" ) ) );
                    }
                    
                    msg.setSubject( subject );
                    
                    msg.setHeader( "X-Mailer", "jbpm human task service" );
                    msg.setSentDate( new Date() );

                    Transport.send(msg);

                } catch (Exception e) {
                    logger.error("Unable to send email notification due to {}", e.getMessage());
                    logger.debug("Stacktrace:", e);
                }
            }
        }
    }
	
	private String getEmailFromOrganizationEntity(UserInfo userInfo, OrganizationalEntity user) {
        if (user instanceof User) {
            return userInfo.getEmailForEntity(user);
        } else {
            return user.getId();
        }
    }

}
