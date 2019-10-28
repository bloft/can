package dk.lbloft;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 */
@Log
public class Console extends PrintStream {
    private final static String FILTER_REGEX = "\u001B\\[[0-9;]+m";

    @Getter
    @RequiredArgsConstructor
    public enum Format {
        Normal(0),
        Bold(1),
        Dim(2),
        Underline(4),
        Blink(5),
        Hidden(8);

        private final int code;
    }

    @RequiredArgsConstructor
    @Getter
    public enum Color {
        Black(0),
        Red(1),
        Green(2),
        Yellow(3),
        Blue(4),
        Magenta(5),
        Cyan(6),
        LightGray(7),
        DarkGray(60),
        LightRed(61),
        LightGreen(62),
        LightYellow(63),
        LightBlue(64),
        LightMagenta(65),
        LightCyan(66),
        Default(9);

        private final int code;
    }


    public static final Console out = new Console(System.out);
    public static final Console err = new Console(System.err);

    public Console(OutputStream out) {
        super(out);
    }

    public void print(Object obj, Format ... formats) {
        print(begin(Color.Default, Color.Default, formats) + obj + end());
    }

    public void print(Object obj, Color fg, Format ... formats) {
        print(begin(fg, Color.Default, formats) + obj + end());
    }

    public void print(Object obj, Color fg, Color bg, Format ... formats) {
        print(begin(fg, bg, formats) + obj + end());
    }

    public void println(Object obj, Format ... formats) {
        println(begin(Color.Default, Color.Default, formats) + obj + end());
    }

    public void println(Object obj, Color fg, Format ... formats) {
        println(begin(fg, Color.Default, formats) + obj + end());
    }

    public void println(Object obj, Color fg, Color bg, Format ... formats) {
        println(begin(fg, bg, formats) + obj + end());
    }

    public void ok() {
        print("\u2713", Color.Green, Format.Bold);
    }

    public void ok(String msg) {
        ok();
        print(" ");
        println(msg);
    }

    public void fail() {
        print("\u2717", Color.Red, Format.Bold);
    }

    public void fail(String msg) {
        fail();
        print(" ");
        println(msg);
    }

    public static String toPrittyTime(long duration, TimeUnit timeunit) {
        return toPrittyTime(timeunit.toMillis(duration));
    }

    /**
     * Convert a time interval to a pritty readable time. Eks 1h 23m 12s
     * @param ms Time in millis
     * @return Time in a human readable format
     */
    public static String toPrittyTime(long ms) {
        StringBuilder sb = new StringBuilder();
        ms = toPrittyTimeHelper(sb, ms, TimeUnit.DAYS, "d");
        ms = toPrittyTimeHelper(sb, ms, TimeUnit.HOURS, "h");
        ms = toPrittyTimeHelper(sb, ms, TimeUnit.MINUTES, "m");
        ms = toPrittyTimeHelper(sb, ms, TimeUnit.SECONDS, "s");
        ms = toPrittyTimeHelper(sb, ms, TimeUnit.MILLISECONDS, "ms");
        if(sb.length() == 0) {
            sb.append("0ms");
        }
        return sb.toString().trim();
    }

    private static long toPrittyTimeHelper(StringBuilder sb, long ms, TimeUnit unit, String name) {
        long tmp = unit.convert(ms, TimeUnit.MILLISECONDS);
        if(tmp > 0) sb.append(tmp).append(name).append(" ");
        return ms - unit.toMillis(tmp);
    }

    public void printBigTitle(String title) {
        printBigTitle(title, getWidth());
    }

    public void printBigTitle(String title, int width) {
        printLine (title, width, '\u2550', '\u2566');
        printTitle(title, width, ' ', '\u2551', '\u2551');
        printLine (title, width, '\u2550', '\u2569');
    }

    private void printLine(String title, int width, char c, char extra) {
        width = Math.max(width, title.length() + 10);
        int startWidth = (width/2)-3 - (title.length() / 2);
        int endWidth = startWidth + (title.length() % 2 == width % 2 ? 0 : 1);

        printLine(startWidth, c, false);
        print(extra);
        printLine(title.length()+2, c, false);
        print(extra);
        printLine(endWidth, c, true);
    }

    public void printLine() {
        printLine(getWidth());
    }

    public void printLine(int width) {
        printLine(width, '\u2500');
    }

    public void printLine(int width, char c) {
        printLine(width, c, true);
    }

    public void printLine(int width, char c, boolean newLine) {
        if(newLine) {
            println(new String(new char[width]).replace("\0", Character.toString(c)));
        } else {
            print(new String(new char[width]).replace("\0", Character.toString(c)));
        }
    }

    public void printTitle(String title) {
        printTitle(title, getWidth());
    }

    public void printTitle(String title, int width) {
        printTitle(title, width, '\u2500', '\u2524', '\u251C');
    }

    public void printTitle(String title, int width, char spaceChar, char startChar, char endChar) {
        width = Math.max(width, title.length() + 10);
        int startWidth = (width/2)-2 - (title.length() / 2);
        int endWidth = startWidth + (title.length() % 2 == width % 2 ? 0 : 1);

        printLine(startWidth, spaceChar, false);
        print(startChar);
        print(" ");
        print(title, Color.Cyan);
        print(" ");
        print(endChar);
        printLine(endWidth, spaceChar, true);
    }

    private String begin(Color fg, Color bg, Format ... formats) {
        Stream<Integer> stream = Arrays.stream(formats).map(Format::getCode);
        stream = Stream.concat(stream, Stream.of(fg.getCode() + 30));
        stream = Stream.concat(stream, Stream.of(bg.getCode() + 40));
        return stream.map(i -> Integer.toString(i)).collect(Collectors.joining(";", "\u001B[", "m"));
    }

    private String end() {
        return begin(Color.Default, Color.Default, Format.Normal);
    }

    public void printCaller() {
        List<StackTraceElement> stack = Arrays.asList(Thread.getAllStackTraces().get(Thread.currentThread()));

        List<StackTraceElement> stackList = stack.
                stream().
                filter(s -> !s.getClassName().startsWith("java.lang.reflect.")).
                filter(s -> !s.getClassName().startsWith("sun.reflect.")).
                filter(s -> !s.getClassName().startsWith("org.codehaus.groovy.")).
                filter(s -> !s.getClassName().startsWith("groovy.lang.")).
                collect(Collectors.toList());

        println("Checking caller of " + stack.get(3).getClassName() + "." + stack.get(3).getMethodName());

        int count = Math.min(10, stackList.size() - 4);
        for (int i = 4; i <= (4 + count); i++) {
            print(i == (4 + count) ? "  \u2570\u2500\u257C " : "  \u251C\u2500\u257C ", Console.Color.Red);
            if(stackList.get(i).getClassName().startsWith("com.stibo.")) {
                println(stackList.get(i), Console.Color.Yellow);
            } else {
                println(stackList.get(i));
            }
        }
        println();
    }

    public void clearScreen() {
        print("\033[H\033[2J");
        flush();
    }

    public int getWidth() {
        try {
            Executer.Result result = Executer.cmd("tput", "cols").run();
            if (result.returnCode == 0) {
                return Integer.parseInt(result.stdOut.trim());
            }
        } catch (Exception e) {}
        return 100;
    }
}