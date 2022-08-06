package fr.dynamx.api.contentpack.object.subinfo;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry;
import fr.dynamx.common.contentpack.ModularVehicleInfo;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.contentpack.type.ObjectInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Owner of {@link ISubInfoType}s <br>
 * See {@link ModularVehicleInfoBuilder} and {@link Vehicle} for an example of owner
 *
 * @param <T> The type of the implementing class
 */
public abstract class SubInfoTypeOwner<T extends SubInfoTypeOwner<T>> implements ISubInfoTypeOwner<T> {
    /**
     * List of owned {@link ISubInfoType}s
     */
    protected final List<ISubInfoType<T>> subProperties = new ArrayList<>();

    /**
     * Adds an {@link ISubInfoType}
     */
    public void addSubProperty(ISubInfoType<T> property) {
        subProperties.add(property);
    }

    /**
     * @return The list of owned {@link ISubInfoType}s
     */
    public List<ISubInfoType<T>> getSubProperties() {
        return subProperties;
    }

    /**
     * An {@link SubInfoTypeOwner} using a builder, see {@link ModularVehicleInfoBuilder} for example
     *
     * @param <T> The class of the builder
     * @param <B> The class of the object to build
     * @see fr.dynamx.common.contentpack.loader.BuildableInfoLoader
     */
    public abstract static class BuildableSubInfoTypeOwner<T extends SubInfoTypeOwner<T>, B extends ObjectInfo<?>> extends SubInfoTypeOwner<T> implements INamedObject {
        /**
         * If errored, the object will not be built, and not present in game
         */
        public abstract boolean isErrored();

        /**
         * Builds this object
         */
        public abstract B build();
    }

    /**
     * {@link ModularVehicleInfoBuilder} owner implementation
     *
     * @see SubInfoTypesRegistry
     */
    public abstract static class Vehicle extends BuildableSubInfoTypeOwner<ModularVehicleInfoBuilder, ModularVehicleInfo<?>> implements ISubInfoType<ModularVehicleInfoBuilder> {
        @Override //A sub info types owner is an info type himself (it is THE root owning all properties)
        public void appendTo(ModularVehicleInfoBuilder partInfo) {
        }
    }
}