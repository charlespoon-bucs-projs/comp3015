package org.e5200256.filetransfer;

import java.io.Closeable;
import java.net.Socket;
import java.util.function.Consumer;

public class DataChannelPool extends SelfHashPool<DataChannel> {
    public <T extends Closeable> String putNew(T io, Socket sck) {
        return this.putNew(io, sck, null);
    }

    public <T extends Closeable> String putNew(T io, Socket sck, Consumer<DataChannel<T>> after) {
        //noinspection SuspiciousMethodCalls
        DataChannel<T> d = new DataChannel<T>(io, sck, this::remove);
        if (after != null) d.addAfter(after);
        String ret = super.putNew(d);
        d.start();
        return ret;
    }

    @Override
    @Deprecated
    String putNew(DataChannel dataChannel) {
        return super.putNew(dataChannel);
    }
}
