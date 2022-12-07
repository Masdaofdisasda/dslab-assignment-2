package dslab.transfer;

import dslab.dns.DNSSingleton;
import dslab.dns.DomainNotFoundException;
import dslab.entity.Domain;
import dslab.entity.ErrorMessage;
import dslab.entity.Message;
import dslab.protocol.dmtp.DMTPClient;
import dslab.protocol.dmtp.exception.DMTPClientException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ForwardMessageRunnable implements Runnable {

    private final Message message;

    public ForwardMessageRunnable(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        DNSSingleton dns = DNSSingleton.getInstance();
        // use HashSet to not get duplicates
        HashMap<String, Domain> recipientDomains = new HashMap<>();

        // get recipient domains
        for (String recipient : message.getRecipients()) {
            try {
                String domainName = recipient.substring(recipient.indexOf("@") + 1);
                Domain domain = dns.lookupDomain(domainName);
                recipientDomains.put(domain.getName(), domain);

            } catch (DomainNotFoundException e) {
                // check if message the domain is not found of is an error message
                // sanity check - an error message should never have a non-existing domain since the recipient is the
                // sender of an originally failed message
                if (message.getClass() == Message.class) {
                    ArrayList<String> errorRecipients = new ArrayList<>();
                    errorRecipients.add(message.getSender());

                    String hostAddress = "";
                    try {
                        hostAddress = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException ex) {
                        System.out.println("Error getting local host address");
                    }

                    ErrorMessage errorMessage = new ErrorMessage(
                            errorRecipients,
                            "mailer@[" + hostAddress + "]",
                            "Error delivering message!",
                            "Error delivering message to " + recipient + "! Domain could not be found."
                    );

                    BackgroundTasksSingleton.getInstance().submit(new ForwardMessageRunnable(errorMessage));
                }
            }
        }

        // now send a message to every recipient
        for (Domain recipientDomain : recipientDomains.values()) {
            DMTPClient dmtpClient = new DMTPClient(message, recipientDomain);

            try {
                dmtpClient.sendMessage();

                // this sends a monitor package for each outgoing message
                // so if user arthur@earth.planet sends massage to trillian@earth.planet & zaphod@univer.ze
                // the user arthur will have 2 entries in the monitoring server, as well as this transfer server
                MonitoringSingleton.getInstance().sendMonitoringPacket(message);

            } catch (DMTPClientException | ConnectException e) {
                generateMessageNotDeliverableErrorMessage(message);
            } catch (IOException e) {
                //
            }
        }
    }

    private void generateMessageNotDeliverableErrorMessage(Message message) {
        // if message which is NOT already an error message couldn't be forwarded -> create error message and
        // send back to sender
        if (message.getClass() == Message.class) {

            ErrorMessage errorMessage = null;
            try {
                errorMessage = new ErrorMessage(
                        new ArrayList<String>(Collections.singletonList(message.getSender())),
                        "mailer@[" + InetAddress.getLocalHost().getHostAddress() + "]",
                        "Error forwarding message",
                        "Your message with subject " + message.getSubject() + " could not be delivered!"
                );
            } catch (UnknownHostException ex) {
                System.out.println("Error getting local host address of Transfer Server!");
            }

            BackgroundTasksSingleton.getInstance().submit(new ForwardMessageRunnable(errorMessage));
        }
    }
}
