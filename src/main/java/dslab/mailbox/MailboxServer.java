package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.nameserver.AlreadyRegisteredException;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.InvalidDomainException;
import dslab.util.Config;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MailboxServer implements IMailboxServer, Runnable {

    private final Config config;
    private final Shell shell;

    private DMTPListenerThread dmtpListenerThread;
    private DMAPListenerThread dmapListenerThread;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.shell = new Shell(in, out);

        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {
        // register at root nameserver
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1",13159);
            INameserverRemote rootNameserver = (INameserverRemote) registry.lookup("root-nameserver");

            String address = InetAddress.getLocalHost().getHostAddress() + ':' + config.getInt("dmtp.tcp.port");
            rootNameserver.registerMailboxServer(config.getString("domain"), address);

            System.out.println("Successfully registered in DNS!");

        } catch (RemoteException e) {
            throw new RuntimeException("Error while obtaining registry/server-remote-object.", e);
        } catch (NotBoundException e) {
            throw new RuntimeException("Error while looking for server-remote-object.", e);
        } catch (AlreadyRegisteredException e) {
            throw new RuntimeException("Error, mailboxserver with this domain is already registered" + e);
        } catch (InvalidDomainException e) {
            throw new RuntimeException("Error, domain " + config.getString("domain") + " is invalid!" + e);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error, local host-address could not be found!" + e);
        }

        // init UserStorage
        String userConfigName = config.getString("users.config");
        UserStorageSingleton.getInstance().parseUsersFromFile(userConfigName.substring(0, userConfigName.length() - ".properties".length())); // cut .properties from filename

        dmtpListenerThread = new DMTPListenerThread(config, shell);
        dmtpListenerThread.start();

        dmapListenerThread = new DMAPListenerThread(config, shell);
        dmapListenerThread.start();

        shell.run();
    }

    @Override
    @Command
    public void shutdown() {
        shell.out().println("Shutting down Mailbox-Server!");

        dmtpListenerThread.shutdown();
        dmapListenerThread.shutdown();

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
