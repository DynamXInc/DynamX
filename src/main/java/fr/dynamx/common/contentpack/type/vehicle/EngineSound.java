package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.registry.PackFileProperty;

/**
 * Info of an engine sound, containing its name and its rpm
 *
 * @see SoundListInfo
 */
public class EngineSound implements INamedObject {
    private static int lastId;

    private final String packName;
    public int id;

    /**
     * RPM range of this sound (array containing the min and the max) <br>
     * OR if it's the starting sound, equals to {-1}
     */
    @PackFileProperty(configNames = "RPMRange")
    private int[] rpmRange;
    @PackFileProperty(configNames = "Sound")
    private String soundName;
    @PackFileProperty(configNames = "PitchRange", required = false)
    private float[] pitchRange = new float[]{0.5f, 2.0f};

    private boolean isInterior;

    public EngineSound(String packName, int[] rpmRange) {
        id = lastId;
        lastId++;
        this.packName = packName;
        this.rpmRange = rpmRange;
    }

    /**
     * @return True if it's not a normal EngineSound with min and max rpm
     */
    public boolean isSpecialSound() {
        return rpmRange.length == 1;
    }

    public int[] getRpmRange() {
        return rpmRange;
    }

    public String getSoundName() {
        return soundName;
    }

    public float[] getPitchRange() {
        return pitchRange;
    }

    public void setInterior(boolean interior) {
        isInterior = interior;
    }

    public boolean isInterior() {
        return isInterior;
    }

    @Override
    public String getName() {
        return "EngineSound_" + soundName;
    }

    @Override
    public String getPackName() {
        return packName;
    }
}
