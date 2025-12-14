package fr.dynamx.api.contentpack.object;

import fr.dynamx.core.common.contentpack.type.ObjectInfo;
import fr.hermes.api.mc.items.HmItem;

/**
 * An IDynamXItem is something having an {@link ObjectInfo}. It is, for example, an item or a block
 *
 * @param <T> The type of the owner {@link ObjectInfo}
 */
public interface IDynamXItem<T extends ObjectInfo<?>> extends HmItem {
    /**
     * @return The {@link ObjectInfo} contained
     */
    T getInfo();

    /**
     * Updates the contained {@link ObjectInfo}, used for hot reload
     */
    void setInfo(T info);
}