package dslab.monitoring;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;

public class MonitoringServer implements IMonitoringServer {

    private final Config config;
    private final Shell shell;

    private ListenerThread listenerThread;

    private final HashMap<String, Integer> users;
    private final HashMap<String, Integer> servers;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.shell = new Shell(in, out);

        this.users = new HashMap<>();
        this.servers = new HashMap<>();

        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {
        listenerThread = new ListenerThread(config, users, servers);
        listenerThread.start();

        shell.run();

        System.out.println("Bye!");
    }

    @Override
    @Command
    public void addresses() {
        for (String user : users.keySet()) {
            shell.out().println(user + " " + users.get(user));
        }
    }

    @Override
    @Command
    public void servers() {
        for (String server : servers.keySet()) {
            shell.out().println(server + " " + servers.get(server));
        }
    }

    @Override
    @Command
    public void shutdown() {
        System.out.println("Shutting down Monitoring server");

        listenerThread.shutdown();

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
