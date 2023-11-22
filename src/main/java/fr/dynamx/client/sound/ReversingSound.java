package fr.dynamx.client.sound;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.BasicEngineModule;

public class ReversingSound extends VehicleSound {
    private final BasicEngineModule engine;
    private final boolean isInterior;

    public ReversingSound(String sound, BaseVehicleEntity<?> vehicle, BasicEngineModule engine, boolean isInterior) {
        super(sound, vehicle);
        this.engine = engine;
        this.isInterior = isInterior;
        setVolumeFactor(isInterior ? 0.7f : 1);
    }

    @Override
    public void update(DynamXSoundHandler handler) {
        if (getState() == EnumSoundState.STOPPING) {
            handler.stopSound(this);
        }
        if (getState() == EnumSoundState.STARTING) {
            setState(EnumSoundState.PLAYING);
            setVolumeFactor(1);
        }
        super.update(handler);
    }

    @Override
    public boolean isSoundActive() {
        return engine.isEngineStarted() && engine.isReversing() && engine.getEngineProperty(VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR) == -1;
    }

    public boolean isInterior() {
        return isInterior;
    }
}
