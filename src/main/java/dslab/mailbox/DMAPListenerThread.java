package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.util.Config;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DMAPListenerThread extends Thread {

    private final Config config;
    private final Shell shell;
    private ServerSocket serverSocket;
    private final AtomicBoolean stopFlag;

    private final ExecutorService acceptUserThreadPool;
    private final ConcurrentHashMap<AcceptUserRunnable, AcceptUserRunnable> runningAccepUserTasks;


    public DMAPListenerThread(Config config, Shell shell) {
        this.config = config;
        this.shell = shell;
        this.stopFlag = new AtomicBoolean(false);

        this.acceptUserThreadPool = Executors.newCachedThreadPool();
        this.runningAccepUserTasks = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        int port = config.getInt("dmap.tcp.port");

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            this.serverSocket = serverSocket;

            Socket clientSocket;

            while (!stopFlag.get()) {
                try {
                    clientSocket = serverSocket.accept();

                    AcceptUserRunnable worker = new AcceptUserRunnable(clientSocket, runningAccepUserTasks);
                    acceptUserThreadPool.submit(worker);

                } catch (SocketException e) {
                    // this exception will occur when closing socket because of server shutdown

                } catch (IOException e) {
                    shell.out().println("I/O error: " + e);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        shell.out().println("Shutting down DMAPListenerThread...");

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error while closing server socket: " + e.getMessage());
            }
        }

        stopFlag.set(true);
    }
}
