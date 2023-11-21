package fr.dynamx.api.contentpack.object;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;

/**
 * todo doc
 */
public interface ICollisionsContainer extends INamedObject {
    /**
     * @return The object's scale modifier
     */
    Vector3f getScaleModifier();

    ObjectCollisionsHelper getCollisionsHelper();
}
