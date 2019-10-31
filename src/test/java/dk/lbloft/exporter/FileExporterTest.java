package dk.lbloft.exporter;

import dk.lbloft.CanListener;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FileExporterTest {

    @Test
    public void dumpSimple() {
        ArrayList<CanListener<?>> listeners = new ArrayList<>();
        listeners.add(CanListener.getByte("Test", 0, 0x001));
        listeners.add(CanListener.getByte("Foo", 0, 0x002));
        listeners.add(CanListener.getByte("Bar", 0, 0x003));
        listeners.add(CanListener.getHex(0x003, 0, 1));
        listeners.add(new CanListener<String>("String", 0x001) {
            @Override
            public String handle(int id, ByteBuffer data) {
                return String.format("%8s", Integer.toBinaryString(data.get() & 0xFF)).replace(' ', '0');
            }
        });

        CanExporter exporter = new FileExporter(new File("test.out"));

        export(exporter, listeners, 0x01);
        export(exporter, listeners, 0x02);
    }

    private void export(CanExporter exporter, List<CanListener<?>> listeners, int value) {
        for (CanListener listener : listeners) {
            listener.newFrame(0x001, ByteBuffer.wrap(new byte[] {(byte)value}));
        }
        exporter.export(listeners);
    }
}
