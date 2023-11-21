package fr.dynamx.client.sound;

import com.jme3.math.Vector3f;
import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.audio.IDynamXSound;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

public abstract class VehicleSound implements IDynamXSound {
    protected final BaseVehicleEntity<?> vehicleEntity;
    protected final EntityPlayer player;
    private final String soundName;

    protected Vector3f playerPos;
    protected Vector3f sourcePos;

    private EnumSoundState state = EnumSoundState.STOPPED;

    public VehicleSound(String soundName, BaseVehicleEntity<?> vehicle) {
        this.soundName = soundName;

        this.vehicleEntity = vehicle;
        this.player = Minecraft.getMinecraft().player;

        this.playerPos = new Vector3f((float) player.posX, (float) player.posY, (float) player.posZ);
        this.sourcePos = vehicle.physicsPosition;
    }

    public void setState(EnumSoundState state) {
        this.state = state;
    }

    public EnumSoundState getState() {
        return state;
    }

    @Override
    public void onStarted() {
        setState(EnumSoundState.PLAYING);
    }

    @Override
    public boolean tryStop() {
        setState(EnumSoundState.STOPPED);
        return true;
    }

    @Override
    public void update(DynamXSoundHandler handler) {
        if (isSoundActive() && !vehicleEntity.isDead) {
            this.playerPos.set((float) player.posX, (float) player.posY, (float) player.posZ);
            this.sourcePos.set(vehicleEntity.physicsPosition);

            handler.setSoundVolume(this, getVolume());
            handler.setPitch(this, getPitch());
            //Set the position to 5 blocks from the player in the direction of the sound.
            //Don't worry about motion as that's used in the sound itself for the pitch.
            Vec3d soundNormalizedPosition = vehicleEntity.getPositionVector();//sourcePos.subtract(playerPos).normalize().scale(5).add(player.getPositionVector());
            handler.setPosition(this, (float) soundNormalizedPosition.x, (float) soundNormalizedPosition.y, (float) soundNormalizedPosition.z);
            if (Minecraft.getMinecraft().isGamePaused()) {
                handler.pause(this);
            } else {
                handler.resume(this);
            }
        } else {
            if (vehicleEntity.isDead) {
                handler.stopSound(this);
            } else if (getState() != EnumSoundState.STOPPED) {
                setState(EnumSoundState.STOPPING);
            }
        }
    }

    public float getPosX() {
        return sourcePos.x;
    }

    public float getPosY() {
        return sourcePos.y;
    }

    public float getPosZ() {
        return sourcePos.z;
    }

    private float volumeFactor = 1;

    public void setVolumeFactor(float factor) {
        volumeFactor = factor;
    }

    public float getVolumeFactor() {
        return volumeFactor;
    }

    @Override
    public float getVolume() {
        //If the player is riding the source, volume will either be 1.0 or 0.5.
        if (vehicleEntity.equals(player.getRidingEntity())) {
            return 1.0F * volumeFactor;
        }

        //Sound is not internal and player is not riding the source.  Volume is player distance.
        /*/playerPos.distanceTo(sourcePos)*/
        return getCurrentVolume() * volumeFactor;
    }

    public float getPitch() {
        //If the player is riding the sound source, don't apply a doppler effect.
        if (vehicleEntity.equals(player.getRidingEntity())) {
            return getCurrentPitch();
        } else {
            Vector3f temp = Vector3fPool.get(playerPos);
            Vector3f temp2 = Vector3fPool.get(sourcePos);
            double soundVelocity = Vector3fPool.get(playerPos).subtractLocal(sourcePos.x, sourcePos.y, sourcePos.z).length() - temp.addLocal((float) player.motionX, (float) player.motionY, (float) player.motionZ)
                    .addLocal(temp2.addLocal((float) vehicleEntity.motionX, (float) vehicleEntity.motionY, (float) vehicleEntity.motionZ).multLocal(-1)).length();
            return (float) (getCurrentPitch() * (1 + soundVelocity / 10F));
        }
    }

    public String getSoundName() {
        return soundName;
    }

    @Override
    public String getSoundUniqueName() {
        return vehicleEntity.getEntityId() + "_" + getSoundName();
    }

    public abstract boolean isSoundActive();

    protected float getCurrentVolume() {
        return 1.0F;
    }

    protected float getCurrentPitch() {
        return 1.0F;
    }

    @Override
    public float getDistanceToPlayer() {
        return playerPos.distance(sourcePos);
    }

    @Override
    public void onMuted() {

    }
}
