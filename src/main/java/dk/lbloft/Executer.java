package dk.lbloft;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.java.Log;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 */
@Log
public class Executer {
    private String[] cmd;
    private HashMap<String, String> env = new HashMap<>(System.getenv());;
    private File workingDir = null;
    private ResultHandler resultHandler = new DefaultResultHandler();
    private Level logLevel = Level.FINER;

    private Executer(String... cmd) {
        this.cmd = cmd;
    }

    public static Executer cmd(String... cmd) {
        return new Executer(cmd);
    }

    public static Executer sh(String cmd, Object ... args) {
        return new Executer("/bin/sh", "-c", String.format(cmd, args));
    }

    public Executer args(String... args) {
        ArrayList<String> cmd = Lists.newArrayList(this.cmd);
        cmd.addAll(Lists.newArrayList(args));
        this.cmd = cmd.toArray((new String[cmd.size()]));
        return this;
    }

    public Executer env(Map<String, String> env) {
        this.env = new HashMap<>(env);
        return this;
    }

    public Executer addEnv(String key, String value) {
        env.put(key, value);
        return this;
    }

    private String[] getEnv() {
        String[] tmpEnv = new String[env.size()];
        int pos = 0;
        for (Map.Entry<String, String> entry : env.entrySet()) {
            tmpEnv[pos++] = entry.toString();
        }
        return tmpEnv;
    }

    public Executer workingDir(Path workingDir) {
        this.workingDir = workingDir.toFile();
        return this;
    }

    public Executer workingDir(File workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    public Executer logLevel(Level logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public Executer logToConsole() {
        this.logLevel = null;
        return this;
    }

    public Executer resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public Future<Result> run(ExecutorService executor) {
        return executor.submit((Callable)this::run);
    }

    @SneakyThrows
    public Result run() {
        StreamGobbler errorGobbler = null;
        StreamGobbler outputGobbler = null;
        Process p = null;
        int returncode = -1;
        try {
            log.fine("Executing " + Arrays.stream(cmd).collect(Collectors.joining(" ")));
            log.fine(" - Environment" + Arrays.stream(getEnv()).collect(Collectors.joining(" ")));
            log.fine(" - WorkingDir" + workingDir);

            p = Runtime.getRuntime().exec(cmd, getEnv(), workingDir);
            errorGobbler = new StreamGobbler(p.getErrorStream(), StreamGobbler.Type.ERROR, logLevel);
            outputGobbler = new StreamGobbler(p.getInputStream(), StreamGobbler.Type.OUTPUT, logLevel);

            errorGobbler.start();
            outputGobbler.start();

            errorGobbler.join();
            outputGobbler.join();

            returncode = p.waitFor();
            p.destroy();
            log.fine(" + returnCode: " + returncode);
        } catch (Exception ignore) {
            log.log(Level.FINE, "Ignore exception: " + ignore.getMessage(), ignore);
        } finally {
            if(p != null) {
                p.destroy();
            }
        }
        return resultHandler.create(returncode, outputGobbler.toString(), errorGobbler.toString());
    }

    protected static class StreamGobbler extends Thread {
        enum Type {ERROR, OUTPUT};

        InputStream is;
        Type type;
        private Level logLevel;
        StringBuilder output = new StringBuilder();

        public StreamGobbler(InputStream is, Type type, Level logLevel) {
            this.is = is;
            this.type = type;
            this.logLevel = logLevel;
        }

        public void run() {
            synchronized( output ) {
                try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        if (logLevel == null) {
                            System.out.println(type.name() + ">" + line);
                        } else {
                            log.log(logLevel, type.name() + ">" + line);
                        }
                        output.append(line).append("\n");
                    }
                } catch (IOException ioe) {
                    log.warning("Ignore exception: " + ioe.getMessage());
                }
            }
        }

        @Override
        public String toString() {
            synchronized( output ) {
                return output.toString();
            }
        }
    }

    public interface  ResultHandler {
        Result create(int returnCode, String stdOut, String stdErr) throws Exception;
    }

    public static class DefaultResultHandler implements ResultHandler {
        @Override
        public Result create(int returnCode, String stdOut, String stdErr) {
            return new Result(returnCode, stdOut, stdErr);
        }
    }

    @ToString
    public static class Result {
        public final int returnCode;
        public final String stdOut;
        public final String stdErr;

        public Result(int returnCode, String stdOut, String stdErr) {
            this.returnCode = returnCode;
            this.stdOut = stdOut;
            this.stdErr = stdErr;
        }
    }
}
