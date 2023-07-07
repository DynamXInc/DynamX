package fr.dynamx.api.physics;

import com.jme3.bullet.CollisionSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.math.Vector3f;
import fr.dynamx.utils.physics.ShapeUtils;
import lombok.Getter;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * All rigid bodies used with DynamX must have an user object that is a BulletShapeType <br>
 * A BulletShapeType gives information about a bullet collision object, used for example to handle collisions with players
 *
 * @param <T> The type of the object using the rigid body
 * @see EnumBulletShapeType
 */
public class BulletShapeType<T> {
    /**
     * The type of the collision object
     */
    @Getter
    private final EnumBulletShapeType type;

    /**
     * The object generating the rigid body
     */
    @Getter
    private final T objectIn;

    @Getter
    private List<Vector3f> debugTriangles;

    /**
     * @param type     The type of the object, important
     * @param objectIn The object generating the rigid body
     */
    public BulletShapeType(EnumBulletShapeType type, T objectIn, CollisionShape space) {
        this.type = type;
        this.objectIn = objectIn;
        //Check client side
        if (FMLCommonHandler.instance().getSide().isClient()) {
            generateDebugTriangles(space);
        }
    }

    public void generateDebugTriangles(CollisionShape shape) {
        FloatBuffer[] debugBuffer;
        CompoundCollisionShape compoundCollisionShape = null;
        if(shape instanceof CompoundCollisionShape){
            compoundCollisionShape = (CompoundCollisionShape) shape;
            debugBuffer = ShapeUtils.getDebugBuffer(compoundCollisionShape);
        }else{
            debugBuffer = new FloatBuffer[]{ShapeUtils.getDebugBuffer(shape)};
        }
        debugTriangles = ShapeUtils.getDebugVectorList(compoundCollisionShape, debugBuffer);
    }


    @Override
    public String toString() {
        return "BulletShapeType{" +
                type +
                ", objectIn=" + objectIn +
                '}';
    }
}