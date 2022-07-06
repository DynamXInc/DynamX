package fr.dynamx.api.contentpack.object;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.contentpack.type.ObjectInfo;

import java.util.Collection;
import java.util.List;

/**
 * Describes an {@link ObjectInfo} which can be used in a {@link fr.dynamx.common.entities.PackPhysicsEntity}
 */
public interface IPhysicsPackInfo
{
    /**
     * @return The center of mass of the object
     */
    Vector3f getCenterOfMass();

    /**
     * @return All collision shapes of the object
     */
    Collection<? extends IShapeInfo> getShapes();

    List<Vector3f> getCollisionShapeDebugBuffer();

    <T extends InteractivePart<?, ModularVehicleInfoBuilder>> List<T> getInteractiveParts();
}
