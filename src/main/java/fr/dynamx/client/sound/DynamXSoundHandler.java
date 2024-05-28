package fr.dynamx.client.sound;

import com.jme3.math.Vector3f;
import fr.dynamx.api.audio.IDynamXSound;
import fr.hermes.forge1122.dynamx.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;

import static fr.dynamx.common.DynamXMain.log;

/**
 * Handles all DynamX sounds (engines...) <br>
 * author DonBruce and modified by the DynamX team
 */
@SideOnly(Side.CLIENT)
public class DynamXSoundHandler {
    //Reflection variables.
    private static final String[] soundSystemNames = {"sndSystem", "field_148620_e"};
    private SoundManager mcSoundManager;
    private SoundSystem mcSoundSystem;
    private int soundSystemStartupDelay = 0;
    private boolean secondStartupTry;

    private final URLStreamHandler resourceStreamHandler = new ResourceStreamHandler();

    /**
     * Called on mc sound system setup
     */
    public void setup(SoundSetupEvent event) {
        mcSoundManager = event.getManager();
    }

    /**
     * Called on mc sound system load
     */
    public void load(SoundLoadEvent event) {
        mcSoundSystem = null;
        secondStartupTry = false;
        soundSystemStartupDelay = 100;
        playingSounds.clear();
    }

    /**
     * Called on world unload
     */
    public void unload() {
        for (IDynamXSound soundID : getPlayingSounds()) {
            if (mcSoundSystem.playing(soundID.getSoundUniqueName())) {
                mcSoundSystem.stop(soundID.getSoundUniqueName());
            }
        }
        playingSounds.clear();
    }

    /**
     * All currently playing (or paused) sounds
     */
    private final List<IDynamXSound> playingSounds = new ArrayList<>();
    /**
     * Sounds waiting to be removed from the playingSounds list
     */
    private final List<IDynamXSound> stoppingSounds = new ArrayList<>();
    /**
     * All trusted sounds (we found the file)
     */
    private final List<String> trustedSounds = new ArrayList<>();
    /**
     * All errored sounds
     */
    private final List<String> erroredSounds = new ArrayList<>();

    /**
     * Called each tick to update the sounds
     */
    public void tick() {
        if (ready()) {
            //Update all sounds
            Vector3fPool.openPool();
            for (IDynamXSound sound : playingSounds) {
                sound.update(this);
            }
            int maxSounds = DynamXConfig.getMaxSounds();
            if (maxSounds > 0 && playingSounds.size() > maxSounds) {
                playingSounds.sort((o1, o2) -> Float.compare(o1.getDistanceToPlayer(), o2.getDistanceToPlayer()));
                for (int i = maxSounds; i < playingSounds.size(); i++) {
                    setSoundVolume(playingSounds.get(i), 0);
                    playingSounds.get(i).onMuted();
                }
            }
            Vector3fPool.closePool();
            //Remove sounds that were stopped
            if (!stoppingSounds.isEmpty()) {
                for (IDynamXSound sound : stoppingSounds) {
                    playingSounds.remove(sound);
                }
                stoppingSounds.clear();
            }
        }
    }

    /**
     * @return The list of all currently playing sounds !
     */
    public List<IDynamXSound> getPlayingSounds() {
        return playingSounds;
    }

