package fr.dynamx.client.sound;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.audio.IDynamXSoundHandler;
import fr.dynamx.common.entities.BaseVehicleEntity;

public class EngineSound extends VehicleSound {
    private final IEngineSoundHandler engine;
    private final fr.dynamx.common.contentpack.type.vehicle.EngineSound soundIn;

    public EngineSound(fr.dynamx.common.contentpack.type.vehicle.EngineSound sound, BaseVehicleEntity<?> vehicle, IEngineSoundHandler engine) {
        super(sound.getSoundName(), vehicle);
        this.soundIn = sound;
        this.engine = engine;
    }

    @Override
    public void onStarted() {
        setState(EnumSoundState.STARTING);
        setVolumeFactor(0);
    }

    @Override
    public boolean tryStop() {
        if (getVolumeFactor() <= 0 || vehicleEntity.isDead) {
            setState(EnumSoundState.STOPPED);
            setVolumeFactor(0);
            return true;
        } else {
            setState(EnumSoundState.STOPPING);
            return false;
        }
    }

    @Override
    public void update(IDynamXSoundHandler handler) {
        if (getState() == EnumSoundState.STOPPING) {
            setVolumeFactor(getVolumeFactor() - 0.02f);
            if (getVolumeFactor() <= 0) {
                handler.stopSound(this);
            }
        }
        if (getState() == EnumSoundState.STARTING) {
            setVolumeFactor(getVolumeFactor() + 0.02f);
            if (getVolumeFactor() >= 1) {
                setState(EnumSoundState.PLAYING);
                setVolumeFactor(1);
            }
        }
        super.update(handler);
    }

    public boolean shouldPlay(float rpm, boolean forInterior) {
        return soundIn.isInterior() == forInterior && soundIn.getRpmRange()[0] <= rpm && soundIn.getRpmRange()[1] >= rpm;
    }

    @Override
    public boolean isSoundActive() {
        return engine.isEngineStarted() || getState() == EnumSoundState.STOPPING;
    }

    @Override
    protected float getCurrentVolume() {
        return (30F * (engine.getSoundPitch()));
    }

    @Override
    protected float getCurrentPitch() {
        float pitch = engine.getSoundPitch();
        float min = soundIn.getPitchRange()[0];

        pitch = (soundIn.getPitchRange()[1] - min) * pitch + min;
        // add a bit of interpolation for when we change gears.
        // this effectively stops the sound from jumping from full revs to low revs and vice versa.
        // maybe a smoothstep or some kind of exponent would work better here.
        //value = DynamXMath.interpolateLinear(5.0f, lastValue, value);

        //float pitch = DynamXMath.clamp(value, 0.5f, 2.0f);

        //lastValue = value;

        //System.out.println(pitch+" "+value+" "+soundIn.pitchRange[1]+" "+min);
        return pitch;
    }

    public interface IEngineSoundHandler {
        boolean isEngineStarted();

        float getSoundPitch();
    }
}
