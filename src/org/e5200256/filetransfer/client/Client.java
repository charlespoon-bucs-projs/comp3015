package org.e5200256.filetransfer.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

class Client {
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.print("Login host: ");
            String host = sc.nextLine();
            System.out.print("Port: ");
            int port = Integer.valueOf(sc.nextLine());

            Socket s;

            ClientAcceptedConnection accepted;
            try {
                s = new Socket(host, port);
                accepted = new ClientAcceptedConnection(s, sc);
            } catch (IOException e) {
                System.out.println("cannot create socket");
                return;
            }
            accepted.work();

            System.out.println("Exit (y/N)? ");
            if (sc.nextLine().toLowerCase().equals("y"))
                return;

            if (System.console() == null)
                System.out.println("----------");
            else {
                try {
                    if (System.getProperty("os.name").contains("Windows"))
                        Runtime.getRuntime().exec("cls");
                    else
                        Runtime.getRuntime().exec("clear");
                } catch (IOException ignored) {
                }
            }
        }
    }
}
