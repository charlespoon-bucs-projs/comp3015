package org.e5200256.filetransfer.client;

import org.e5200256.filetransfer.DataChannelPool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ClientAcceptedConnection {
    private Socket s;
    private Scanner sc;

    private DataChannelPool dcPool = new DataChannelPool();

    private static String workingDir = System.getProperty("user.dir");

    private InputStream in;
    private OutputStream out;
    private BufferedReader br;
    private BufferedWriter bw;
    private DataInputStream din;
    private DataOutputStream dout;

    ClientAcceptedConnection(Socket socket, Scanner scanner) throws IOException {
        s = socket;
        sc = scanner;
        br = new BufferedReader(new InputStreamReader(in = s.getInputStream()));
        bw = new BufferedWriter(new OutputStreamWriter(out = s.getOutputStream()));
        din = new DataInputStream(in);
        dout = new DataOutputStream(out);
    }

    public void work() {
        String password;
        if (System.console() == null) {
            System.out.print("Password: ");
            password = sc.nextLine();
        } else
            password = String.valueOf(System.console().readPassword("Password: "));

        try {
            bw.write(password + "\r\n");
            bw.flush();

            if (!br.readLine().equals("200")) {
                System.out.println("incorrect password");
                return;
            } else {
                System.out.println("logged in");
            }

            boolean closing = false;
            while (!closing) {
                System.out.print("> ");
                bw.write(sc.nextLine() + "\r\n");
                bw.flush();
                //noinspection ImplicitArrayToString
                String rec = new String(receiveAndUnpack(din));
                System.out.println(rec);
                parseAndDoBackground(rec);
            }
        } catch (IOException e) {
            System.out.println("read write error");
        }
    }

    private static byte[] receiveAndUnpack(DataInputStream in) throws IOException {
        byte[] ret = new byte[in.readInt()];

        in.read(ret);

        return ret;
    }

    private void parseAndDoBackground(String rec) {
        // receive files
        {
            Pattern regex = Pattern.compile("(?<=Transferring: \\().+@[\\d\\.:]+@\\d+(?=\\))");
            Matcher regexMatcher = regex.matcher(rec);

            List<String[]> found = new LinkedList<>();
            while (regexMatcher.find()) {
                found.add(regexMatcher.group().split("@"));
            }

            if (found.size() > 0) for (String[] f : found) {
                final String fileName = f[0];
                InetAddress ip;
                try {
                    ip = InetAddress.getByName(f[1]);
                } catch (UnknownHostException ignored) {
                    continue;
                }
                int port = Integer.valueOf(f[2]);

                // check exist and overwrite?
                Path writeDest = Paths.get(workingDir, fileName);
                if (writeDest.toFile().exists()) {
                    System.out.printf("File \"%s\" already exist, overwrite (y/N)? ", fileName);
                    if (!sc.nextLine().toLowerCase().equals("y"))
                        continue;
                }

                FileOutputStream fOut = null;
                try {
                    fOut = new FileOutputStream(writeDest.toAbsolutePath().toString());
                } catch (FileNotFoundException ignored) {
                }

                Socket s;
                try {
                    s = new Socket(ip, port);
                } catch (IOException e) {
                    System.out.println("Cannot open socket.");
                    continue;
                }

                dcPool.putNew(fOut, s, r -> System.out.printf("File \"%s\" successfully transferred.\r\n> ", fileName));
            }
        }
    }
}
