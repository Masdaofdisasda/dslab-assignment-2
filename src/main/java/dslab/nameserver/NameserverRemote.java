package dslab.nameserver;

import dslab.entity.MailboxEntry;
import dslab.entity.NameserverEntry;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class NameserverRemote implements INameserverRemote {

    private final ConcurrentHashMap<String, MailboxEntry> addresses;
    private final ConcurrentHashMap<String, NameserverEntry> nameservers;

    public NameserverRemote() {
        addresses = new ConcurrentHashMap<>();
        nameservers = new ConcurrentHashMap<>();
    }

    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        System.out.println("Registering nameserver with domain " + domain);

        String[] domainParts = domain.split("\\.");

        if (domainParts.length == 1) {
            String name = domainParts[0];
            if (nameservers.containsKey(name)) {
                throw new AlreadyRegisteredException("Domain " + name + " already registered in nameserver!");
            }

            NameserverEntry newEntry = new NameserverEntry(name, nameserver);
            nameservers.put(name, newEntry);

            System.out.println("Domain " + name + " registered!");

        } else if (domainParts.length > 1) {

            String top = domainParts[domainParts.length - 1];
            String[] remainingParts = Arrays.copyOf(domainParts, domainParts.length - 1);
            String remaining = String.join(".", remainingParts);

            if (!nameservers.containsKey(top)){
                throw new InvalidDomainException("No nameserver for domain " + top + " is registered! Cannot register " + domain + "!");
            }

            nameservers.get(top).getRemote().registerNameserver(remaining, nameserver);
            System.out.println("Passed along registration of " + remaining  + " to nameserver " + top);

        } else throw new InvalidDomainException("Error, domain " + domain + " is invalid!");

    }

    @Override
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        return null;
    }

    @Override
    public String lookup(String username) throws RemoteException {
        System.out.println("Called lookup method");
        return username + " test";
        //return null;
    }
}
