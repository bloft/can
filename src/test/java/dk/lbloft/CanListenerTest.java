package dk.lbloft;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class CanListenerTest {

    @Test
    public void testBit() {
        CanListener<String> l1 = CanListener.getBit(null, "Test", 0, 0x01, "On", "Off", 0x001);

        l1.handle(0x001, ByteBuffer.wrap(new byte[] {0x01}));
        System.out.println(l1.getValue());
    }
}
