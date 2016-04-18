package org.e5200256.filetransfer.server;

import org.e5200256.filetransfer.CommandArguments;
import org.e5200256.filetransfer.DataChannelInitializeServerSocketHouse;
import org.e5200256.filetransfer.DataChannelPool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

class AcceptedServerConnection extends Thread {
    private Socket s;

    private InputStream in;
    private OutputStream out;

    private DataInputStream din;
    private DataOutputStream dout;

    private BufferedReader br;
    private BufferedWriter bw;

    private File currDir;
    private File rootDir;
    private Map<String, File> currDirFiles;

    private DataChannelPool dtPool;

    private DataChannelInitializeServerSocketHouse serverSocketHouse;

    public AcceptedServerConnection(Socket s) {
        this.s = s;
        try {
            in = s.getInputStream();
            out = s.getOutputStream();

            din = new DataInputStream(in);
            dout = new DataOutputStream(out);

            br = new BufferedReader(new InputStreamReader(in));
            bw = new BufferedWriter(new OutputStreamWriter(out));
        } catch (IOException e) {
            System.out.println("Cannot create i/o streams");
        }
        currDir = Paths.get(System.getProperty("user.dir")).toFile();
        rootDir = currDir;
        dtPool = new DataChannelPool();
        serverSocketHouse = new DataChannelInitializeServerSocketHouse();
    }

    @Override
    public void run() {
        System.out.printf("established connection (%s:%d)\r\n", s.getInetAddress(), s.getPort());

        try {
            if (!"password".equals(br.readLine())) {
                bw.write("Invalid password, rejected.\r\n");
                return;
            } else {
                bw.write("200\r\n");
            }
            bw.flush();

            currDirFiles = getFileList(currDir.toPath());

            boolean exiting = false;
            while (!exiting) {
                String raw = br.readLine();
                System.out.printf("<< (%s:%d) %s\r\n", s.getInetAddress().getHostAddress(), s.getPort(), raw);
                CommandArguments ca = CommandArguments.CreateWithSpliting(raw);

                if (ca.getCommand().equals("ls")) {
                    //noinspection ConfusingArgumentToVarargsMethod
                    String r = String.join("\r\n", currDirFiles.values().stream().map(AcceptedServerConnection::formatFileProperties).toArray(String[]::new));
                    packetAndSend(r.getBytes(), r.getBytes().length);
                } else if (ca.getCommand().equals("cd")) {
                    String arg = ca.get(0);
                    if (arg.equals("/")) {
                        currDir = rootDir;
                        currDirFiles = getFileList(currDir.toPath());
                    } else if (arg.equals("..") && !currDir.toPath().equals(rootDir.toPath())) {
                        currDir = currDir.getParentFile();
                        currDirFiles = getFileList(currDir.toPath());
                    } else if (arg.length() > 0 && !arg.equals(".")) {
                        File target = currDirFiles.get(arg);
                        if (target.isDirectory()) {
                            currDir = target;
                            currDirFiles = getFileList(currDir.toPath());
                        }
                    }

                    String vPath = currDir.getAbsolutePath().substring(rootDir.getAbsoluteFile().toString().length()).replace('\\', '/');
                    if (vPath.length() == 0) vPath = "/";

                    packetAndSend("Changed directory to \"%s\"", vPath);
                } else if (ca.getCommand().equals("get")) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < ca.length(); i++) {
                        String fn = ca.get(i);
                        File f = Paths.get(currDir.getAbsolutePath(), fn).toFile();
                        if (!f.exists() || !f.isFile()) {
                            sb.append("\r\n\"");
                            sb.append(fn);
                            sb.append("\" does not exist");
                        } else {
                            FileInputStream fs = new FileInputStream(f);

                            InetAddress localAddress = s.getLocalAddress();
                            int portNumber = generateNewFreePortNumber();

                            InetAddress remoteAddress = s.getInetAddress();

                            sb.append(String.format("\r\nTransferring: (%s@%s@%d)", f.getName(), localAddress.getHostAddress(), portNumber));

                            System.out.printf("(file:%s) >> (%s:%d) *Start*\r\n", f.getName(), s.getInetAddress().getHostAddress(), s.getPort());
                            serverSocketHouse.putNewSingleServerSocket(
                                    portNumber,
                                    remoteAddress, s -> dtPool.putNew(fs, s,
                                            r -> System.out.printf("(file:%s) >> (%s:%d) *Done*\r\n",
                                                    f.getName(),
                                                    s.getInetAddress().getHostAddress(),
                                                    s.getPort())
                                    )
                            );
                        } // TO!DO: dir
                    }

                    String output = sb.toString();
                    if (output.length() > 4) output = output.substring(2); // "CRLF" = 2
                    packetAndSend(output);

//                    new DataChannel<FileInputStream>(new FileInputStream())
                } else if (ca.getCommand().equals("quit")) {
                    packetAndSend("Bye.");
                    exiting = true;
                } else if (ca.getCommand().equals("help")) {
                    String content =
                            "ls              List contents in current directory\r\n" +
                                    "cd [dir]        Change directory\r\n" +
                                    "get {[file]+}   Download a file from remote.\r\n" +
                                    "help            Show the list of the commands available and their descriptions.\r\n" +
                                    "quit            Ends the session.";
                    packetAndSend(content);
                } else {
                    packetAndSend("Unrecognised command");
                }
            }

