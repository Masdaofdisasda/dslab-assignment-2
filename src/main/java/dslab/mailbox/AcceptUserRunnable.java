package dslab.mailbox;

import dslab.protocol.dmap.DMAP;
import dslab.protocol.dmap.exception.DMAPErrorException;
import dslab.protocol.dmap.exception.DMAPTerminateConnectionException;
import dslab.util.Config;
import dslab.util.WrappedSocket;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


public class AcceptUserRunnable implements Runnable {

    private final String componentId;
    private final Config config;
    private final WrappedSocket socket;
    private final ConcurrentHashMap<AcceptUserRunnable, AcceptUserRunnable> runningAcceptUserTasks;


    public AcceptUserRunnable(String componentId, Config config, Socket socket, ConcurrentHashMap<AcceptUserRunnable, AcceptUserRunnable> runningAcceptUserTasks) throws IOException {
        this.componentId = componentId;
        this.config = config;
        this.socket = new WrappedSocket(socket);
        this.runningAcceptUserTasks = runningAcceptUserTasks;
    }

    @Override
    public void run() {
        System.out.println("DMAP socket accepted!");
        String input;

        // init dmtp and print first message
        // validate that recipient mail ends with domain name of this server and that user exists in server
        DMAP protocol = new DMAP(componentId);

        socket.write(protocol.processInput(null));

        try {
            while (!socket.isClosed() && (input = socket.read()) != null) {

                try {
                    String output = protocol.processInput(input);

                    socket.write(output);

                } catch (DMAPErrorException e) {
                    socket.write(e.getMessage());
                } catch (DMAPTerminateConnectionException e) {
                    socket.write(e.getMessage());
                    socket.close();
                }
            }

        } catch (IOException e) {
            // Ignored because we cannot handle it
        }
    }

    public void close() {
        System.out.println("Closing AcceptUserRunnable thread!");
        runningAcceptUserTasks.remove(this);

        socket.close();
    }
}
