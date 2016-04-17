package org.e5200256.filetransfer.server;

import java.io.IOException;
import java.net.ServerSocket;

class Server {

    public static void main(String[] args) {
        ServerSocket ss;
        try {
            ss = new ServerSocket(33333);

            while (true) {
                new AcceptedServerConnection(ss.accept()).start();
            }
        } catch (IOException e) {
            System.out.println("cannot create socket");
        }
    }
}
