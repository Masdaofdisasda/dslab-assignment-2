package dslab.transfer;

import dslab.protocol.dmtp.DMTP;
import dslab.protocol.dmtp.exception.DMTPErrorException;
import dslab.protocol.dmtp.exception.DMTPTerminateConnectionException;
import dslab.util.WrappedSocket;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class AcceptMessageRunnable implements Runnable {
    private final WrappedSocket socket;
    private final ConcurrentHashMap<AcceptMessageRunnable, AcceptMessageRunnable> runningAcceptMessageTasks;

    public AcceptMessageRunnable(Socket socket, ConcurrentHashMap<AcceptMessageRunnable, AcceptMessageRunnable> runningAcceptMessageTasks) throws IOException {
        this.socket = new WrappedSocket(socket);
        this.runningAcceptMessageTasks = runningAcceptMessageTasks;

        runningAcceptMessageTasks.put(this, this);
    }

    @Override
    public void run() {
        System.out.println("TransferServer DMTP socket accepted!");
        String message;

        // init dmtp and print first message
        // validator for transfer returns true for every recipient
        DMTP protocol = new DMTP((String recipient) -> true);
        socket.write(protocol.processInput(null));

        try {
            while (!socket.isClosed() && (message = socket.read()) != null) {

                try {
                    String output = protocol.processInput(message);

                    //check if message is ready to send after this interaction
                    if (protocol.isMessageReadyToSend()) {
                        BackgroundTasksSingleton backgroundTasks = BackgroundTasksSingleton.getInstance();
                        backgroundTasks.submit(new ForwardMessageRunnable(protocol.getMessage()));
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
        System.out.println("Closing DMTPAcceptRunnable thread!");
        runningAcceptMessageTasks.remove(this);

        socket.close();
    }
}
