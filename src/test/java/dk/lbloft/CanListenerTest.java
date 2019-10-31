package dk.lbloft;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class CanListenerTest {

    private ByteBuffer wrap(int ... bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        for (int aByte : bytes) {
            buffer.put((byte) aByte);
        }
        return buffer;
    }

    @Test
    public void testSetMapper() {
        CanListener<Integer> l1 = CanListener.getByte("Test", 0, 0x001).setMapping(i -> i*2);
        assertThat(l1.newFrame(0x001, wrap(0x00)), is(0));
        assertThat(l1.newFrame(0x001, wrap(0x01)), is(2));

        CanListener<Integer> l2 = CanListener.getByte("Test", 0, 0x001).setMapping(i -> (int)(i*1.25));
        assertThat(l2.newFrame(0x001, wrap(0x04)), is(5));
    }

    @Test
    public void testBit() {
        CanListener<String> l1 = CanListener.getBit("Test", 0, 0x01, "On", "Off", 0x001);
        assertThat(l1.newFrame(0x001, wrap(0x01)), is("On"));
        assertThat(l1.newFrame(0x001, wrap(0x02)), is("Off"));
        assertThat(l1.newFrame(0x001, wrap(0x03)), is("On"));

        CanListener<String> l2 = CanListener.getBit("Test", 1, 0x01, "On", "Off", 0x001);
        assertThat(l2.newFrame(0x001, wrap(0x00, 0x01)), is("On"));
        assertThat(l2.newFrame(0x001, wrap(0x00, 0x02)), is("Off"));
        assertThat(l2.newFrame(0x001, wrap(0x00, 0x03)), is("On"));

        CanListener<String> l3 = CanListener.getBit("Test", 1, 0x01, "Open", "Close", 0x001);
        assertThat(l3.newFrame(0x001, wrap(0x00, 0x01)), is("Open"));
        assertThat(l3.newFrame(0x001, wrap(0x00, 0x02)), is("Close"));
    }

    @Test
    public void testByte() {
        CanListener<Integer> l1 = CanListener.getByte("Test", 0, 0x001);
        assertThat(l1.newFrame(0x001, wrap(0x00)), is(0));
        assertThat(l1.newFrame(0x001, wrap(0x01)), is(1));
        assertThat(l1.newFrame(0x001, wrap(0xFF, 0x00)), is(255));

        CanListener<Integer> l2 = CanListener.getByte("Test", 1, 0x001);
        assertThat(l2.newFrame(0x001, wrap(0x00, 0x00)), is(0));
        assertThat(l2.newFrame(0x001, wrap(0x00, 0x01)), is(1));
        assertThat(l2.newFrame(0x001, wrap(0x00, 0xFF)), is(255));
    }

    @Test
    public void testShort() {
        CanListener<Integer> l1 = CanListener.getShort("Test", 0, 0x001);
        assertThat(l1.newFrame(0x001, wrap(0x00, 0x00)), is(0));
        assertThat(l1.newFrame(0x001, wrap(0x00, 0x01)), is(1));
        assertThat(l1.newFrame(0x001, wrap(0x01, 0x00)), is(256));
        assertThat(l1.newFrame(0x001, wrap(0xFF, 0xFF, 0x00)), is(65535));

        CanListener<Integer> l2 = CanListener.getShort("Test", 1, 0x001);
        assertThat(l2.newFrame(0x001, wrap(0x00, 0xFF, 0xFF)), is(65535));
    }

    @Test
    public void testToHex() {
        CanListener<String> l1 = CanListener.getHex(0x001, 0, 1);
        assertThat(l1.newFrame(0x001, wrap(0x00, 0x00)), is("00"));
        assertThat(l1.newFrame(0x001, wrap(0x01, 0x00)), is("01"));
        assertThat(l1.newFrame(0x001, wrap(0xFF, 0x00)), is("ff"));

        CanListener<String> l2 = CanListener.getHex(0x001, 0, 2);
        assertThat(l2.newFrame(0x001, wrap(0x00, 0x00)), is("00 00"));
        assertThat(l2.newFrame(0x001, wrap(0x01, 0x10)), is("01 10"));
        assertThat(l2.newFrame(0x001, wrap(0xFF, 0x01)), is("ff 01"));

        CanListener<String> l3 = CanListener.getHex(0x001);
        assertThat(l3.newFrame(0x001, wrap(0xFF)), is("ff"));
        assertThat(l3.newFrame(0x001, wrap(0xFF, 0x00)), is("ff 00"));
        assertThat(l3.newFrame(0x001, wrap(0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06)), is("ff 00 01 02 03 04 05 06"));
    }
}
