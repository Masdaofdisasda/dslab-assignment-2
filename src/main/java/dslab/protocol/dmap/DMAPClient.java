package dslab.protocol.dmap;

import dslab.protocol.dmap.exception.DMAPErrorException;
import dslab.util.WrappedSocket;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static dslab.protocol.dmap.DMAP.NUL_TERMINATOR;

public class DMAPClient {

  private final WrappedSocket socket;

  public DMAPClient(String domain, int port) throws IOException {
    socket = new WrappedSocket(new Socket(domain, port));
    if (!Objects.equals(socket.read(), "ok DMAP")) {
      throw new DMAPErrorException("Could not connect to DMAP Server with socket: " + socket);
    }
  }

  public void login(String username, String password) throws IOException {
    socket.write("login " + username + " " + password);
    if (!Objects.equals(socket.read(), "ok")){
      throw new DMAPErrorException("Could not login");
    }
  }

  public List<String> list() throws IOException {
    socket.write("list");
    List<String> messages = new ArrayList<>();
    String response = socket.read();

    if (response.equals("You have no stored messages!")) {
      messages.add(response);
      return messages;
    }

    while (response.charAt(0) != NUL_TERMINATOR) { // prevents read() from blocking indefinitely
      messages.add(response);
      response = socket.read();
    }

    return messages;
  }

  public String show(String messageId) throws IOException {
    socket.write("show " + messageId);
    StringBuilder message = new StringBuilder();
    String response = socket.read();

    while (response.charAt(0) != NUL_TERMINATOR) { // prevents read() from blocking indefinitely
      message.append(response).append('\n');
      response = socket.read();
    }

    return message.toString();
  }

  public void delete(String messageId) throws IOException {
    socket.write("delete " + messageId);
    if (!Objects.equals(socket.read(), "ok")){
      throw new DMAPErrorException("An error occurred when deleting message " + messageId);
    }
  }

  public void logout() throws IOException {
    socket.write("logout");
    if (!Objects.equals(socket.read(), "ok")){
      throw new DMAPErrorException("Could not logout");
    }
  }

  public void quit() {
    socket.close();
  }
}
