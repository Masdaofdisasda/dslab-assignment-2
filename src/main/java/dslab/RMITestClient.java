package dslab;

import dslab.nameserver.AlreadyRegisteredException;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.InvalidDomainException;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMITestClient {

    public static void main(String[] args) {
        System.out.println("starting test client");

        try {
            // obtain registry that was created by the server
            Registry registry = LocateRegistry.getRegistry(
                    "127.0.0.1",
                    13159
            );

            // look for the bound server remote-object implementing the IServerRemote interface
            INameserverRemote server = (INameserverRemote) registry.lookup("root-nameserver");

            server.registerMailboxServer("vienna.earth.planet", "fener.com:1234");

        } catch (RemoteException e) {
            throw new RuntimeException("Error while obtaining registry/server-remote-object.", e);
        } catch (NotBoundException e) {
            throw new RuntimeException("Error while looking for server-remote-object.", e);
        } catch (AlreadyRegisteredException e) {
            throw new RuntimeException(e);
        } catch (InvalidDomainException e) {
            throw new RuntimeException(e);
        }
    }
}
