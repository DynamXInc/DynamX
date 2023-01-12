package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.HelicopterEngineModule;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@RegisteredSubInfoType(name = "HelicopterPhysics", registries = {SubInfoTypeRegistries.HELICOPTER})
public class HelicopterPhysicsInfo extends SubInfoType<ModularVehicleInfo> {

    @Getter
    @PackFileProperty(configNames = "MinPower", defaultValue = "0.4f", description = "The minimum power of the rotor")
    private float minPower = 0.4f;
    @Getter
    @PackFileProperty(configNames = "InclinedGravityFactor", defaultValue = "1.8f", description = "The gravity factor when the helicopter is inclined")
    private float inclinedGravityFactor = 1.8f;
    @Getter
    @PackFileProperty(configNames = "ThrustForce", defaultValue = "3000", description = "The force of the gravity when the helicopter is inclined")
    private float thrustForce = 3000;
    @Getter
    @PackFileProperty(configNames = "VerticalThrustCompensation", defaultValue = "2000", description = "todo")
    private float verticalThrustCompensation = 2000;
    @Getter
    @PackFileProperty(configNames = "BrakeForce", defaultValue = "200", description = "The force apllied to the rotor when the player down")
    private float brakeForce = 500;
    @Getter
    @PackFileProperty(configNames = "MouseYawForce", defaultValue = "2600", description = "The force applied to the rotor when the player move the mouse")
    private float mouseYawForce = 2600;
    @Getter
    @PackFileProperty(configNames = "MousePitchForce", defaultValue = "2000", description = "The force applied to the rotor when the player move the mouse")
    private float mousePitchForce = 2000;
    @Getter
    @PackFileProperty(configNames = "MouseRollForce", defaultValue = "400", description = "The force applied to the rotor when the player move the mouse")
    private float mouseRollForce = 400;
    @Getter
    @PackFileProperty(configNames = "RollForce", defaultValue = "6000", description = "The force of inclination when the player keep the key pressed")
    private float rollForce = 6000;

    private List<EngineSound> soundsEngine;
    public String startingSoundInterior;
    public String startingSoundExterior;

    public HelicopterPhysicsInfo(ISubInfoTypeOwner<ModularVehicleInfo> owner) {
        super(owner);
    }

    public List<EngineSound> getEngineSounds() {
        return soundsEngine;
    }

    @Override
    public String getName() {
        return "HelicopterPhysics of " + getOwner().getName();
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        owner.addSubProperty(this);
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        modules.add(new HelicopterEngineModule(entity));
    }

    @Override
    public void postLoad(ModularVehicleInfo owner, boolean hot) {
        //And sounds
        if (owner.defaultSounds != null) {
            SoundListInfo engineSound = DynamXObjectLoaders.SOUNDS.findInfo(owner.defaultSounds);
            if (engineSound == null)
                throw new IllegalArgumentException("Engine sounds " + owner.defaultSounds + " of " + owner.getFullName() + " were not found, check file names and previous loading errors !");
            setSounds(engineSound.getSoundsIn());
        }
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
}
