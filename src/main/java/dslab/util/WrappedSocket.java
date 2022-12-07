package dslab.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class WrappedSocket {
    Socket socket;
    BufferedReader reader;
    PrintWriter writer;

    public WrappedSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public String read() throws IOException {
        return reader.readLine();
    }

    public void write(String text) {
        writer.println(text);
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignored because we cannot handle it
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public int getPort() {
        return socket.getPort();
    }
}
