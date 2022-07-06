package fr.dynamx.api.contentpack.object;

import fr.dynamx.common.contentpack.type.ObjectInfo;

/**
 * An InfoOwner is something using an {@link ObjectInfo}, it can be, for example, an item or a block
 * @param <T> The type of the owner {@link ObjectInfo}
 */
public interface IInfoOwner<T extends ObjectInfo<?>>
{
    /**
     * @return The {@link ObjectInfo} contained
     */
    T getInfo();

    /**
     * Updates the contained {@link ObjectInfo}, used for hot reload
     */
    void setInfo(T info);
}