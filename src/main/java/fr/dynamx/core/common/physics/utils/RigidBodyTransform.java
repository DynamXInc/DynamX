package fr.dynamx.core.common.physics.utils;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * Container for position and rotation of a physics object
 */
public class RigidBodyTransform {
    private final Vector3f position = new Vector3f();
    private final Quaternion rotation = new Quaternion();

    public RigidBodyTransform() {
    }

    public RigidBodyTransform(RigidBodyTransform transform) {
        set(transform);
    }

    public RigidBodyTransform(PhysicsRigidBody of) {
        set(of);
    }

    public void set(PhysicsRigidBody from) {
        from.getPhysicsLocation(position);
        from.getPhysicsRotation(rotation);
    }

    public void set(RigidBodyTransform transform) {
        position.set(transform.getPosition());
        rotation.set(transform.getRotation());
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public void setRotation(Quaternion rotation) {
        this.rotation.set(rotation);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    @Override
    public String toString() {
        return "RBTransform{" +
                "position=" + position +
                ", rotation=" + rotation +
                '}';
    }
}
