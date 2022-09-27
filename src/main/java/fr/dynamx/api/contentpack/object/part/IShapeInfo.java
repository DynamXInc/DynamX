package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;

/**
 * A simple cuboid collision shape
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
}
