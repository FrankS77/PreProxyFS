package de.fschullerer.preproxyfs.testutil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Mocked socket class for tests.
 *
 * @author From internet but modified.
 */
public class MockSocket extends Socket {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Byte> bytesList = new ArrayList<>();

    private byte[] input;

    /** Constructor. */
    public MockSocket() {}

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.input);
    }

    @Override
    public OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            // every time we call `write` (out.print),
            // we add the bytes to the list 'bytesList'
            public void write(int b) {
                bytesList.add((byte) b);
            }
        };
    }

    /**
     * Set the input of this mocked socket.
     *
     * @param input The input.
     */
    public void setInput(byte[] input) {
        this.input = input;
    }
}
