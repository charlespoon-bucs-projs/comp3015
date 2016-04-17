package org.e5200256.filetransfer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class SingleServerSocket extends Thread {
    private final DataChannelInitializeServerSocketHouse lomo;
    private ServerSocket ss;
    private Consumer<Socket> after;
    private InetAddress dest;

    private SingleServerSocket(DataChannelInitializeServerSocketHouse lomo, int port, InetAddress dest, Consumer<Socket> after) throws IOException {
        this.lomo = lomo;
        this.ss = new ServerSocket(port);
        this.after = after;
        this.dest = dest;
    }

    public static SingleServerSocket create(DataChannelInitializeServerSocketHouse lomo, int port, InetAddress dest, Consumer<Socket> after) {
        try {
            return new SingleServerSocket(lomo, port, dest, after);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void run() {
        Socket s;
        try {
            for (s = ss.accept(); !s.getInetAddress().getHostAddress().equals(dest.getHostAddress()); s = ss.accept());
        } catch (IOException ignored) {
            return;
        }
        lomo.remove(this);
        after.accept(s);
    }
}
