package fr.dynamx.api.contentpack.object;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * Describes an {@link ObjectInfo} which can be used in a {@link fr.dynamx.common.entities.PackPhysicsEntity}
 */
public interface IPhysicsPackInfo extends ICollisionsContainer {
    /**
     * @return The center of mass of the object
     */
    Vector3f getCenterOfMass();

    default <T extends InteractivePart<?, ?>> List<T> getInteractiveParts() {
        return Collections.emptyList();
    }

    default List<IDrawablePart<?>> getDrawableParts() {
        return Collections.emptyList();
    }

    ItemStack getPickedResult(int metadata);
}
