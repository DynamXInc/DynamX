package fr.dynamx.api.contentpack.object.subinfo;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;

import javax.annotation.Nullable;

/**
 * A sub info type is an object containing specific properties in an info file <br>
 * Example : <br>
 * <code>
 * Shape_Top{ <br>
 * ShapeScale: 0.936054 1.60526 0.280718 <br>
 * ShapePosition: -0.023108 0.605695 1.45133 <br>
 * }
 * </code> <br> is a {@link fr.dynamx.common.contentpack.parts.PartShape}
 * <br>For vehicles, it should be registered in the registry in order to be used by the loader
 *
 * @param <T> The type of the {@link ISubInfoTypeOwner} owning this sub info
 * @see SubInfoTypesRegistry
 * @see SubInfoType
 */
public interface ISubInfoType<T extends ISubInfoTypeOwner<?>> extends INamedObject {
    /**
     * Called when this sub info has been read and should be added to the corresponding {@link ISubInfoTypeOwner} <br>
     * See {@link ModularVehicleInfo} for the most common {@link ISubInfoTypeOwner}
     * <p>
     * todo update doc
     */
    void appendTo(T owner);

    /**
     * @return The owner of this property
     */
    @Nullable
    T getOwner();

    /**
     * Post loads this sub info <br>
     * Can be used to complete this sub info with other infos (for example, add the 'sounds_' to an engine)
     *
     * @param owner The owner of this sub property
     * @param hot If it's a hot reloading (info owners already created)
     */
    default void postLoad(T owner, boolean hot) {}

    /**
     * Adds the {@link IPhysicsModule}s associated with this sub info type to the given entity
     *
     * @param entity  The entity being initialized
     * @param modules The modules list where you should add your module(s)
     */
    default void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
    }

    /**
     * todo doc
     *
     * @return
     */
    default INamedObject getRootOwner() {
        INamedObject parent = this;
        while (parent instanceof ISubInfoType && ((ISubInfoType<?>) parent).getOwner() != null) {
            parent = ((ISubInfoType<?>) parent).getOwner();
        }
        return parent;
    }
}
