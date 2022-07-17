package fr.dynamx.api.contentpack.registry;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;

/**
 * A function creating an {@link ISubInfoType} with its owner and its name
 *
 * @param <T> The {@link ISubInfoTypeOwner} owning the created ISubInfoType
 * @see SubInfoTypeEntry
 */
@FunctionalInterface
public interface ISubInfoTypeCreator<T extends ISubInfoTypeOwner<?>> {
    /**
     * @param owner        The owner of the {@link ISubInfoType}, used to track loading errors
     * @param infoTypeName The name of the {@link ISubInfoType} to create
     * @return The ISubInfoType corresponding to infoTypeName
     */
    ISubInfoType<T> apply(T owner, String infoTypeName);
}