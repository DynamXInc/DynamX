package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Engine contained in an engine file
 */
public class BaseEngineInfo extends SubInfoTypeOwner<BaseEngineInfo> implements ISubInfoType<ModularVehicleInfo> {
    private final String packName;
    private final String engineName;

    @Getter
    @PackFileProperty(configNames = "Power")
    private float power;
    @Getter
    @PackFileProperty(configNames = "MaxRPM")
    private float maxRevs;
    @Getter
    @PackFileProperty(configNames = {"EngineBraking", "Braking"})
    private float braking;

    public List<Vector3f> points = new ArrayList<>();

    @Getter
    private List<EngineSound> engineSounds;
    public String startingSoundInterior;
    public String startingSoundExterior;

    public BaseEngineInfo(String packName, String name) {
        this.packName = packName;
        this.engineName = name;
    }

    void addPoint(RPMPower rpmPower) {
        points.add(rpmPower.getRpmPower());
    }

    @Override
    public String getName() {
        return engineName;
    }

    @Override
    public String getPackName() {
        return packName;
    }

    @Override
    public String getFullName() {
        return packName + "." + engineName;
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        float max = 0;
        for (Vector3f power : points) {
            if (power.x > max)
                max = power.x;
        }
        if (max < maxRevs)
            throw new IllegalArgumentException("Engine's MaxRPM must be lower or equal to the bigger point's RPM");

        //Fix bug : engine duplicated when using pack sync option
        owner.getSubProperties().removeIf(p -> p.getFullName().equals(getFullName()));
        owner.addSubProperty(this);
    }

    @Override
    public void postLoad(ModularVehicleInfo owner, boolean hot) {
        if (owner.defaultSounds != null) {
            SoundListInfo engineSound = DynamXObjectLoaders.SOUNDS.findInfo(owner.defaultSounds);
            if (engineSound == null)
                throw new IllegalArgumentException("Engine sounds " + owner.defaultSounds + " of " + owner.getFullName() + " were not found, check file names and previous loading errors !");
            setSounds(engineSound.getSoundsIn());
        }
    }

    public void setSounds(List<EngineSound> sounds) {
        engineSounds = new ArrayList<>();
        for (EngineSound sound : sounds) {
            if (sound.isSpecialSound()) {
                if (sound.getRpmRange()[0] == -1) //A starting sound
                {
                    if (sound.isInterior())
                        startingSoundInterior = sound.getSoundName();
                    else
                        startingSoundExterior = sound.getSoundName();
                }
            } else
                engineSounds.add(sound);
        }
    }

    @Nullable
    @Override
    public ModularVehicleInfo getOwner() {
        return null;
    }

    public void addGear(GearInfo gearInfo) {
    }
}

