package fr.dynamx.utils.debug;

import fr.aym.acslib.utils.packetserializer.ISerializablePacket;

import java.util.concurrent.atomic.AtomicInteger;

public class TerrainDebugData implements ISerializablePacket {
    private static final AtomicInteger lastUuid = new AtomicInteger(Integer.MIN_VALUE);

    private int uuid;
    private TerrainDebugRenderer renderer;
    private float[] data;

    public TerrainDebugData() {
    }

    public TerrainDebugData(TerrainDebugRenderer renderer, float[] data) {
        this.uuid = lastUuid.getAndIncrement();
        if (lastUuid.get() == Integer.MAX_VALUE)
            lastUuid.set(Integer.MIN_VALUE);
        this.renderer = renderer;
        this.data = data;
    }

    public int getUuid() {
        return uuid;
    }

    public float[] getData() {
        return data;
    }

    public TerrainDebugRenderer getRenderer() {
        return renderer;
    }

    @Override
    public Object[] getObjectsToSave() {
        return new Object[]{uuid, renderer.ordinal(), data};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        this.uuid = (int) objects[0];
        this.renderer = TerrainDebugRenderer.values()[(int) objects[1]];
        this.data = (float[]) objects[2];
    }
}
