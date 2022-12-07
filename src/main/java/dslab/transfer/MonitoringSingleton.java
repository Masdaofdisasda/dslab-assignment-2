package dslab.transfer;

import dslab.entity.Message;

import java.io.IOException;
import java.net.*;

public class MonitoringSingleton {
    private static volatile MonitoringSingleton INSTANCE;
    private String localHostName;
    private int localHostPort;

    private String monitoringHostName;
    private int monitoringPort;

    private boolean started;

    private DatagramSocket socket;

    private MonitoringSingleton() {
        this.started = false;
    }

    public static synchronized MonitoringSingleton getInstance() {
        if (INSTANCE == null) INSTANCE = new MonitoringSingleton();

        return INSTANCE;
    }

    public void startSocket(String monitoringHost, int monitoringPort, String hostName, int hostPort) {
        if (!this.started) {
            this.monitoringHostName = monitoringHost;
            this.monitoringPort = monitoringPort;
            this.localHostName = hostName;
            this.localHostPort = hostPort;

            try {
                this.socket = new DatagramSocket();
            } catch (SocketException e) {
                System.out.println("Error opening monitoring udp-socket!");
            }
            this.started = true;
        }
    }

    public void sendMonitoringPacket(Message message) {
        if (this.started) {
            String monitoringMessage = localHostName + ":" + localHostPort + " " + message.getSender();

            // convert the message to a byte[]
            byte[] buffer = monitoringMessage.getBytes();

            // create the datagram packet with all the necessary information
            // for sending the packet to the server
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName(monitoringHostName),
                        monitoringPort);

                socket.send(packet);

            } catch (UnknownHostException e) {
                System.out.println("Monitoring server address could not be found.");
            } catch (IOException e) {
                System.out.println("Error sending monitoring packet.");
            }
        }
    }
}
