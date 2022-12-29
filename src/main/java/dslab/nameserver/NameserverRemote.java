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

        // split domain into its zones; earth.planet -> ["earth", "planet"]
        String[] domainParts = domain.split("\\.");

        // only one zone in domain -> nameserver needs to be saved here as "child"
        if (domainParts.length == 1) {
            if (nameservers.containsKey(domain)) {
                throw new AlreadyRegisteredException("Nameserver " + domain + " already registered in nameserver!");
            }

            NameserverEntry newEntry = new NameserverEntry(domain, nameserver);
            nameservers.put(domain, newEntry);

            System.out.println("Nameserver " + domain + " registered!");

        } else if (domainParts.length > 1) {
            // more than one zone left -> pass registration further down

            String top = domainParts[domainParts.length - 1];
            String[] remainingParts = Arrays.copyOf(domainParts, domainParts.length - 1);
            String remainingDomain = String.join(".", remainingParts);

            if (!nameservers.containsKey(top)) {
                throw new InvalidDomainException("No nameserver for domain " + top + " is registered! Cannot register " + domain + "!");
            }

            nameservers.get(top).getRemote().registerNameserver(remainingDomain, nameserver);
            System.out.println("Passed along nameserver registration of " + remainingDomain + " to nameserver " + top);

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

            if (!nameservers.containsKey(top)) {
                throw new InvalidDomainException("No nameserver for domain " + top + " is registered! Cannot register " + domain + "!");
            }

            nameservers.get(top).getRemote().registerMailboxServer(remaining, address);
            System.out.println("Passed along mailserver registration of " + remaining + " to nameserver " + top);

        } else throw new InvalidDomainException("Error, domain " + domain + " is invalid!");
    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        if (nameservers.containsKey(zone)) return nameservers.get(zone).getRemote();
        else throw new RemoteException("Nameserver for zone " + zone + " not found!");
    }

    @Override
    public String lookup(String domain) throws RemoteException {
        if (addresses.containsKey(domain)) return addresses.get(domain).getAddress();
        else throw new RemoteException("No address for domain " + domain + " found!");
    }

    public List<MailboxEntry> getAddressesList() {
        return new ArrayList<>(addresses.values());
    }

    public List<NameserverEntry> getNameserversList() {
        return new ArrayList<>(nameservers.values());
    }
}
