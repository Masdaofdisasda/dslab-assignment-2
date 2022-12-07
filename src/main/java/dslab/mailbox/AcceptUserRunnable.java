package dslab.mailbox;

import dslab.protocol.dmap.DMAP;
import dslab.protocol.dmap.exception.DMAPErrorException;
import dslab.protocol.dmap.exception.DMAPTerminateConnectionException;
import dslab.util.WrappedSocket;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


public class AcceptUserRunnable implements Runnable {
    private final WrappedSocket socket;
    private final ConcurrentHashMap<AcceptUserRunnable, AcceptUserRunnable> runningAccepUserTasks;


    public AcceptUserRunnable(Socket socket, ConcurrentHashMap<AcceptUserRunnable, AcceptUserRunnable> runningAccepUserTasks) throws IOException {
        this.socket = new WrappedSocket(socket);
        this.runningAccepUserTasks = runningAccepUserTasks;
    }

    @Override
    public void run() {
        System.out.println("DMAP socket accepted!");
        String input;

        // init dmtp and print first message
        // validate that recipient mail ends with domain name of this server and that user exists in server
        DMAP protocol = new DMAP();

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
        runningAccepUserTasks.remove(this);

        socket.close();
    }
}