    /**
     * Plays a single sound.  Format of soundName should be modID:soundFileName and the sound should be in the sounds folder <br>
     * No sounds.json needed
     *
     * @param soundPosition The position of the sound
     * @param soundName     The sound file name
     * @param volume        The sound volume
     * @param pitch         The sound pitch, 1 for no deformation
     * @implNote The default sound distance is 48 blocks with a linear attenuation
     */
    public void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch) {
        playSingleSound(soundPosition, soundName, volume, pitch, SoundSystemConfig.ATTENUATION_LINEAR, 48);
    }

    /**
     * Plays a single sound.  Format of soundName should be modID:soundFileName and the sound should be in the sounds folder <br>
     * No sounds.json needed
     *
     * @param soundPosition   The position of the sound
     * @param soundName       The sound file name
     * @param volume          The sound volume
     * @param pitch           The sound pitch, 1 for no deformation
     * @param attenuationType The sound attenuation type - see {@link paulscode.sound.SoundSystemConfig} fields
     * @param distOrRoll      Either the fading distance or rolloff factor, depending on the value of "attenuationType"
     */
    public void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch, int attenuationType, float distOrRoll) {
        if (ready() && DynamXConfig.getMasterSoundVolume() > 0) {
            try {
                //Need to add the DynamX_Main.MODID: prefix as the URL will trim off the first section, leading to a bad parse.
                URL soundURL = new URL(null, DynamXConstants.ID + ":" + soundName + ".ogg", resourceStreamHandler);
                if (trustedSounds.contains(soundName) || soundURL.openStream() != null) {
                    if (!trustedSounds.contains(soundName))
                        trustedSounds.add(soundName);
                    Vec3d soundNormalizedPosition = new Vec3d(soundPosition.x, soundPosition.y, soundPosition.z);
                    String soundTempName = mcSoundSystem.quickPlay(false, soundURL, soundURL.getFile(), false, (float) soundNormalizedPosition.x, (float) soundNormalizedPosition.y, (float) soundNormalizedPosition.z, attenuationType, distOrRoll);
                    mcSoundSystem.setVolume(soundTempName, MathHelper.clamp(volume * DynamXConfig.getMasterSoundVolume(), 0.0F, 1.0F));
                    mcSoundSystem.setPitch(soundTempName, pitch);
                }
            } catch (FileNotFoundException e) {
                if (!erroredSounds.contains(soundName)) {
                    log.error("Sound " + soundName + " not found", e);
                    erroredSounds.add(soundName);
                }
            } catch (Exception e) {
                if (!erroredSounds.contains(soundName)) {
                    log.error("COULD NOT PLAY SOUND:" + soundName, e);
                    erroredSounds.add(soundName);
                }
            }
        }
    }

    /**
     * Plays a streaming sound, used for engines sound, for example
     *
     * @param soundPosition The position of the sound (can be later updated by the sound)
     * @param sound         The sound to play
     * @implNote The default sound distance is 48 blocks with a linear attenuation
     */
    public void playStreamingSound(Vector3f soundPosition, IDynamXSound sound) {
        playStreamingSound(soundPosition, sound, SoundSystemConfig.ATTENUATION_LINEAR, 48);
    }

    /**
     * Plays a streaming sound, used for engines sound, for example
     *
     * @param soundPosition   The position of the sound (can be later updated by the sound)
     * @param sound           The sound to play
     * @param attenuationType The sound attenuation type - see {@link paulscode.sound.SoundSystemConfig} fields
     * @param distOrRoll      Either the fading distance or rolloff factor, depending on the value of "attenuationType"
     */
    public void playStreamingSound(Vector3f soundPosition, IDynamXSound sound, int attenuationType, float distOrRoll) {
        if (ready() && DynamXConfig.getMasterSoundVolume() > 0) {
            if (playingSounds.contains(sound))
                throw new IllegalStateException("Sound " + sound + " is already playing !");
            String soundID = sound.getSoundUniqueName();
            String soundName = soundID.substring(soundID.indexOf('_') + 1);
            if (!playingSounds.contains(sound) && !Minecraft.getMinecraft().isGamePaused()) {
                try {
                    URL soundURL = new URL(null, DynamXConstants.ID + ":" + soundName + ".ogg", resourceStreamHandler);
                    if (trustedSounds.contains(soundName) || soundURL.openStream() != null) {
                        if (!trustedSounds.contains(soundName))
                            trustedSounds.add(soundName);
                        playingSounds.add(sound);
                        mcSoundSystem.newSource(false, soundID, soundURL, soundURL.toString(), true, soundPosition.x, soundPosition.y, soundPosition.z, attenuationType, distOrRoll);
                        setSoundVolume(sound, sound.getVolume());
                        mcSoundSystem.play(soundID);
                        sound.onStarted();
                    }
                } catch (FileNotFoundException e) {
                    if (!erroredSounds.contains(soundName)) {
                        log.error("Looping sound " + soundName + " not found", e);
                        erroredSounds.add(soundName);
                    }
                } catch (Exception e) {
                    if (!erroredSounds.contains(soundName)) {
                        log.error("(0x1) COULD NOT PLAY LOOPING SOUND:" + soundName, e);
                        erroredSounds.add(soundName);
                    }
                }
            }
        }
    }

    /**
     * Tries to stop a sound (the sound must return true in the IDynamXSound.tryStop method)
     */
    public void stopSound(IDynamXSound sound) {
        if (playingSounds.contains(sound)) {
            if (sound.tryStop()) {
                try {
                    if (mcSoundSystem != null) {
                        mcSoundSystem.stop(sound.getSoundUniqueName());
                    }
                    stoppingSounds.add(sound);
                } catch (Exception e) {
                    log.error("COULD NOT STOP LOOPING SOUND:" + sound.getSoundUniqueName(), new RuntimeException(e));
                }
            }
        }
    }

    public SoundManager getMcSoundManager() {
        return mcSoundManager;
    }

    public SoundSystem getMcSoundSystem() {
        return mcSoundSystem;
    }

    /**
     * Sets the volume of a streaming sound
     *
     * @param volume Must be between 0 and 1
     */
    public void setSoundVolume(IDynamXSound sound, float volume) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setVolume(sound.getSoundUniqueName(), MathHelper.clamp(volume * DynamXConfig.getMasterSoundVolume(), 0.0F, 1.0F));
    }

    /**
     * Sets the pitch of a streaming sound
     *
     * @param pitch Must be between 0.5 and 2
     */
    public void setPitch(IDynamXSound sound, float pitch) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setPitch(sound.getSoundUniqueName(), pitch);
    }

    /**
     * Sets the attenuation type of streaming sound
     *
     * @param attenuationType The sound attenuation type - see {@link paulscode.sound.SoundSystemConfig} fields
     */
    public void setAttenuationType(IDynamXSound sound, int attenuationType) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setAttenuation(sound.getSoundUniqueName(), attenuationType);
    }

    /**
     * Sets the radius of a streaming sound
     *
     * @param radius Either the fading distance or rolloff factor, depending on the value of the attenuationType
     */
    public void setSoundDistance(IDynamXSound sound, float radius) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setDistOrRoll(sound.getSoundUniqueName(), radius);
    }

    /**
     * Sets the position of a streaming sound
     */
    public void setPosition(IDynamXSound sound, float x, float y, float z) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setPosition(sound.getSoundUniqueName(), x, y, z);
    }

    /**
     * Pauses a streaming sound
     */
    public void pause(IDynamXSound sound) {
        if (playingSounds.contains(sound))
            mcSoundSystem.pause(sound.getSoundUniqueName());
    }

    /**
     * Resumes a streaming sound
     */
    public void resume(IDynamXSound sound) {
        if (playingSounds.contains(sound))
            mcSoundSystem.play(sound.getSoundUniqueName());
    }


    public void setMasterVolume(float masterVolume) {
        DynamXConfig.setMasterSoundVolume(masterVolume);
        for(IDynamXSound sound : playingSounds) {
            setSoundVolume(sound, sound.getVolume());
        }
    }

    /**
     * @return False if mcSoundSystem is null, because we wait for it to be ready, or if no world is loaded, or calling the function from the server side
     */
    private boolean ready() {
        if (Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().world.isRemote) {
            //If we don't have the running instance of the SoundSystem, get it now.
            if (mcSoundSystem == null) {
                if (soundSystemStartupDelay > 0) {
                    --soundSystemStartupDelay;
                    return false;
                }
                initSoundSystemHooks();
            }
            return true;
        }
        return false;
    }

    /**
     * Populates the static soundsystem fields when called. Used when either the regular or
     * looping sound systems first try to play a sound and notice they are not populated yet.
     */
    private void initSoundSystemHooks() {
        Exception lastException = null;

        if (mcSoundManager == null) {
            mcSoundManager = ObfuscationReflectionHelper.getPrivateValue(SoundHandler.class, Minecraft.getMinecraft().getSoundHandler(), 5);
            if (secondStartupTry)
                throw new IllegalStateException("McSoundManager is null !");
            soundSystemStartupDelay = 1500;
            secondStartupTry = true;
            return;
        } else {
            mcSoundManager = ObfuscationReflectionHelper.getPrivateValue(SoundHandler.class, Minecraft.getMinecraft().getSoundHandler(), 5);
        }

        //Get the SoundSystem from the SoundManager.
        for (String soundSystemName : soundSystemNames) {
            try {
                Field soundSystemField = SoundManager.class.getDeclaredField(soundSystemName);
                soundSystemField.setAccessible(true);
                mcSoundSystem = (SoundSystem) soundSystemField.get(mcSoundManager);
            } catch (Exception e) {
                lastException = e;
            }
        }
        if (mcSoundSystem == null) {
            log.error("ERROR IN SOUND SYSTEM REFLECTION!  COULD NOT FIND SOUNDSYSTEM!", new RuntimeException(lastException));
        }
    }

    public float getMasterVolume() {
        return DynamXConfig.getMasterSoundVolume();
    }

    /**
     * Custom stream handler for our sounds, to bypass sounds.json
     */
    private static class ResourceStreamHandler extends URLStreamHandler {

        public ResourceStreamHandler() {
        }
        protected URLConnection openConnection(URL connection) {
            return new URLConnection(connection) {
                @Override
                public void connect() {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    String soundName = connection.toString();
                    String packID = soundName.substring(0, soundName.indexOf(':'));
                    soundName = soundName.substring(packID.length() + 1);
                    IResource res;
                    try {
                        res = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(DynamXConstants.ID, "sounds/" + soundName));
                    } catch (IOException e) {
                        //Handled at upper level log.error("Sound "+connection.toString()+" cannot be loaded !", e);
                        throw e;
                    }
                    return res.getInputStream();
                }
            };
        }

    }
}
