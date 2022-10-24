package fr.dynamx.api.contentpack.object;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.common.contentpack.parts.PartShape;

/**
 * Describes a IShapedObject <br>
 * The function generateShape() is automatically called by the associated {@link fr.dynamx.common.contentpack.loader.InfoLoader}
 */
public interface IShapeContainer extends INamedObject {
    /**
     * @return The object's scale modifier
     */
    Vector3f getScaleModifier();

    /**
     * Adds a {@link PartShape} to this object
     */
    void addCollisionShape(PartShape<?> tPartShape);
}
