package fr.dynamx.client.sound;

import com.jme3.math.Vector3f;
import fr.dynamx.api.audio.IDynamXSound;
import fr.dynamx.api.audio.IDynamXSoundHandler;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
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
 * Default DynamX implementation of {@link IDynamXSoundHandler}
 * <p>
 * author DonBruce and modified by the DynamX team
 */
@SideOnly(Side.CLIENT)
public class DynamXSoundHandler implements IDynamXSoundHandler {
    //Reflection variables.
    private static final String[] soundSystemNames = {"sndSystem", "field_148620_e"};
    private SoundManager mcSoundManager;
    private SoundSystem mcSoundSystem;
    private int soundSystemStartupDelay = 0;
    private boolean secondStartupTry;

    private final URLStreamHandler resourceStreamHandler = new ResourceStreamHandler();

    @Override
    public void setup(SoundSetupEvent event) {
        mcSoundManager = event.getManager();
    }

    @Override
    public void load(SoundLoadEvent event) {
        mcSoundSystem = null;
        secondStartupTry = false;
        soundSystemStartupDelay = 100;
        playingSounds.clear();
    }

    @Override
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

    @Override
    public void tick() {
        if (ready()) {
            //Update all sounds
            Vector3fPool.openPool();
            for (IDynamXSound sound : playingSounds) {
                sound.update(this);
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

    @Override
    public List<IDynamXSound> getPlayingSounds() {
        return playingSounds;
    }

    @Override
    public void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch) {
        playSingleSound(soundPosition, soundName, volume, pitch, SoundSystemConfig.ATTENUATION_LINEAR, 48);
    }

    @Override
    public void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch, int attenuationType, float distOrRoll) {
        if (ready()) {
            try {
                //Need to add the DynamX_Main.MODID: prefix as the URL will trim off the first section, leading to a bad parse.
                URL soundURL = new URL(null, DynamXConstants.ID + ":" + soundName + ".ogg", resourceStreamHandler);
                if (trustedSounds.contains(soundName) || soundURL.openStream() != null) {
                    if (!trustedSounds.contains(soundName))
                        trustedSounds.add(soundName);
                    Vec3d soundNormalizedPosition = new Vec3d(soundPosition.x, soundPosition.y, soundPosition.z);
                    String soundTempName = mcSoundSystem.quickPlay(false, soundURL, soundURL.getFile(), false, (float) soundNormalizedPosition.x, (float) soundNormalizedPosition.y, (float) soundNormalizedPosition.z, attenuationType, distOrRoll);
                    mcSoundSystem.setVolume(soundTempName, volume);
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

    @Override
    public void playStreamingSound(Vector3f soundPosition, IDynamXSound sound) {
        playStreamingSound(soundPosition, sound, SoundSystemConfig.ATTENUATION_LINEAR, 48);
    }

    @Override
    public void playStreamingSound(Vector3f soundPosition, IDynamXSound sound, int attenuationType, float distOrRoll) {
        if (ready()) {
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
                        mcSoundSystem.newSource(false, soundID, soundURL, soundURL.toString(), true, soundPosition.x, soundPosition.y, soundPosition.z, attenuationType, distOrRoll);
                        mcSoundSystem.play(soundID);
                        sound.onStarted();
                        playingSounds.add(sound);
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

    @Override
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

    @Override
    public void setVolume(IDynamXSound sound, float volume) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setVolume(sound.getSoundUniqueName(), volume);
    }

    @Override
    public void setPitch(IDynamXSound sound, float pitch) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setPitch(sound.getSoundUniqueName(), pitch);
    }

    @Override
    public void setAttenuationType(IDynamXSound sound, int attenuationType) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setAttenuation(sound.getSoundUniqueName(), attenuationType);
    }

    @Override
    public void setSoundDistance(IDynamXSound sound, float radius) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setDistOrRoll(sound.getSoundUniqueName(), radius);
    }

    @Override
    public void setPosition(IDynamXSound sound, float x, float y, float z) {
        if (playingSounds.contains(sound))
            mcSoundSystem.setPosition(sound.getSoundUniqueName(), x, y, z);
    }

    @Override
    public void pause(IDynamXSound sound) {
        if (playingSounds.contains(sound))
            mcSoundSystem.pause(sound.getSoundUniqueName());
    }

    @Override
    public void resume(IDynamXSound sound) {
        if (playingSounds.contains(sound))
            mcSoundSystem.play(sound.getSoundUniqueName());
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
