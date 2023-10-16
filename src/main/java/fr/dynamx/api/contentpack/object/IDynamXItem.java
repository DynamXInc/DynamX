package fr.dynamx.api.contentpack.object;

import fr.dynamx.common.contentpack.type.ObjectInfo;

/**
 * An IDynamXItem is something having an {@link ObjectInfo}. It is, for example, an item or a block
 *
 * @param <T> The type of the owner {@link ObjectInfo}
 */
public interface IDynamXItem<T extends ObjectInfo<?>> {
    /**
     * @return The {@link ObjectInfo} contained
     */
    T getInfo();

    /**
     * Updates the contained {@link ObjectInfo}, used for hot reload
     */
    void setInfo(T info);
}