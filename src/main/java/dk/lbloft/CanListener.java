package dk.lbloft;

import com.github.kayak.core.Frame;
import com.github.kayak.core.FrameListener;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public abstract class CanListener<T> implements FrameListener {
    @Getter
    private String name;

    @Getter
    private final Set<Integer> ids = new TreeSet<>();

    private Function<T, T> mapping = t -> t;

    @Getter
    private T value = null;

    @Getter
    private boolean changed;

    public CanListener(String name, Integer ... ids) {
        this.name = name;
        this.ids.addAll(Arrays.asList(ids));
    }

    public CanListener<T> setMapping(Function<T, T> mapping) {
        this.mapping = mapping;
        return this;
    }

    @Override
    public final void newFrame(Frame frame) {
        newFrame(frame.getIdentifier(), ByteBuffer.wrap(frame.getData()));
    }

    /**
     * Report a new frame
     * Can be used for unit testing
     * @param id Can id
     * @param data Can data
     * @return The calculated value
     */
    public final T newFrame(int id, ByteBuffer data) {
        T newValue = mapping.apply(handle(id, data));
        changed = value == null || value.equals(newValue);
        if(changed) onChange(newValue);
        return value = newValue;
    }

    /**
     * Process can data to extract a value
     * @param id Can id
     * @param data Can data (up to 8 bytes)
     * @return The calculated value
     */
    public abstract T handle(int id, ByteBuffer data);

    /**
     * Tricker when the value is changed
     * @param value The new value
     */
    protected void onChange(T value) {}

    @Override
    public String toString() {
        return String.format("%s: %s", name, value.toString());
    }

    // Helpers

    public static CanListener<String> getBit(String name, int pos, int filter, String on, String off, Integer ... ids) {
        return new CanListener<String>(name, ids) {
            public String handle(int id, ByteBuffer data) {
                return (data.get(pos) & filter) == filter ? on : off;
            }
        };
    }

    public static CanListener<Integer> getByte(String name, int pos, Integer ... ids) {
        return new CanListener<Integer>(name, ids) {
            public Integer handle(int id, ByteBuffer data) {
                return data.get(pos) & 0xFF;
            }
        };
    }

    public static CanListener<Integer> getShort(String name, int pos, Integer ... ids) {
        return new CanListener<Integer>(name, ids) {
            public Integer handle(int id, ByteBuffer data) {
                return data.getShort(pos) & 0xFFFF;
            }
        };
    }

    public static CanListener<Integer> getInt(String name, int pos, Integer ... ids) {
        return new CanListener<Integer>(name, ids) {
            public Integer handle(int id, ByteBuffer data) {
                return data.getInt(pos) & 0xFFFFFFFF;
            }
        };
    }

    public static CanListener<String> getHex(Integer id) {
        return getHex(id, 0, 8);
    }

    public static CanListener<String> getHex(Integer id, int pos, int size) {
        return new CanListener<String>(id.toString(), id) {
            @Override
            public String handle(int id, ByteBuffer data) {
                StringBuilder sb = new StringBuilder();
                try {
                    for (int i = pos; i < pos + size; i++) {
                        sb.append(String.format(" %02x", data.get(i) & 0xFF));
                    }
                } catch (IndexOutOfBoundsException e) {}
                return sb.toString().trim();
            }
        };
    }
}
