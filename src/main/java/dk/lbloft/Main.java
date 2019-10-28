package dk.lbloft;

import com.github.kayak.core.*;
import picocli.CommandLine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
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

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main())
                .addSubcommand(new Daemon())
                .execute(args)
        );
    }

    @Override
    public Integer call() throws Exception {
        Console.out.printf("Connecting to %s on %s:%s\n", bus, host, port);

        BusURL url = new BusURL(host, port, bus); /* Connection to the socketcand */
        TimeSource timeSource = new TimeSource(); /* TimeSource to control the bus */

        /* Create a bus and configure it */
        Bus bus = new Bus();
        bus.setConnection(url);
        bus.setTimeSource(timeSource);

        /* A Subscription is the link between a Bus and a FrameListener.
         * We are only interested in ID 0x12. The Subscription manages that
         * only Frames with this ID get delivered.
         */
        Subscription subscription = new Subscription(receiver, bus);
        //subscription.subscribe(0x611, false);
        //subscription.subscribe(0x619, false);

        // subscription.setSubscribeAll(true);

        ArrayList<CanListener> listeners = new ArrayList<>();

        listeners.add(CanListener.getByte(bus, "Speed", 0, 0x3D0));
        listeners.add(CanListener.getInt(bus, "Trip", 4, 0x611));
        listeners.add(CanListener.getShort(bus, "Left (km)", 5, 0x619));

        listeners.add(CanListener.getBit(bus, "Left Door", 5, 0x20, "Open", "Close", 0x620));
        listeners.add(CanListener.getBit(bus, "Right Door", 5, 0x10, "Open", "Close", 0x620));
        listeners.add(CanListener.getBit(bus, "Handbreak", 7, 0x20, "On", "Off", 0x620));

        listeners.add(new CanListener<Integer>("RPM", bus, 0x1C4) {
            public Integer handle(int id, ByteBuffer data) {
                return (int)((data.getShort() & 0xffff) / 1.25);
            }
        });

        listeners.add(new CanListener<String>("Speed Limiter", bus, 0x3C5) {
            public String handle(int id, ByteBuffer data) {
                int type = data.get();
                if((type & 0xFF) == 0x80) {
                    return "" + (data.get() & 0xFF);
                } else if((type & 0xFF) == 0xC0) {
                    return "" + (data.get() & 0xFF) + " disabled";
                } else if((type & 0xFF) == 0x00) {
                    return "OFF";
                } else {
                    return "Unknown";
                }
            }
        });

        listeners.add(new CanListener<String>("Info", bus, 0x620) {
            public String handle(int id, ByteBuffer data) {
                String value = "";
                for(int i = 4; i < 8; i++) {
                    byte b = data.get(i);
                    value += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
                    value += " ";
                }
                return value;
            }
        });

        listeners.add(new CanListener<Double>("Fuel", bus, 0x3D0, 0x3D3) {
            int speed = 0;
            int maf = 0;

            @Override
            public Double handle(int id, ByteBuffer data) {
                if(id == 0x3D0) speed = data.get() & 0xFF;
                if(id == 0x3D3) maf = data.getShort() & 0xffff;
                return maf * (3600 / (14.7 * 820)) / speed;  // Calc l/100km
            }
        });

/*
        listeners.add(new CanListener<Integer>("Speed", bus, 0x3D0) {
            public Integer handle(int id, ByteBuffer data) {
                return data.get() & 0xff;
            }
        });

        listeners.add(new CanListener<Integer>("Trip", bus, 0x611) {
            public Integer handle(int id, ByteBuffer data) {
                return (data.getInt(4) & 0xffffffff);
            }
        });

        listeners.add(new CanListener<Integer>("Left (km)", bus, 0x619) {
            public Integer handle(int id, ByteBuffer data) {
                return (data.getShort(5) & 0xffff);
            }
        });
        
        listeners.add(new CanListener<String>("Left Door", bus, 0x620) {
            public String handle(int id, ByteBuffer data) {
                return (data.get(5) & 0x20) == 0x20 ? "Open" : "Close";
            }
        });

        listeners.add(new CanListener<String>("Right Door", bus, 0x620) {
            public String handle(int id, ByteBuffer data) {
                return (data.get(5) & 0x10) == 0x10 ? "Open" : "Close";
            }
        });

        listeners.add(new CanListener<String>("Handbreak", bus, 0x620) {
            public String handle(int id, ByteBuffer data) {
                return (data.get(7) & 0x10) == 0x10 ? "On" : "Off";
            }
        });
*/


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
            Console.out.clearScreen();
            Console.out.printTitle("Can data");
            for (CanListener listener : listeners) {
                if(listener.getValue() != null) {
                    Console.out.println(listener.toString());
                }
            }
            Console.out.printLine();
            Console.out.println(new Date());
        }

        timeSource.stop();

        return 0;
    }

    /* FrameListener that will print out every Frame it receives */
    private static FrameListener receiver = frame -> System.out.println(frame.toLogFileNotation());
}
