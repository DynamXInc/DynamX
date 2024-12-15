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
    private EngineConfigType configType;

    @Getter
    @PackFileProperty(configNames = {"MaxPower", "Power"}, required = false)
    private float maxPower = -1;
    @Getter
    @PackFileProperty(configNames = "MaxTorque", required = false)
    private float maxTorque = -1;

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

    protected void addPoint(EngineConfigType engineConfigType, RPMPower rpmPower) {
        if (configType == null) {
            configType = engineConfigType;
        } else if (configType != engineConfigType) {
            throw new IllegalArgumentException("Mismatching RPMPoint types ! The first one is " + configType + ". The current one is " + engineConfigType);
        }
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
        Vector3f max = new Vector3f(0, 0, 0);
        for (Vector3f power : points) {
            // rpm test
            if (power.x > max.x)
                max.x = power.x;
            // hp/torque test
            if (power.y > max.y) {
                max.y = power.y;
                max.z = power.x; // rpm at which the hp/torque is maxed
            }
        }
        if (max.x < maxRevs)
            throw new IllegalArgumentException("Engine's MaxRPM must be lower or equal to the bigger point's RPM");
        if (configType == EngineConfigType.TORQUE && maxTorque == -1) {
            throw new IllegalArgumentException("Engine's MaxTorque must be set when using a torque curve");
        } else if (configType == EngineConfigType.POWER && maxPower == -1) {
            throw new IllegalArgumentException("Engine's MaxPower must be set when using a power curve");
        }
        //Fix bug : engine duplicated when using pack sync option
        owner.getSubProperties().removeIf(p -> p.getFullName().equals(getFullName()));
        owner.addSubProperty(this);
    }

    @Nullable
    @Override
    public ModularVehicleInfo getOwner() {
        return null;
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

    public void addGear(GearInfo gearInfo) {
    }

    public enum EngineConfigType {
        /**
         * Legacy power curve support
         */
        POWER,
        /**
         * Newer torque support (and better physically)
         */
        TORQUE
    }
}

