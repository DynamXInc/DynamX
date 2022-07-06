package fr.dynamx.api.physics;

/**
 * All rigid bodies used with DynamX must have an user object that is a BulletShapeType <br>
 *     A BulletShapeType gives information about a bullet collision object, used for example to handle collisions with players
 *
 * @param <T> The type of the object using the rigid body
 * @see EnumBulletShapeType
 */
public class BulletShapeType<T>
{
    private final EnumBulletShapeType type;
    private final T objectIn;

    /**
     * @param type The type of the object, important
     * @param objectIn The object generating the rigid body
     */
    public BulletShapeType(EnumBulletShapeType type, T objectIn)
    {
        this.type = type;
        this.objectIn = objectIn;
    }

    /**
     * @return The type of the collision object
     */
    public EnumBulletShapeType getType() {
        return type;
    }

    /**
     * @return The object generating the rigid body
     */
    public T getObjectIn() {
        return objectIn;
    }

    @Override
    public String toString() {
        return "BulletShapeType{" +
                type +
                ", objectIn=" + objectIn +
                '}';
    }
}