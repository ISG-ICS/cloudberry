package util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class BinaryMessageBuilder {

    // data type size
    static int DOUBLE_BYTES = 8;
    static int BIG_INT_BYTES = 8;
    static int LONG_BYTES = 8;

    // initialized number of records
    static int INIT_CAPACITY = 2000;

    // header size
    int headerSize;

    /**
     * ---- header ----
     *   id        category  start     end
     * | VARCHAR | VARCHAR | 8 BYTES | 8 BYTES |
     * ---- payload ----
     *   id1       lng1      lat1      id2       lng2      lat2      ...
     * | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | ...
     *
     * NOTE: VARCHAR = 1 BYTE of length + length BYTES of real UTF-8 characters
     */
    byte[] buffer;
    int capacity;
    int count;
    int tupleSize;

    public BinaryMessageBuilder(String _id, String _category, long _start, long _end) {
        capacity = INIT_CAPACITY;
        byte[] id = _id.getBytes(StandardCharsets.UTF_8);
        byte[] category = _category.getBytes(StandardCharsets.UTF_8);
        headerSize = 1 + id.length + 1 + category.length + LONG_BYTES + LONG_BYTES;
        tupleSize = BIG_INT_BYTES + DOUBLE_BYTES + DOUBLE_BYTES;
        buffer = new byte[headerSize + tupleSize * INIT_CAPACITY];
        buildHeader(id, category, _start, _end);
        count = 0;
    }

    private void buildHeader(byte[] _id, byte[] _category, long _start, long _end) {
        // variable length for _id string
        int offset = 0;
        // write length of _id
        buffer[offset] = (byte) _id.length;
        offset += 1;
        // write each char of _id
        for (int i = 0; i < _id.length; i ++) {
            buffer[offset + i] = _id[i];
        }
        offset += _id.length;

        // variable length for _category string
        // write length of _category
        buffer[offset] = (byte) _category.length;
        offset += 1;
        for (int i = 0; i < _category.length; i ++) {
            buffer[offset + i] = _category[i];
        }
        offset += _category.length;

        // write _start long
        buffer[offset+0] = (byte) ((_start >> 56) & 0xff);
        buffer[offset+1] = (byte) ((_start >> 48) & 0xff);
        buffer[offset+2] = (byte) ((_start >> 40) & 0xff);
        buffer[offset+3] = (byte) ((_start >> 32) & 0xff);
        buffer[offset+4] = (byte) ((_start >> 24) & 0xff);
        buffer[offset+5] = (byte) ((_start >> 16) & 0xff);
        buffer[offset+6] = (byte) ((_start >>  8) & 0xff);
        buffer[offset+7] = (byte) ((_start >>  0) & 0xff);
        offset += LONG_BYTES;

        // write _end long
        buffer[offset+0] = (byte) ((_end >> 56) & 0xff);
        buffer[offset+1] = (byte) ((_end >> 48) & 0xff);
        buffer[offset+2] = (byte) ((_end >> 40) & 0xff);
        buffer[offset+3] = (byte) ((_end >> 32) & 0xff);
        buffer[offset+4] = (byte) ((_end >> 24) & 0xff);
        buffer[offset+5] = (byte) ((_end >> 16) & 0xff);
        buffer[offset+6] = (byte) ((_end >>  8) & 0xff);
        buffer[offset+7] = (byte) ((_end >>  0) & 0xff);
    }

    public void add(BigInteger _id, double _lng, double _lat) {
        // buffer is full
        if (count == capacity - 1) {
            // double the size
            byte[] newBuffer = new byte[headerSize + tupleSize * capacity * 2];
            // copy to new buffer
            System.arraycopy(buffer, 0, newBuffer, 0, headerSize + tupleSize * capacity);
            capacity *= 2;
            buffer = newBuffer;
        }
        // move offset
        int offset = headerSize + tupleSize * count;
        // write id
        byte[] id = _id.toByteArray();
        buffer[offset+0] = id[0];
        buffer[offset+1] = id[1];
        buffer[offset+2] = id[2];
        buffer[offset+3] = id[3];
        buffer[offset+4] = id[4];
        buffer[offset+5] = id[5];
        buffer[offset+6] = id[6];
        buffer[offset+7] = id[7];
        // move offset
        offset = offset + BIG_INT_BYTES;

        // write longitude
        long lng = Double.doubleToRawLongBits(_lng);
        buffer[offset+0] = (byte) ((lng >> 56) & 0xff);
        buffer[offset+1] = (byte) ((lng >> 48) & 0xff);
        buffer[offset+2] = (byte) ((lng >> 40) & 0xff);
        buffer[offset+3] = (byte) ((lng >> 32) & 0xff);
        buffer[offset+4] = (byte) ((lng >> 24) & 0xff);
        buffer[offset+5] = (byte) ((lng >> 16) & 0xff);
        buffer[offset+6] = (byte) ((lng >>  8) & 0xff);
        buffer[offset+7] = (byte) ((lng >>  0) & 0xff);
        offset = offset + DOUBLE_BYTES;

        // write latitude
        long lat = Double.doubleToRawLongBits(_lat);
        buffer[offset+0] = (byte) ((lat >> 56) & 0xff);
        buffer[offset+1] = (byte) ((lat >> 48) & 0xff);
        buffer[offset+2] = (byte) ((lat >> 40) & 0xff);
        buffer[offset+3] = (byte) ((lat >> 32) & 0xff);
        buffer[offset+4] = (byte) ((lat >> 24) & 0xff);
        buffer[offset+5] = (byte) ((lat >> 16) & 0xff);
        buffer[offset+6] = (byte) ((lat >>  8) & 0xff);
        buffer[offset+7] = (byte) ((lat >>  0) & 0xff);

        // move pos
        count++;
    }

    public byte[] getBuffer() {
        // shrink buffer to exact the size of data payload
        byte[] newBuffer = new byte[headerSize + tupleSize * count];
        // copy to new buffer
        System.arraycopy(buffer, 0, newBuffer, 0, headerSize + tupleSize * count);
        buffer = newBuffer;
        return buffer;
    }
}

