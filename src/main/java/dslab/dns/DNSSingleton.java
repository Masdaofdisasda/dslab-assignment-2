package dslab.dns;

import dslab.entity.Domain;
import dslab.nameserver.INameserverRemote;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class DNSSingleton {
    private static volatile DNSSingleton INSTANCE;

    private String registryHost;
    private int registryPort;
    private String rootId;
    private INameserverRemote rootNameserverRemote;

    private DNSSingleton() {}

    public static synchronized DNSSingleton getInstance() {
        if (INSTANCE == null) INSTANCE = new DNSSingleton();

        return INSTANCE;
    }

    public Domain lookupDomain(String domain) throws DomainNotFoundException {
        // if rootNameserverRemote has not been fetched yet get it
        if (rootNameserverRemote == null) {
            try {
                Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
                rootNameserverRemote = (INameserverRemote) registry.lookup(rootId);
            } catch (RemoteException e) {
                throw new RuntimeException("Error while obtaining registry/server-remote-object.", e);
            } catch (NotBoundException e) {
                throw new RuntimeException("Error while looking for server-remote-object.", e);
            }
        }

        String[] domainParts = domain.split("\\.");
        INameserverRemote nameServerRemote = rootNameserverRemote;

        // iteratively get nameserverRemotes
        for (int i = domainParts.length - 1; i > 0; i--) {
            try {
                nameServerRemote = nameServerRemote.getNameserver(domainParts[i]);
            } catch (RemoteException e) {
                throw new RuntimeException("Error while getting remote nameserver" + e);
            }
        }

        try {
            String address = nameServerRemote.lookup(domainParts[0]);
            String[] addressParts = address.split(":");

            return new Domain(domain, addressParts[0], Integer.parseInt(addressParts[1]));

        } catch (RemoteException e) {
            throw new RuntimeException("Error while looking up address on namerserveremote" + e);
        }
    }

    public void setup(String registryHost, int registryPort, String rootId) {
        this.registryHost = registryHost;
        this.registryPort = registryPort;
        this.rootId = rootId;
    }
}
