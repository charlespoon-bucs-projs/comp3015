package org.e5200256.filetransfer;

import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;

public class DataChannelInitializeServerSocketHouse extends SelfHashPool<SingleServerSocket> {
    public String putNewSingleServerSocket(int port, InetAddress dest, Consumer<Socket> after) {
        SingleServerSocket sss = SingleServerSocket.create(this, port, dest, after);
        if (sss == null)
            return null;
        String ret = super.putNew(sss);
        sss.start();
        return ret;
    }

    @Override
    @Deprecated
    public String putNew(SingleServerSocket singleServerSocket) {
        return super.putNew(singleServerSocket);
    }
}
