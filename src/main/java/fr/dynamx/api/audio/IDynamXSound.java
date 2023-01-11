package fr.dynamx.api.audio;

import fr.dynamx.client.sound.DynamXSoundHandler;

/**
 * Pattern for DynamXSound, used in {@link IDynamXSoundHandler} <br>
 * You don't need any sounds.json to use this system
 */
public interface IDynamXSound {
    /**
     * @param handler The {@link IDynamXSoundHandler} owning this sound, use it to stop the sound or change its volume
     */
    void update(DynamXSoundHandler handler);

    /**
     * @return The unique name of this sound, should be different for each entity
     */
    String getSoundUniqueName();

    /**
     * Called when the sound is started, use it to make soft transitions
     */
    void onStarted();

    /**
     * Called when the sound need to be started
     *
     * @return False to cancel sound stop, if you want to make a soft transition, you should call IDynamXSoundHandler.stopSound at the end of the transition, and return true here
     */
    boolean tryStop();

    //TODO DOC
    /**

     * @return The volume.
     */
    float getVolume();
}
