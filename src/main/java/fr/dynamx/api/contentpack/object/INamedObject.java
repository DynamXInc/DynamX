package fr.dynamx.api.contentpack.object;

/**
 * An object coming from a pack : has a name and a pack name
 */
public interface INamedObject
{
    /**
     * @return The name of the object
     */
    String getName();

    /**
     * @return The name of the pack owning the object
     */
    String getPackName();

    /**
     * @return A name composed with the packName and name, should be unique
     */
    default String getFullName() {
        return getPackName()+"."+getName();
    }

    /**
     * Called when the info properties has been loaded
     *
     * @param hotReload If it's an hot reload
     */
    default void onComplete(boolean hotReload){}
}
