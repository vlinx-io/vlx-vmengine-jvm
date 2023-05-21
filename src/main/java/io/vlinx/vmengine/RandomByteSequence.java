package io.vlinx.vmengine;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public final class RandomByteSequence extends DataInputStream {

    private static final class RandomByteArrayStream extends ByteArrayInputStream {

        RandomByteArrayStream(final byte[] bytes) {
            super(bytes);
        }

        int getPosition() {
            return pos;
        }

        void unreadByte() {
            if (pos > 0) {
                pos--;
            }
        }

        void seek(int pos) {
            this.pos = pos;
        }
    }

    private final RandomByteArrayStream byteStream;

    public RandomByteSequence(final byte[] bytes) {
        super(new RandomByteArrayStream(bytes));
        byteStream = (RandomByteArrayStream) in;
    }

    public int index() {
        return byteStream.getPosition();
    }

    public void seek(int pos) {
        byteStream.seek(pos);
    }

    void unreadByte() {
        byteStream.unreadByte();
    }
}