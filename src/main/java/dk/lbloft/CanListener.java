package dk.lbloft;

import com.github.kayak.core.Bus;
import com.github.kayak.core.Frame;
import com.github.kayak.core.FrameListener;
import com.github.kayak.core.Subscription;
import lombok.Getter;

import java.nio.ByteBuffer;

public abstract class CanListener<T> implements FrameListener {
    @Getter
    private String name;

    @Getter
    private T value = null;

    public CanListener(String name, Bus bus, int ... ids) {
        this.name = name;
        for (int id : ids) {
            new Subscription(this, bus).subscribe(id, false);
        }
    }

    @Override
    public final void newFrame(Frame frame) {
        value = handle(frame.getIdentifier(), ByteBuffer.wrap(frame.getData()));
    }

    public abstract T handle(int id, ByteBuffer data);

    @Override
    public String toString() {
        return String.format("%s: %s", name, value.toString());
    }

    public static CanListener<String> getBit(Bus bus, String name, int pos, int filter, String on, String off, int ... ids) {
        return new CanListener<String>(name, bus, ids) {
            public String handle(int id, ByteBuffer data) {
                return (data.get(pos) & filter) == filter ? on : off;
            }
        };
    }

    public static CanListener<Integer> getByte(Bus bus, String name, int pos, int ... ids) {
        return new CanListener<Integer>(name, bus, ids) {
            public Integer handle(int id, ByteBuffer data) {
                return data.get(pos) & 0xFF;
            }
        };
    }

    public static CanListener<Integer> getShort(Bus bus, String name, int pos, int ... ids) {
        return new CanListener<Integer>(name, bus, ids) {
            public Integer handle(int id, ByteBuffer data) {
                return data.getShort(pos) & 0xFFFF;
            }
        };
    }

    public static CanListener<Integer> getInt(Bus bus, String name, int pos, int ... ids) {
        return new CanListener<Integer>(name, bus, ids) {
            public Integer handle(int id, ByteBuffer data) {
                return data.getInt(pos) & 0xFFFFFFFF;
            }
        };
    }
}
