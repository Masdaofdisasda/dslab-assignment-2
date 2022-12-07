package dslab.dns;

import dslab.entity.Domain;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.ResourceBundle;

public class DNSSingleton {
    private static volatile DNSSingleton INSTANCE;

    private final HashMap<String, Domain> domains;

    private DNSSingleton() {
        domains = new HashMap<>();
    }

    public static synchronized DNSSingleton getInstance() {
        if (INSTANCE == null) INSTANCE = new DNSSingleton();

        return INSTANCE;
    }

    public Domain lookupDomain(String name) throws DomainNotFoundException {
        Domain domain = domains.get(name);
        if (domain == null) throw new DomainNotFoundException("Domain " + name + " not found!");
        return domain;
    }

    // load all domains from file and parse them to domain objects
    public void parseDomainsFromFile() {
        ResourceBundle resources = ResourceBundle.getBundle("domains");
        Enumeration<String> keys = resources.getKeys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            String[] parts = resources.getString(key).split(":");

            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            Domain domain = new Domain(key, host, port);

            domains.put(key, domain);
        }
    }
}
