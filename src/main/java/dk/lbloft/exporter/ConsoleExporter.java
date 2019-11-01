package dk.lbloft.exporter;

import dk.lbloft.CanListener;
import dk.lbloft.Console;

import java.util.Collection;
import java.util.Date;

public class ConsoleExporter implements CanExporter {
    @Override
    public void export(Collection<CanListener<?>> listeners) {
        Console.out.clearScreen();
        Console.out.printTitle("Can data");
        boolean even = true;
        int w = Console.out.getWidth();
        for (CanListener listener : listeners) {
            if(listener.getValue() != null) {
                Console.out.println(
                        String.format("%s %" + (w - listener.getName().length() -1) + "s", listener.getName(), listener.getValue()),
                        Console.Color.Default, even ? Console.Color.Default : Console.Color.DarkGray
                );
            }
            even = !even;
        }
        Console.out.printLine();
        Console.out.println(new Date());
    }
}
