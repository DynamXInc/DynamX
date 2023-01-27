package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;

import java.util.ArrayList;
import java.util.List;

/**
 * Sounds contained in a sounds file
 */
public class SoundListInfo implements ISubInfoTypeOwner<SoundListInfo> {
    private final String name, packName;
    private final List<EngineSound> soundsIn = new ArrayList<>();

    public SoundListInfo(String packName, String name) {
        this.packName = packName;
        this.name = name;
    }

    public void addSound(EngineSound sound) {
        soundsIn.add(sound);
    }

    public List<EngineSound> getSoundsIn() {
        return soundsIn;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPackName() {
        return packName;
    }

    @Override
    public String getFullName() {
        return packName + "." + name;
    }

    @Override
    public void addSubProperty(ISubInfoType<SoundListInfo> property) {
        throw new UnsupportedOperationException("Sound sub info type");
    }

    @Override
    public List<ISubInfoType<SoundListInfo>> getSubProperties() {
        throw new UnsupportedOperationException("Sound sub info type");
    }
}
