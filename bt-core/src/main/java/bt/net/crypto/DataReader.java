package bt.net.crypto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;

class DataReader {

    private final ReadableByteChannel channel;
    private final Duration timeout;

    DataReader(ReadableByteChannel channel, Duration timeout) {
        this.channel = channel;
        this.timeout = timeout;
    }

    int read(ByteBuffer buffer, int min, int limit) {
        try {
            return read(channel, buffer, timeout, min, limit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int read(ReadableByteChannel channel, ByteBuffer buf, Duration timeout, int min, int limit) throws IOException {
        if (min < 0 || limit < 0 || limit < min) {
            throw new IllegalArgumentException("Illegal arguments: min (" + min + "), limit (" + limit + ")");
        }

        long t1 = System.currentTimeMillis();
        int readTotal = 0;
        int read;
        long timeoutMillis = timeout.toMillis();
        int times_nothing_received = 0;
        do {
            read = channel.read(buf);
            if (read == 0) {
                times_nothing_received++;
            } else if (read < 0) {
                throw new RuntimeException("Received EOF, total bytes read: " + readTotal + ", expected: " + min + ".." + limit);
            } else {
                readTotal += read;
                times_nothing_received = 0;
            }
            if (readTotal > limit) {
                throw new IllegalStateException("More than " + limit + " bytes received: " + readTotal);
            } else if (readTotal >= min && times_nothing_received >= 3) {
                // hasn't received anything for 3 times in a row; assuming all data has arrived
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for data", e);
            }
        } while (System.currentTimeMillis() - t1 <= timeoutMillis);

        if (readTotal < min) {
            throw new IllegalStateException("Less than " + min + " bytes received: " + readTotal);
        }

        return readTotal;
    }
}