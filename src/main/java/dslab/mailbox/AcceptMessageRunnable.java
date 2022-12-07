package dslab.mailbox;

import dslab.entity.Message;
import dslab.protocol.dmtp.DMTP;
import dslab.protocol.dmtp.DMTPRecipientValidatorSAM;
import dslab.protocol.dmtp.exception.DMTPErrorException;
import dslab.protocol.dmtp.exception.DMTPTerminateConnectionException;
import dslab.util.WrappedSocket;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class AcceptMessageRunnable implements Runnable {
    private final WrappedSocket socket;
    private final ConcurrentHashMap<AcceptMessageRunnable, AcceptMessageRunnable> runningAcceptMessageTasks;
    private final String domain;

    private final DMTPRecipientValidatorSAM isUserFromThisDomainValidator;

    public AcceptMessageRunnable(Socket socket, ConcurrentHashMap<AcceptMessageRunnable, AcceptMessageRunnable> runningAcceptMessageTasks, String domain) throws IOException {
        this.socket = new WrappedSocket(socket);
        this.runningAcceptMessageTasks = runningAcceptMessageTasks;
        this.domain = domain;

        this.isUserFromThisDomainValidator = (String recipient) -> {
            if (recipient.endsWith(domain)) {
                String userName = recipient.substring(0, recipient.length() - (domain.length() + 1));
                if (!UserStorageSingleton.getInstance().doesUserExist(userName)) throw new DMTPErrorException("error unknown recipient");
                return true;
            }
            return false;
        };

        runningAcceptMessageTasks.put(this, this);
    }

    @Override
    public void run() {
        System.out.println("DMTP socket accepted!");
        String input;

        // init dmtp and print first message
        // validate that recipient mail ends with domain name of this server and that user exists in server
        DMTP protocol = new DMTP(isUserFromThisDomainValidator);

        socket.write(protocol.processInput(null));

        try {
            while (!socket.isClosed() && (input = socket.read()) != null) {

                try {
                    String output = protocol.processInput(input);

                    if (protocol.isMessageReadyToSend()) {

                        // message fully received, store in local data structure
                        Message message = protocol.getMessage();

                        for (String recipient : message.getRecipients()) {
                            if (isUserFromThisDomainValidator.isValidForThisDomain(recipient)) {
                                String userName = recipient.substring(0, recipient.length() - (domain.length() + 1));
                                MessageStorageSingleton.getInstance().storeMessage(userName, message);
                            }
                        }
                    }

                    socket.write(output);

                } catch (DMTPErrorException e) {
                    socket.write(e.getMessage());
                } catch (DMTPTerminateConnectionException e) {
                    socket.write(e.getMessage());
                    close();
                }
            }

        } catch (IOException e) {
            // Ignored because we cannot handle it
        }
    }

    public void close() {
        System.out.println("Closing DMAPAcceptMessageRunnable thread!");
        runningAcceptMessageTasks.remove(this);

        socket.close();
    }
}