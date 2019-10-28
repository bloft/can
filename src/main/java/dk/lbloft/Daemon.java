package dk.lbloft;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "daemon")
public class Daemon implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        return 1;
    }
}