            if (dtPool.size() > 0) {
                dtPool.getAll().stream().forEach((dc) -> {
                    try {
                        if (interrupted())
                            dc.interrupt();
                        else
                            dc.join();
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted on waiting data channel \"%s\" to be completed. Will kill all remainings.");
                    }
                });
            }
        } catch (IOException e) {
            System.out.printf("(%s:%d) IO error: %s\r\n", s.getInetAddress().getHostAddress(), s.getPort(), e.getMessage());
        }
    }

    private static String formatFileProperties(File file) {
        // -rw-r--r-- 1 ftp ftp       52620388 Sep 12  2013 Delight.zip
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd  yyyy", Locale.ENGLISH);
        return String.format("%s%s%s%s 1 ftp ftp %12d %s %s",
                file.isDirectory() ? "d" : "-",
                (file.canRead() ? "r" : "-") + (file.canWrite() ? "w" : "-") + (file.canExecute() ? "x" : "-"),
                (file.canRead() ? "r" : "-") + "-" + (file.canExecute() ? "x" : "-"),
                (file.canRead() ? "r" : "-") + "-" + (file.canExecute() ? "x" : "-"),
                file.length(),
                sdf.format(file.lastModified()),
                file.getName()
        );
    }

    private static Map<String, File> getFileList(Path path) throws IOException {
        return Files.walk(
                Paths.get(path.toAbsolutePath().toString()), 1)
                .filter(e -> {
                    try {
                        return (Files.isRegularFile(e) || Files.isDirectory(e)) && !Files.isHidden(e);
                    } catch (IOException ex) {
                        System.out.printf("Error when traversing through a file: %s", e.toAbsolutePath());
                        return false;
                    }
                })
                .map(Path::toFile)
                .collect(Collectors.toMap(File::getName, e -> e));
    }

    private void packetAndSend(byte[] content, int len) throws IOException {
        dout.writeInt(len);
        out.write(content, 0, len);
        out.flush();
    }

    private void packetAndSend(String content) throws IOException {
        byte[] c = content.getBytes();
        packetAndSend(c, c.length);
    }

    private void packetAndSend(String format, Object... args) throws IOException {
        packetAndSend(String.format(format, args));
    }

    private int generateNewFreePortNumber() {
        int port;
        while (true) {
            int port1 = (int) (Math.random() * 60 + 195); // low = 195; high = 255
            int port2 = (int) (Math.random() * 256); // low = 0; high = 255
            port = port1 * 256 + port2; // low = 1024; high = 65535

            try {
                //noinspection EmptyTryBlock
                try (ServerSocket ss = new ServerSocket(port)) {
                }
                break;
            } catch (IOException ignored) {
            }
        }

        return port;
    }
}
