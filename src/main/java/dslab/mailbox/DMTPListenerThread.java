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

public class DMTPListenerThread extends Thread {

    private final Config config;
    private final Shell shell;
    private ServerSocket serverSocket;
    private final AtomicBoolean stopFlag;

    private final ExecutorService acceptMessageThreadPool;
    private final ConcurrentHashMap<AcceptMessageRunnable, AcceptMessageRunnable> runningAcceptMessageTasks;


    public DMTPListenerThread(Config config, Shell shell) {
        this.config = config;
        this.shell = shell;
        this.stopFlag = new AtomicBoolean(false);

        this.acceptMessageThreadPool = Executors.newCachedThreadPool();
        this.runningAcceptMessageTasks = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        int port = config.getInt("dmtp.tcp.port");

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            this.serverSocket = serverSocket;

            Socket clientSocket;

            while (!stopFlag.get()) {
                try {
                    clientSocket = serverSocket.accept();

                    AcceptMessageRunnable worker = new AcceptMessageRunnable(clientSocket, runningAcceptMessageTasks, config.getString("domain"));
                    acceptMessageThreadPool.submit(worker);

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
        shell.out().println("Stopping all DMTPListenerThreads!");

        for (AcceptMessageRunnable thread : runningAcceptMessageTasks.keySet()) {
            thread.close();
        }

        acceptMessageThreadPool.shutdown();

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                shell.out().println("Error while closing server socket: " + e.getMessage());
            }
        }

        stopFlag.set(true);

        shell.out().println("All DMTPListenerThreads stopped!");
    }
}
