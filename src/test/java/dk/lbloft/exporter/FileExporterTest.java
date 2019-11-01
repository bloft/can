package dk.lbloft.exporter;

import dk.lbloft.CanListener;
import dk.lbloft.Main;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
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

        System.out.println(String.format("trip-%1$tF-%1$tR.log", new Date()));

        CanExporter exporter = new FileExporter(new File("test.out"));

        export(exporter, listeners, 0x01);
        export(exporter, listeners, 0x02);
    }

    @Test
    public void testAllInMain() throws InterruptedException {
        ArrayList<CanListener<?>> listeners = Main.buildListeners();

        CanExporter exporter = new ConsoleExporter();

        Thread.sleep(1000);

        export(exporter, listeners, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08);

    }


    // Helper methods

    private void export(CanExporter exporter, List<CanListener<?>> listeners, int value) {
        export(exporter, listeners, new byte[] {(byte)value});
    }

    private void export(CanExporter exporter, List<CanListener<?>> listeners, byte ... value) {
        for (CanListener listener : listeners) {
            listener.newFrame(0x001, ByteBuffer.wrap(value));
        }
        exporter.export(listeners);
    }
}
