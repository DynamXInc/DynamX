package fr.dynamx.api.contentpack.object;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.entities.PackPhysicsEntity;
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

    void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules);

    default List<IDrawablePart<?, ?>> getDrawableParts() {
        return Collections.emptyList();
    }

    ItemStack getPickedResult(int metadata);

    float getLinearDamping();
    float getAngularDamping();

    default float getInWaterLinearDamping() {
        return 0.6f;
    }
    default float getInWaterAngularDamping() {
        return 0.6f;
    }

    float getRenderDistance();

    SceneGraph<?,?> getSceneGraph();
}
