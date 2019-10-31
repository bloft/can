package dk.lbloft.exporter;

import com.google.common.io.Files;
import dk.lbloft.CanListener;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.Function;

public class FileExporter implements CanExporter {
    private File outFile;

    private boolean isFirst = true;

    public FileExporter(File outFile) {
        this.outFile = outFile;
        if(outFile.exists()) {
            outFile.delete();
        }
    }

    @Override
    public void export(Collection<CanListener<?>> listeners) {
        if(isFirst) {
            writeLine(listeners, l -> l.getName());
            isFirst = false;
        }
        writeLine(listeners, l -> l.getValue());
    }

    @SneakyThrows
    private void writeLine(Collection<CanListener<?>> listeners, Function<CanListener, Object> function) {
        StringBuilder sb = new StringBuilder();
        for (CanListener listener : listeners) {
            Object element = function.apply(listener);
            if(element instanceof Number) {
                sb.append(element);
            } else {
                sb.append("\"");
                sb.append(element);
                sb.append("\"");
            }
            sb.append(", ");
        }
        sb.append("\n");
        Files.append(sb.toString(), outFile, Charset.defaultCharset());
    }
}
