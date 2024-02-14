package fr.dynamx.api.contentpack.object.subinfo;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Owner of {@link ISubInfoType}s <br>
 * See {@link SubInfoTypeOwner} for an example of owner
 *
 * @param <T> The type of the implementing class
 */
public interface ISubInfoTypeOwner<T extends ISubInfoTypeOwner<?>> extends INamedObject {
    /**
     * Adds an {@link ISubInfoType}
     */
    void addSubProperty(ISubInfoType<T> property);

    /**
     * @return The list of owned {@link ISubInfoType}s
     */
    List<ISubInfoType<T>> getSubProperties();

    /**
     * @return The configured properties <strong>before</strong> parsing this object <br>
     * The list is modified by the object loader and then contains all the loaded properties
     */
    default List<PackFilePropertyData<?>> getInitiallyConfiguredProperties() {
        return new ArrayList<>();
    }

    /**
     * @return The ISubInfoType matching to the given clazz, or null
     */
    @Nullable
    default <A extends ISubInfoType<T>> A getSubPropertyByType(Class<A> clazz) {
        return (A) this.getSubProperties().stream().filter(p -> clazz.equals(p.getClass())).findFirst().orElseGet(() -> null); //Don't remove the () -> : idea doesn't understand it
    }

    /**
     * If null, the object loader will use the last used registry (usually the default one) <br>
     * @return The sub info types registry for this object
     */
    default SubInfoTypesRegistry<T> getSubInfoTypesRegistry() {
        return null;
    }
}