package fr.dynamx.common.contentpack.loader;

import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * List of {@link ISubInfoTypeOwner}s with utility methods to load them
 *
 * @param <T> The objects class
 * @see INamedObject
 * @see ObjectLoader
 */
@Getter
public abstract class InfoList<T extends ISubInfoTypeOwner<?>> {
    /**
     * Loaded objects, identified by their full name
     */
    protected final Map<String, T> infos = new HashMap<>();
    /**
     * SubInfoTypesRegistry for this object
     */
    protected final SubInfoTypesRegistry<T> subInfoTypesRegistry;

    /**
     * @param subInfoTypesRegistry
     */
    public InfoList(@Nullable SubInfoTypesRegistry<T> subInfoTypesRegistry) {
        this.subInfoTypesRegistry = subInfoTypesRegistry;
    }

    /**
     * @return The name of the listed objects, used for logging and errors
     */
    public abstract String getName();

    /**
     * Clears infos, used for hot reload
     *
     * @param hot If it's a hot reload
     */
    public void clear(boolean hot) {
        infos.clear();
    }

    /**
     * Puts the info into infos map, and updates other references to these objects (in {@link IInfoOwner}s for example)
     */
    public void loadItems(T info, boolean hot) {
        info.onComplete(hot);
        infos.put(info.getFullName(), info);
    }

    /**
     * Post-loads the objects (shape generation...), and creates the IInfoOwners
     *
     * @param hot True if it's a hot reload
     */
    public abstract void postLoad(boolean hot);

    /**
     * @return The info from the info's full name, or null
     */
    @Nullable
    public T findInfo(String infoFullName) {
        return infos.get(infoFullName);
    }

    public boolean hasSubInfoTypesRegistry() {
        return subInfoTypesRegistry != null;
    }
}
