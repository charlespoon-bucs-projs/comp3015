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
    private Exception exception;

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
        } catch (IOException e) {
            System.out.printf("(%s:%d) IO error on transfering bytes on %s\r\n", sck.getInetAddress(), sck.getPort(),  toString());
            exception = e;
        }


        try {
            io.close();
        } catch (IOException e) {
            System.out.printf("(%s:%d) IO error on closing local file stream on %s\r\n", sck.getInetAddress(), sck.getPort(),  toString());
            exception = e;
        }
        try {
            sck.close();
        } catch (IOException e) {
            System.out.printf("(%s:%d) IO error on closing socket on %s\r\n", sck.getInetAddress(), sck.getPort(),  toString());
            exception = e;
        }
        afters.stream().forEach(a -> a.accept(this));
    }

    private void transferBytes(InputStream in, OutputStream out) throws IOException {
        long readed = 0L;

        byte[] reading = new byte[1024];
        int thisRead = 0;
        while ((thisRead = in.read(reading)) > -1 && !isInterrupted())
        {
            out.write(reading);
            out.flush();

//            System.out.println(readed += thisRead);
        }
        out.flush();
    }

    @Override
    public String toString() {
        return "data channel on port " + sck.getLocalPort() + " between (local) " + io.toString() + " and (remote) Socket@" + sck.getInetAddress() + ":" + sck.getPort();
    }

    public Exception getException() {
        return exception;
    }
}
