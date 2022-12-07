package dslab.transfer;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.dns.DNSSingleton;
import dslab.util.Config;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

// why extend Runnable here? ITransferServer already extends Runnable
public class TransferServer implements ITransferServer, Runnable {

    private final Config config;
    private final Shell shell;

    private ListenerThread listenerThread;


    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.shell = new Shell(in, out);

        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {
        // init DNS
        DNSSingleton.getInstance().parseDomainsFromFile();

        // init Monitoring
        String localHost = "";
        try {
            localHost = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("Error getting local host address!");
        }
        MonitoringSingleton.getInstance().startSocket(
                config.getString("monitoring.host"),
                config.getInt("monitoring.port"),
                localHost,
                config.getInt("tcp.port")
        );

        listenerThread = new ListenerThread(config, shell);
        listenerThread.start();

        shell.run();

        System.out.println("Bye!");
    }

    @Override
    @Command
    public void shutdown() {
        listenerThread.shutdown();

        // shutdown background tasks thread pool
        BackgroundTasksSingleton.getInstance().shutdown();

        System.out.println("Transfer Server Shutdown");

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

}
