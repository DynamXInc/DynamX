package fr.dynamx.common.physics.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.parts.PartFloat;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class FloatPhysicsHandler {

    @Setter
    @Getter
    protected float buoyCoefficient = 1f;
    @Setter
    @Getter
    protected float dragCoefficient = 0.05f;
    private final PhysicsEntity<?> handledEntity;
    @Getter
    @Setter
    private float size = 1;
    @Getter
    @Setter
    private Vector3f scale = new Vector3f(1, 1, 1);
    @Getter
    private final Vector3f debugDragForce = new Vector3f();
    @Getter
    private final Vector3f debugBuoyForce = new Vector3f();
    @Getter
    private final Vector3f position = new Vector3f();

    public FloatPhysicsHandler(PhysicsEntity<?> handledEntity){
        this.handledEntity = handledEntity;
    }
    public FloatPhysicsHandler(PhysicsEntity<?> handledEntity, PartFloat<?> partFloat, Vector3f position) {
        this.handledEntity = handledEntity;
        this.buoyCoefficient = partFloat.getBuoyCoefficient();
        this.dragCoefficient = partFloat.getDragCoefficient();
        this.size = partFloat.getSize();
        this.scale = partFloat.getScale();
        this.position.set(position);
    }

    public void handleBuoyancy(float waterLevel) {
        AbstractEntityPhysicsHandler<?, ?> physicsHandler = handledEntity.physicsHandler;
        if(physicsHandler == null){
            return;
        }
        Vector3f rotatedFloatPos = DynamXGeometry.rotateVectorByQuaternion(position, handledEntity.physicsRotation);
        Vector3f floatPosInWorldPos = handledEntity.physicsPosition.add(rotatedFloatPos);

        double dy = waterLevel - floatPosInWorldPos.y;

        if (!(dy > 0)) {
            return;
        }
        float area = size * size;
        dy = Math.min(dy, scale.y);

        Vector3f buoyForce = Vector3fPool.get(0, dy * area * DynamXPhysicsHelper.WATER_DENSITY * DynamXPhysicsHelper.GRAVITY * buoyCoefficient, 0);

        debugBuoyForce.set(buoyForce.mult(0.001f));
        physicsHandler.applyForce(buoyForce.multLocal(0.05f), rotatedFloatPos);

        Vector3f velocityAtPoint = DynamXPhysicsHelper.getVelocityAtPoint(physicsHandler.getLinearVelocity(), physicsHandler.getAngularVelocity(), rotatedFloatPos);
        float velocityLength = velocityAtPoint.length();
        Vector3f dragDir = velocityAtPoint.normalize();
        Vector3f dragForce = dragDir.multLocal(0.5f * DynamXPhysicsHelper.WATER_DENSITY * velocityLength * velocityLength * dragCoefficient * area);

        if (Vector3f.isValidVector(dragForce))
            physicsHandler.applyForce(dragForce.multLocal(0.05f), rotatedFloatPos);
        Vector3f nonRotatedDrag = DynamXGeometry.rotateVectorByQuaternion(dragForce, DynamXGeometry.inverseQuaternion(handledEntity.physicsRotation, QuaternionPool.get()));
        debugDragForce.set(nonRotatedDrag);
    }

}
