package dslab.entity;

import dslab.nameserver.INameserverRemote;

// used to store sub-nameservers for nameservers
// e.g. nameserver planet has entry for "earth, 1.2.3.4:1234" and "mars, 2.3.4.5:2345"
public class NameserverEntry {
    private final String zone;
    private final INameserverRemote remote;

    public NameserverEntry(String zone, INameserverRemote remote) {
        this.zone = zone;
        this.remote = remote;
    }

    public String getZone() {
        return zone;
    }

    public INameserverRemote getRemote() {
        return remote;
    }
}
