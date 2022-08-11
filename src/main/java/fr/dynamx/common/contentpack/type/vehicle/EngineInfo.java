package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.EngineModule;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Engine contained in an engine file
 */
public class EngineInfo extends SubInfoTypeOwner<EngineInfo> implements ISubInfoType<ModularVehicleInfoBuilder>, INamedObject {
    private final String packName;
    private final String engineName;

    @PackFileProperty(configNames = "SteeringMethod", required = false, defaultValue = "0")
    public int steeringMethod = 0;

    @PackFileProperty(configNames = "Power")
    private float power;
    @PackFileProperty(configNames = "MaxRPM")
    private float maxRevs;
    @PackFileProperty(configNames = "Braking")
    private float braking;
    @PackFileProperty(configNames = "TurnSpeed", required = false, defaultValue = "0.09")
    private final float turnSpeed = 0.09f;

    public List<Vector3f> points = new ArrayList<>();
    public List<GearInfo> gears = new ArrayList<>();

    private List<EngineSound> soundsEngine;
    public String startingSoundInterior;
    public String startingSoundExterior;

    public EngineInfo(String packName, String name) {
        this.packName = packName;
        this.engineName = name;
    }

    byte i = 0;

    void addGear(GearInfo gear) {
        gear.setId(i);
        gears.add(i, gear);
        i++;
    }

    void addPoint(RPMPower rpmPower) {
        points.add(rpmPower.getRpmPower());
    }

    public void setSounds(List<EngineSound> sounds) {
        soundsEngine = new ArrayList<>();
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
                soundsEngine.add(sound);
        }
    }

    public List<EngineSound> getEngineSounds() {
        return soundsEngine;
    }

    public float getPower() {
        return power;
    }

    public float getBraking() {
        return braking;
    }

    public float getMaxRevs() {
        return maxRevs;
    }

    public float getTurnSpeed() {
        return turnSpeed;
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
    public void onComplete(boolean hotReload) {
        float max = 0;
        for (Vector3f power : points) {
            if (power.x > max)
                max = power.x;
        }
        if (max < maxRevs)
            throw new IllegalArgumentException("Engine's MaxRPM must be lower or equal to the bigger point's RPM");
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder owner) {
        //Fix bug : engine duplicated when using pack sync option
        owner.getSubProperties().removeIf(p -> p.getFullName().equals(getFullName()));
        owner.addSubProperty(this);
    }

    @Nullable
    @Override
    public ModularVehicleInfoBuilder getOwner() {
        return null;
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        modules.add(new EngineModule(entity, this));
    }
}

