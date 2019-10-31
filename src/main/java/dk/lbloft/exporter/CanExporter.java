package dk.lbloft.exporter;

import dk.lbloft.CanListener;

import java.util.Collection;

public interface CanExporter {
    void export(Collection<CanListener> listeners);
}
