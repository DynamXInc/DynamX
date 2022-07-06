package fr.dynamx.client.sound;

import fr.dynamx.api.audio.IDynamXSoundHandler;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;

import static fr.dynamx.api.entities.VehicleEntityProperties.getPropertyIndex;

public class SkiddingSound extends VehicleSound
{
    private final WheelsModule wheelsModule;
    private boolean playing;

    public SkiddingSound(String sound, BaseVehicleEntity<?> vehicle, WheelsModule wheelsModule){
        super(sound, vehicle);
        this.wheelsModule = wheelsModule;
        this.playing = false;
    }

    @Override
    public void update(IDynamXSoundHandler handler) {
        super.update(handler);
        float numSkdding = 0;
        for (int i = 0; i < vehicleEntity.getPackInfo().getPartsByType(PartWheel.class).size(); i++) {
            if(wheelsModule.getPropulsionProperties()[getPropertyIndex(i, VehicleEntityProperties.EnumWheelProperties.SKIDINFO)] < 0.2f) {
                numSkdding+=1-wheelsModule.getPropulsionProperties()[getPropertyIndex(i, VehicleEntityProperties.EnumWheelProperties.SKIDINFO)];
            }
        }
        //System.out.println(playing+" "+numSkdding);
        if(numSkdding > 0) {
            setVolumeFactor(numSkdding*10);
            playing = true;
        }
        else if(playing && numSkdding == 0) {
            setVolumeFactor(0);
           playing = false;
        }
    }

    @Override
    public boolean isSoundActive(){
        return true;
    }
}
