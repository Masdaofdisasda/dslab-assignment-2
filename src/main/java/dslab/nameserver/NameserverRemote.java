package dslab.nameserver;

import dslab.entity.MailboxEntry;
import dslab.entity.NameserverEntry;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            if (nameservers.containsKey(domain)) {
                throw new AlreadyRegisteredException("Nameserver " + domain + " already registered in nameserver!");
            }

            NameserverEntry newEntry = new NameserverEntry(domain, nameserver);
            nameservers.put(domain, newEntry);

            System.out.println("Nameserver " + domain + " registered!");

        } else if (domainParts.length > 1) {

            String top = domainParts[domainParts.length - 1];
            String[] remainingParts = Arrays.copyOf(domainParts, domainParts.length - 1);
            String remaining = String.join(".", remainingParts);

            if (!nameservers.containsKey(top)){
                throw new InvalidDomainException("No nameserver for domain " + top + " is registered! Cannot register " + domain + "!");
            }

            nameservers.get(top).getRemote().registerNameserver(remaining, nameserver);
            System.out.println("Passed along nameserver registration of " + remaining  + " to nameserver " + top);

        } else throw new InvalidDomainException("Error, domain " + domain + " is invalid!");
    }

    @Override
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        System.out.println("Registering mailserver with domain " + domain + " and address " + address);

        String[] domainParts = domain.split("\\.");

        if (domainParts.length == 1) {
            if (addresses.containsKey(domain)) {
                throw new AlreadyRegisteredException("Mailserver " + domain + " already registered in nameserver!");
            }

            MailboxEntry newEntry = new MailboxEntry(domain, address);
            addresses.put(domain, newEntry);

            System.out.println("Mailserver " + domain + " registered!");

        } else if (domainParts.length > 1) {

            String top = domainParts[domainParts.length - 1];
            String[] remainingParts = Arrays.copyOf(domainParts, domainParts.length - 1);
            String remaining = String.join(".", remainingParts);

            if (!nameservers.containsKey(top)){
                throw new InvalidDomainException("No nameserver for domain " + top + " is registered! Cannot register " + domain + "!");
            }

            nameservers.get(top).getRemote().registerMailboxServer(remaining, address);
            System.out.println("Passed along mailserver registration of " + remaining  + " to nameserver " + top);

        } else throw new InvalidDomainException("Error, domain " + domain + " is invalid!");
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

    public List<MailboxEntry> getAddressesList() {
        return new ArrayList<>(addresses.values());
    }

    public List<NameserverEntry> getNameserversList() {
        return new ArrayList<>(nameservers.values());
    }
}
