package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.utils.optimization.MutableBoundingBox;

/**
 * A simple cuboid collision shape
 *
 * todo outdated doc
 */
public interface IShapeInfo {
    /**
     * @return Center of the shape
     */
    Vector3f getPosition();

    /**
     * @return Half of the total size on each side
     */
    Vector3f getSize();

    MutableBoundingBox getBoundingBox();

    default PartShape.EnumPartType getShapeType() {
        return PartShape.EnumPartType.BOX;
    }
}
