package dslab.entity;

// used to store MailboxServers in Nameserver
// e.g. nameserver earth.planet has entry "vienna, 1.2.2.3:1223"
public class MailboxEntry {
    private String domain;
    private String address;

    public MailboxEntry(String domain, String address) {
        this.domain = domain;
        this.address = address;
    }

    public String getDomain() {
        return domain;
    }

    public MailboxEntry setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public MailboxEntry setAddress(String address) {
        this.address = address;
        return this;
    }
}
