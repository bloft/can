package dk.lbloft;

import com.github.kayak.core.*;
import dk.lbloft.exporter.CanExporter;
import dk.lbloft.exporter.ConsoleExporter;
import dk.lbloft.exporter.FileExporter;
import picocli.CommandLine;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "can")
public class Main implements Callable<Integer> {
    boolean running = true;

    @CommandLine.Option(names = {"-h", "--host"}, description = "The host to connect to", defaultValue = "127.0.0.1")
    private String host;

    @CommandLine.Option(names = {"-p", "--port"}, description = "The port to connect to", defaultValue = "28600")
    private int port;

    @CommandLine.Option(names = {"-b", "--bus"}, description = "The name of the bus", defaultValue = "can0")
    private String bus;

    private Bus busConnection;
    private ArrayList<CanListener<?>> listeners = new ArrayList<>();
    private ArrayList<CanExporter> exporters = new ArrayList<>();

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main())
                .addSubcommand(new Daemon())
                .execute(args)
        );
    }

    public CanListener add(CanListener<?> listener) {
        Subscription subscription = new Subscription(listener, busConnection);
        for (Integer id : listener.getIds()) {
            subscription.subscribe(id, false);
        }
        return listener;
    }

    @Override
    public Integer call() throws Exception {
        Console.out.printf("Connecting to %s on %s:%s\n", bus, host, port);

        BusURL url = new BusURL(host, port, bus); /* Connection to the socketcand */
        TimeSource timeSource = new TimeSource(); /* TimeSource to control the bus */

        /* Create a bus and configure it */
        busConnection = new Bus();
        busConnection.setConnection(url);
        busConnection.setTimeSource(timeSource);

        listeners = buildListeners();

        for (CanListener listener : listeners) {
            add(listener);
        }

        exporters.add(new ConsoleExporter());
        exporters.add(new FileExporter(new File("test.out")));


        /* Starting the TimeSource will make the Bus connect to the socketcand
         * and deliver Frames. After two seconds the connections are terminated.
         */
        timeSource.play();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                running = false;
            }
        });

        while (running) {
            Thread.sleep(1000);
            for (CanExporter exporter : exporters) {
                exporter.export(listeners);
            }
        }

        timeSource.stop();

        return 0;
    }

    public ArrayList<CanListener<?>> buildListeners() {
        ArrayList<CanListener<?>> listeners = new ArrayList<>();

        listeners.add(CanListener.getByte( "Speed", 0, 0x3D0));
        listeners.add(CanListener.getShort("RPM", 0, 0x1C4).setMapping(i -> (int)(i*1.25)));
        listeners.add(CanListener.getInt("Trip", 4, 0x611));
        listeners.add(CanListener.getShort("Left (km)", 5, 0x619));

        listeners.add(CanListener.getBit("Left Door", 5, 0x20, "Open", "Close", 0x620));
        listeners.add(CanListener.getBit("Right Door", 5, 0x10, "Open", "Close", 0x620));
        listeners.add(CanListener.getBit("Handbreak", 7, 0x20, "On", "Off", 0x620));

        listeners.add(new CanListener<String>("Speed Limiter", 0x3C5) {
            public String handle(int id, ByteBuffer data) {
                switch (data.get(0) & 0xFF) {
                    case 0x00: return "Off";
                    case 0x80: return String.format("%s", data.get(1) & 0xFF);
                    case 0xC0: return String.format("%s disabled", data.get(1) & 0xFF);
                    case 0xE0: return String.format("%s error", data.get(1) & 0xFF);
                    default: return "Unknown";
                }
            }
        });

        listeners.add(new CanListener<String>("Info1", 0x620) {
            public String handle(int id, ByteBuffer data) {
                String value = "";
                for(int i = 0; i < 5; i++) {
                    value += " " + String.format("%8s", Integer.toBinaryString(data.get(i) & 0xFF)).replace(' ', '0');
                }
                return value.trim();
            }
        });

        listeners.add(new CanListener<String>("Info2", 0x620) {
            public String handle(int id, ByteBuffer data) {
                String value = "";
                for(int i = 4; i < 8; i++) {
                    value += " " + String.format("%8s", Integer.toBinaryString(data.get(i) & 0xFF)).replace(' ', '0');
                }
                return value.trim();
            }
        });

        listeners.add(new CanListener<Double>("Fuel Used", 0x3D3) {
            private long last = -1;

            @Override
            public Double handle(int id, ByteBuffer data) {
                long current = System.currentTimeMillis();
                long diff = current - last;
                last = current;
                if(getValue() == null) {
                    return 0.0;
                } else {
                    int maf = data.getShort() & 0xFFFF;
                    return getValue() + (maf * (3600 / (14.7 * 740)) * (diff / 1000));
                }
            }
        });

        listeners.add(new CanListener<Double>("Fuel", 0x3D0, 0x3D3) {
            int speed = 0;
            int maf = 0;

            @Override
            public Double handle(int id, ByteBuffer data) {
                if(id == 0x3D0) speed = data.get() & 0xFF;
                if(id == 0x3D3) maf = data.getShort() & 0xffff;
                return Math.round(maf * (3600 / (14.7 * 740)) / speed * 100) / 100.0;  // Calc l/100km
            }
        });

        return listeners;
    }

    /* FrameListener that will print out every Frame it receives */
    private static FrameListener receiver = frame -> System.out.println(frame.toLogFileNotation());
}
