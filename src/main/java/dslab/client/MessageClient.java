package dslab.client;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.entity.Domain;
import dslab.entity.Message;
import dslab.protocol.dmap.DMAPClient;
import dslab.protocol.dmap.exception.DMAPErrorException;
import dslab.protocol.dmtp.DMTPClient;
import dslab.protocol.dmtp.exception.DMTPClientException;
import dslab.protocol.dmtp.exception.DMTPErrorException;
import dslab.util.Config;
import dslab.util.MessageHasher;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Client program for user to interact with the email system.
 */
public class MessageClient implements IMessageClient, Runnable {

  private final Config config;
  private final Shell shell;
  private DMAPClient client;
  private final Domain transferDomain;
  private final MessageHasher hasher = new MessageHasher();

  /**
   * Creates a new client instance.
   *
   * @param componentId the id of the component that corresponds to the Config resource
   * @param config      the component config
   * @param in          the input stream to read console input from
   * @param out         the output stream to write console output to
   */
  public MessageClient(String componentId, Config config, InputStream in, PrintStream out) {
    this.config = config;
    this.shell = new Shell(in, out);

    String host = config.getString("transfer.host");
    int port = config.getInt("transfer.port");
    transferDomain = new Domain("Transfer Server", host, port);

    shell.register(this);
    shell.setPrompt(componentId + "> ");
  }

  @Override
  public void run() {
    try {
      client = new DMAPClient(config.getString("mailbox.host"), config.getInt("mailbox.port"));
      client.login(config.getString("mailbox.user"), config.getString("mailbox.password"));
    } catch (IOException e) {
      throw new DMAPErrorException("Could not login with reason: " + e.getMessage());
    }
    shell.run();

    shell.out().println("Bye!");
  }

  /**
   * Obtains a list of all messages from the logged-in user's mailbox.
   */
  @Override
  @Command
  public void inbox() {
    try {
      // Obtain overview list of messages from DMAP Client
      List<String> messageList = client.list();

      // Check if client has no messages
      if (messageList.size() == 1 && Objects.equals(messageList.get(0), "You have no messages.")) {
        shell.out().println(messageList.get(0));
        return;
      }

      // Process messages
      // Extract message ids from overview list
      List<String> messageIds = messageList.stream()
          .map(message -> message.substring(0, message.indexOf(" ")))
          .collect(Collectors.toList());

      // Get message details (show) from DMAP Client
      List<Message> messages = messageIds.stream().map(messageId -> {
        Message message = new Message();
        try {
          message = extractMessage(client.show(messageId));
          message.setId(messageId);
        } catch (IOException e) {
          //
        }
        return message;
      }).collect(Collectors.toList());

      messages.forEach(message -> shell.out().println(message));

    } catch (IOException e) {
      shell.err().println(e.getMessage());
    }
  }

  /**
   * Creates an instance of Message from a message string.
   *
   * @param message the message string to be parsed.
   * @return instance of Message.
   */
  private static Message extractMessage(String message) {
    String[] lines = message.split("\n");
    Message msg = new Message();

    // Extract message details
    // from ...
    msg.setSender(lines[0].substring(5));
    // to ...
    msg.setRecipients(new ArrayList<>(List.of((lines[1].substring(3)))));
    // subject ...
    msg.setSubject(lines[2].substring(8));
    // data ...
    msg.setData(lines[3].substring(5));
    // hash ...
    if (lines.length == 5) msg.setHash(lines[4].substring(5));

    return msg;
  }

  /**
   * Deletes a message from the logged-in user's mailbox.
   *
   * @param id the message id to delete
   */
  @Override
  @Command
  public void delete(String id) {
    try {
      client.delete(id);
      shell.out().println("ok");
    } catch (IOException e) {
      shell.err().println(e.getMessage());
    } catch (DMAPErrorException e) {
      shell.err().println("error");
    }
  }

  @Override
  @Command
  public void verify(String messageId) {
    try {
      String message = client.show(messageId);
      Message msg = extractMessage(message);
      if (msg.getHash() != null) {
        shell.out().println(
            hasher.verify(msg)
            ? "ok"
            : "error");

      } else {
        shell.out().println("error sender did not specify a hash");
      }
    } catch (IOException e) {
      shell.err().println(e.getMessage());
    }
  }

  /**
   * Sends a message to someone
   *
   * @param to      comma separated list of recipients
   * @param subject the message subject
   * @param data    the message data
   */
  @Override
  @Command
  public void msg(String to, String subject, String data) {

    Message message = new Message();
    message.setSender(config.getString("transfer.email"));
    message.setRecipients(extractRecipients(to));
    message.setData(extractData(data));
    message.setSubject(extractSubject(subject));
    message.setHash(hasher.encodeHash(hasher.calculateHash(message)));

    DMTPClient dmtpClient = new DMTPClient(message, transferDomain);
    try {
      dmtpClient.sendMessage();
      shell.out().println("ok");
    } catch (DMTPClientException | ConnectException e) {
      shell.err().println(e.getMessage());
    } catch (IOException e) {
      //
    }

  }

  @Override
  @Command
  public void shutdown() {
    try {
      client.logout();
      client.quit();
    } catch (IOException e) {
      //
    }
    throw new StopShellException();
  }

  private String extractSubject(String subject) {
    if (subject.length() <= 2) {
      throw new DMTPErrorException("error no subject");
    }

    return subject.replace("\"", "");
  }

  private String extractData(String data) {
    if (data.length() <= 2) {
      throw new DMTPErrorException("error no data");
    }

    return data.replace("\"", "");
  }

  private ArrayList<String> extractRecipients(String recipients) {
    String[] splitString = recipients.split(",");

    return Arrays.stream(splitString)
        .sequential()
        .map(String::trim).collect(Collectors.toCollection(ArrayList::new));
  }

  public static void main(String[] args) throws Exception {
    IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
    client.run();
  }
}
