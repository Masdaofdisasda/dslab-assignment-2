package dslab.nameserver;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

public class Nameserver implements INameserver {

    private final Config config;
    private final Shell shell;
    private Registry registry;

    private INameserverRemote nameserverRemote;
    private INameserverRemote stub;


    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public Nameserver(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        shell = new Shell(in, out);

        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {

        try {
            nameserverRemote = new NameserverRemote();

            stub = (INameserverRemote) UnicastRemoteObject.exportObject(nameserverRemote, 0);
            System.out.println("Remote Object epxorted!");

            if (config.containsKey("domain")) {
                // zone nameserver
                Registry registry = LocateRegistry.getRegistry(
                        config.getString("registry.host"),
                        config.getInt("registry.port")
                );

                try {
                    INameserverRemote rootNameserverRemote = (INameserverRemote) registry.lookup(config.getString("root_id"));
                    rootNameserverRemote.registerNameserver(config.getString("domain"), nameserverRemote);
                    System.out.println("Nameserver registered at parent/root nameserver");
                } catch (NotBoundException e) {
                    throw new RuntimeException("Error while looking up remote object of root ns!" + e);
                } catch (AlreadyRegisteredException e) {
                    System.out.println("Error while registering nameserver. Server is already registered!" + e);
                } catch (InvalidDomainException e) {
                    System.out.println("Error while registering nameserver. Invalid domain!" + e);
                }


            } else {
                // root nameserver
                // create and export the registry instance on localhost at the specified port
                registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
                registry.bind(config.getString("root_id"), stub);
                System.out.println("Remote Object bound!");
            }

        } catch (RemoteException e) {
            throw new RuntimeException("Error while starting server.", e);
        } catch (AlreadyBoundException e) {
            throw new RuntimeException("Error while binding remote object to registry.", e);
        }

        shell.run();

        System.out.println("Bye!");
    }

    @Override
    @Command
    public void nameservers() {
        System.out.println("printing nameservers");
        // TODO
    }

    @Override
    @Command
    public void addresses() {
        System.out.println("printing addresses");
        // TODO
    }

    @Override
    @Command
    public void shutdown() {
        System.out.println("Shutting down...");

        try {
            // unexport the previously exported remote object
            UnicastRemoteObject.unexportObject(nameserverRemote, true);
            System.out.println("Remote Object unexported");
        } catch (NoSuchObjectException e) {
            System.err.println("Error while unexporting object: " + e.getMessage());
        }

        if (!config.containsKey("domain")) {
            try {
                // unbind the remote object so that a client can't find it anymore
                registry.unbind(config.getString("root_id"));
                System.out.println("Remote Object unbound from registry");
            } catch (Exception e) {
                System.err.println("Error while unbinding object: " + e.getMessage());
            }
        }

        System.out.println("Shutdown complete");
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        INameserver component = ComponentFactory.createNameserver(args[0], System.in, System.out);
        component.run();
    }
}
