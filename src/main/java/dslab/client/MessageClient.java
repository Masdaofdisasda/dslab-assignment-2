package dslab.client;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

import java.io.InputStream;
import java.io.PrintStream;

public class MessageClient implements IMessageClient, Runnable {

    private final String componentId;
    private final Config config;
    private final Shell shell;
    private MessageClientDMAP2Thread dmap2Thread;


    /**
     * Creates a new client instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public MessageClient(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {
        dmap2Thread = new MessageClientDMAP2Thread(config, shell);
        dmap2Thread.start();

        shell.run();
        shell.out().println(String.format("Shutting down message-client %s SUCCESSFUL!", componentId));
    }

    @Override
    @Command
    public void inbox() {
        dmap2Thread.inbox();
    }

    @Override
    @Command
    public void delete(String id) {

    }

    @Override
    @Command
    public void verify(String id) {

    }

    @Override
    @Command
    public void msg(String to, String subject, String data) {

    }

    @Override
    @Command
    public void shutdown() {
        shell.out().println(String.format("Shutting down message-client %s!", componentId));
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
        client.run();
    }
}
