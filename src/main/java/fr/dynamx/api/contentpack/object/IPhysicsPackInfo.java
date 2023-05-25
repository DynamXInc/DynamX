package fr.dynamx.api.contentpack.object;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Describes an {@link ObjectInfo} which can be used in a {@link fr.dynamx.common.entities.PackPhysicsEntity}
 */
public interface IPhysicsPackInfo {
    /**
     * @return The center of mass of the object
     */
    Vector3f getCenterOfMass();

    /**
     * @return All collision shapes of the object
     */
    Collection<? extends IShapeInfo> getShapes();

    List<Vector3f> getCollisionShapeDebugBuffer();

    default <T extends InteractivePart<?, ?>> List<T> getInteractiveParts() {
        return Collections.emptyList();
    }

    CompoundCollisionShape getPhysicsCollisionShape();

    default List<IDrawablePart<?>> getDrawableParts() {
        return Collections.emptyList();
    }

    ItemStack getPickedResult(int metadata);

    String getFullName();
}
