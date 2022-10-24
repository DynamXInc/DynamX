package fr.dynamx.api.contentpack.object.subinfo;

import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Owner of {@link ISubInfoType}s <br>
 * See {@link ModularVehicleInfo} for an example of owner
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
}