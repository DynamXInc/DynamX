package fr.dynamx.api.contentpack.object;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.common.contentpack.parts.PartShape;

/**
 * Describes a IShapedObject <br>
 * The function generateShape() is automatically called by the associed {@link fr.dynamx.common.contentpack.loader.InfoLoader}
 */
public interface IShapedObject extends INamedObject {
    /**
     * Generates the shape of the object <br>
     * If an exception is thrown then setShapeErrored() will be called
     */
    void generateShape();

    /**
     * Marks this object as errored
     */
    void setShapeErrored();

    /**
     * @return The object's scale modifier
     */
    Vector3f getScaleModifier();

    /**
     * Adds a {@link BasePart} to this object
     */
    void addPart(BasePart<?> tBasePart);

    /**
     * Adds a {@link PartShape} to this object
     */
    void addCollisionShape(PartShape<?> tPartShape);
}
