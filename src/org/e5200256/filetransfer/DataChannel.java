package org.e5200256.filetransfer;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class DataChannel<T extends Closeable> extends Thread {
    private Closeable io;
    private Socket sck;
    private List<Consumer<DataChannel<T>>> afters = new LinkedList<>();

    public DataChannel(T io, Socket sck) {
        this.io = io;
        this.sck = sck;
    }

    public DataChannel(T io, Socket sck, Consumer<DataChannel<T>> after) {
        this(io, sck);
        this.afters.add(after);
    }

    public boolean addAfter(Consumer<DataChannel<T>> after) {
        return this.afters.add(after);
    }

    public boolean removeAfter(Consumer<DataChannel<T>> after) {
        return this.afters.remove(after);
    }

    @Override
    public void run() {
        try {
            if (InputStream.class.isAssignableFrom(io.getClass())) {
                // 我寫嘢去 sck
                InputStream in = (InputStream)io;
                transferBytes(in, sck.getOutputStream());
            } else if (OutputStream.class.isAssignableFrom(io.getClass())) {
                // sck 寫嘢俾我
                OutputStream out = (OutputStream)io;
                transferBytes(sck.getInputStream(), out);
            }
            io.close();
            sck.close();
            afters.stream().forEach(a -> a.accept(this));
        } catch (IOException e) {
            System.out.println("IO error on transfering bytes on " + toString());
        }
    }

    private void transferBytes(InputStream in, OutputStream out) throws IOException {
        long readed = 0L;

        byte[] reading = new byte[1024];
        int thisRead = 0;
        while ((thisRead = in.read(reading)) > -1)
        {
            out.write(reading);
            out.flush();

            readed += thisRead;
        }
        out.flush();
    }

    @Override
    public String toString() {
        return "data channel on port " + sck.getLocalPort() + " between (local) " + io.toString() + " and (remote) Socket@" + sck.getInetAddress() + ":" + sck.getPort();
    }
}
