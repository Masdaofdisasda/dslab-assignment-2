package dslab.monitoring;

import dslab.util.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ListenerThread extends Thread {
    private final Config config;
    private final AtomicBoolean stopFlag;

    private DatagramSocket socket;

    private final HashMap<String, Integer> users;
    private final HashMap<String, Integer> servers;


    public ListenerThread(Config config, HashMap<String, Integer> users, HashMap<String, Integer> servers) {
        this.config = config;
        this.stopFlag = new AtomicBoolean(false);

        this.users = users;
        this.servers = servers;
    }

    @Override
    public void run() {

        try {
            // constructs a datagram socket and binds it to the specified port
            socket = new DatagramSocket(config.getInt("udp.port"));

            System.out.println("Monitoring listener Thread started...");

            byte[] buffer;
            DatagramPacket packet;

            while (!stopFlag.get()) {
                buffer = new byte[1024];
                // create a datagram packet of specified length (buffer.length)
                packet = new DatagramPacket(buffer, buffer.length);

                // wait for incoming packets from client
                socket.receive(packet);
                // get the data from the packet
                String request = new String(packet.getData());

                // split into server and user parts
                String[] parts = request.split(" ");

                if (parts.length == 2) {
                    String server = parts[0].trim();
                    String user = parts[1].trim();

                    if (!users.containsKey(user)) users.put(user, 0);
                    Integer userCount = users.get(user);
                    users.put(user, userCount + 1);

                    if (!servers.containsKey(server)) servers.put(server, 0);
                    Integer serverCount = servers.get(server);
                    servers.put(server, serverCount + 1);
                }
            }

        } catch (SocketException e) {
            // this will be reached when socket gets closed on shutdown

        } catch (IOException e) {
            throw new RuntimeException("Cannot listen on UDP port.", e);
        }
    }

    public void shutdown() {
        socket.close();
        stopFlag.set(true);
    }
}
