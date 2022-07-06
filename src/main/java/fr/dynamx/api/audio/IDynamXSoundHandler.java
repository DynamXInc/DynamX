package fr.dynamx.api.audio;

import com.jme3.math.Vector3f;
import fr.dynamx.client.sound.VehicleSound;
import net.minecraft.client.audio.ISound;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;

import java.util.List;

/**
 * The SoundHandler used for all vehicle sounds (and more if you add sounds) <br>
 *     Owned by the ClientProxy
 */
public interface IDynamXSoundHandler
{
    /**
     * @return The list of all currently playing sounds !
     */
    List<IDynamXSound> getPlayingSounds();

    /**
     * Plays a single sound.  Format of soundName should be modID:soundFileName and the sound should be in the sounds folder <br>
     *     No sounds.json needed
     *
     * @param soundPosition The position of the sound
     * @param soundName The sound file name
     * @param volume The sound volume
     * @param pitch The sound pitch, 1 for no deformation
     * @implNote The default sound distance is 48 blocks with a linear attenuation
     */
    void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch);

    /**
     * Plays a single sound.  Format of soundName should be modID:soundFileName and the sound should be in the sounds folder <br>
     *     No sounds.json needed
     *
     * @param soundPosition The position of the sound
     * @param soundName The sound file name
     * @param volume The sound volume
     * @param pitch The sound pitch, 1 for no deformation
     * @param attenuationType The sound attenuation type - see {@link paulscode.sound.SoundSystemConfig} fields
     * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attenuationType"
     */
    void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch, int attenuationType, float distOrRoll);

    /**
     * @deprecated Wrong naming, use playStreamingSound instead
     */
    @Deprecated
    default void playLoopingSound(Vector3f soundPosition, IDynamXSound sound) {
        playStreamingSound(soundPosition, sound);
    }

    /**
     * Plays a streaming sound, used for engines sound, for example
     *
     * @param soundPosition The position of the sound (can be later updated by the sound)
     * @param sound The sound to play
     * @implNote The default sound distance is 48 blocks with a linear attenuation
     */
    void playStreamingSound(Vector3f soundPosition, IDynamXSound sound);

    /**
     * Plays a streaming sound, used for engines sound, for example
     *
     * @param soundPosition The position of the sound (can be later updated by the sound)
     * @param sound The sound to play
     * @param attenuationType The sound attenuation type - see {@link paulscode.sound.SoundSystemConfig} fields
     * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attenuationType"
     */
    void playStreamingSound(Vector3f soundPosition, IDynamXSound sound, int attenuationType, float distOrRoll);

    /**
     * Tries to stop a sound (the sound must return true in the IDynamXSound.tryStop method)
     */
    void stopSound(IDynamXSound sound);

    /**
     * Sets the volume of a streaming sound
     * @param volume Must be between 0 and 1
     */
    void setVolume(IDynamXSound sound, float volume);

    /**
     * Sets the pitch of a streaming sound
     * @param pitch Must be between 0.5 and 2
     */
    void setPitch(IDynamXSound sound, float pitch);

    /**
     * Sets the position of a streaming sound
     */
    void setPosition(IDynamXSound sound, float x, float y, float z);

    /**
     * Sets the attenuation type of streaming sound
     * @param attenuationType The sound attenuation type - see {@link paulscode.sound.SoundSystemConfig} fields
     */
    void setAttenuationType(IDynamXSound sound, int attenuationType);

    /**
     * Sets the radius of a streaming sound
     * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of the attenuationType
     */
    void setSoundDistance(IDynamXSound sound, float distOrRoll);

    /**
     * Pauses a streaming sound
     */
    void pause(IDynamXSound sound);

    /**
     * Resumes a streaming sound
     */
    void resume(IDynamXSound sound);

    /**
     * Called each tick to update the sounds
     */
    void tick();

    /**
     * Called on mc sound system setup
     */
    void setup(SoundSetupEvent event);

    /**
     * Called on mc sound system load
     */
    void load(SoundLoadEvent event);

    /**
     * Called on world unload
     */
    void unload();
}
