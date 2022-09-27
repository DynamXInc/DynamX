package fr.dynamx.utils.optimization;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@NotThreadSafe
public class UPDByteArrayPool {
    private static final UPDByteArrayPool INSTANCE = new UPDByteArrayPool(4096);
    private final int arraysSize;
    private final Queue<byte[]> frees = new ConcurrentLinkedQueue<>();

    public UPDByteArrayPool(int arraysSize) {
        this.arraysSize = arraysSize;
    }

    public byte[] get() {
        if (!frees.isEmpty())
            return frees.poll();
        return new byte[arraysSize];
    }

    public void free(byte[] array) {
        if (array.length == arraysSize)
            frees.add(array);
        else
            throw new IllegalArgumentException("Wrong array length : " + array.length + " instead of " + arraysSize);
    }

    public static UPDByteArrayPool getINSTANCE() {
        return INSTANCE;
    }
}
