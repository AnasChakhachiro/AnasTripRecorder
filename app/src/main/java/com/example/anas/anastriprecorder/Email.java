
package com.example.anas.anastriprecorder;

import android  .os         .AsyncTask                  ;
import android  .text       .TextUtils                  ;
import java     .util       .Date                       ;
import java     .util       .Properties                 ;
import javax    .activation .CommandMap                 ;
import javax    .activation .DataHandler                ;
import javax    .activation .FileDataSource             ;
import javax    .activation .MailcapCommandMap          ;
import javax    .mail       .BodyPart                   ;
import javax    .mail       .Multipart                  ;
import javax    .mail       .PasswordAuthentication     ;
import javax    .mail       .Session                    ;
import javax    .mail       .Transport                  ;
import javax    .mail       .internet  .InternetAddress ;
import javax    .mail       .internet  .MimeBodyPart    ;
import javax    .mail       .internet  .MimeMessage     ;
import javax    .mail       .internet  .MimeMultipart   ;


public class Email extends javax.mail.Authenticator{
    private String      user        ;
    private String      pass        ;
    private String[]    to          ;
    private String      from        ;
    private String      port        ;
    private String      sport       ;
    private String      host        ;
    private String      subject     ;
    private String      body        ;
    private boolean     auth        ;
    private boolean     debuggable  ;
    private Multipart   multipart   ;
    private MimeMessage msg         ;

    public Email(String user, String pass) {
        this.user = user;
        this.pass = pass;

        host        = "smtp.gmail.com"   ; // default smtp server
        port        = "465"              ; // default smtp port
        sport       = "465"              ; // default socketfactory port
        debuggable  = false              ; // debug mode on or off - default off
        auth        = true               ; // smtp authentication - default on
        multipart   = new MimeMultipart();

        // There is something wrong with MailCap, javamail can not find a handler for the multipart/mixed part, so this bit needs to be added.
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html"          );
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml"            );
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain"        );
        mc.addMailcap("multipart;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed"    );
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);
    }


    public boolean sendEmailInBackground(){
        try {
            Properties props = setProperties();
            Session session = Session.getInstance(props, this);
            if (!user.equals("") && !pass.equals("") && to.length > 0 && !from.equals("") && !subject.equals("") && !body.equals("")) {
                msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(from));
                InternetAddress[] addressTo = new InternetAddress[to.length];
                for (int i = 0; i < to.length; i++) {
                    if (!to[i].equals(""))
                        addressTo[i] = new InternetAddress(to[i]);
                }
                msg.setRecipients(MimeMessage.RecipientType.TO, addressTo);
                msg.setSubject(subject);
                msg.setSentDate(new Date());
                // setup message body
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(body);
                multipart.addBodyPart(messageBodyPart);
                // Put parts in message
                msg.setContent(multipart);
                new sendEmailAsyncTask().execute();
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public class sendEmailAsyncTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Transport.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }


    public void addAttachment(String filename) throws Exception {
        BodyPart messageBodyPart = new MimeBodyPart();
        FileDataSource source =  new FileDataSource(filename);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);
    }

    @Override
    protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, pass);
    }

    private Properties setProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        if(debuggable)      {props.put(  "mail.debug"  ,"true");}
        if(   auth   )      {props.put("mail.smtp.auth","true");}
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.socketFactory.port", sport);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        return props;
    }

    public static boolean isValidEmail(CharSequence email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void setBody(String body) {this.body = body;}

    public void setSubject(String subject) {this.subject = subject;}

    public void setFrom(String from) {this.from = from;}

    public void setTo(String[] to) {this.to = to;}


    public static void sendEmailAfterRegistrationOrUpdate(User user) {
        {
            Email mail = new Email("myEmail@gmail.com", "myPassword");
            String updatedName = user.getName();
            String updatedEmail = user.getEmail();
            String nPW = user.getPassword();
            String updatedRecoveryEmail = user.getRecoveryEmail();
            mail.setTo(new String[]{updatedRecoveryEmail, updatedEmail});
            mail.setSubject("tripRecorder Account Data");
            mail.setFrom("tripRecorder");
            mail.setBody("Dear " + updatedName + ",\n" +
                    "Your current account data are:\n\n" +

                    "Name is: " + updatedName + "\n" +
                    "Account email is: " + updatedEmail + "\n" +
                    "Account password is: " + nPW.substring(0,2)+"**************"+nPW.substring(nPW.length()-1)+"\n" +
                    "Recovery email is: " + updatedRecoveryEmail + "\n\n" +

                    "If you have any other questions, you can reach us at:\n " +
                    "Tel: +492222555588\tEmail: triprecordermailservice@gmail.com\n" +
                    "Thank you for using TripRecorder\n\n" +

                    "Service team\n" +
                    "Anas GmbH");
            mail.sendEmailInBackground();
        }
    }


}