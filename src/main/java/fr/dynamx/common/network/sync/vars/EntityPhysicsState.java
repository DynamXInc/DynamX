package fr.dynamx.common.network.sync.vars;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.SyncTracker;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;

/**
 * Holds previous states of an entity, to smoothly fix it's position when we receive a sync packet, corresponding to a previous date <br>
 * Works with {@link PosSynchronizedVariable}
 */
@Deprecated
public class EntityPhysicsState {
    protected final PhysicsEntity<?> entityIn;
    public final Vector3f pos = new Vector3f();
    public Quaternion rotation = new Quaternion();
    public int simulationTime;

    public EntityPhysicsState(PhysicsEntity<?> entityIn) {
        this.entityIn = entityIn;
        //pos.set(entityIn.physicsHandler.getPosition());
        //rotation.set(entityIn.physicsHandler.getRotation());
        simulationTime = ClientPhysicsSyncManager.simulationTime;
    }

    public void addToOlders(Vector3f offsetn, Quaternion offsetQuat, float step) {
        Vector3f finalOffsetn = Vector3fPool.get(offsetn);
        /*PhysicsEntityNetHandler<? extends PhysicsEntity<?>> h = entityIn.getNetwork();
        h.getOldStates().forEach((i, s) -> {
            if (i < ClientPhysicsSyncManager.simulationTime) {
                s.pos.addLocal(finalOffsetn);
                DynamXMath.slerp(step, s.rotation, offsetQuat, s.rotation);
            }
        });*/
    }

    public void interpolateDeltas(Vector3f with, Quaternion quaternion, boolean bodyActive, int step, int pass) {
        Vector3f nPos = entityIn.physicsHandler.getCollisionObject().getPhysicsLocation(Vector3fPool.get());
        Vector3f oldPos = Vector3fPool.get(pos);
        Vector3f sub = Vector3fPool.get((float) DynamXMath.interpolateDoubleDelta(with.x, oldPos.x, step), (float) DynamXMath.interpolateDoubleDelta(with.y, oldPos.y, step), (float) DynamXMath.interpolateDoubleDelta(with.z, oldPos.z, step));
        if (pass != 0) {
            sub.y = 0;
        } else {
            sub.y /= 2;
        }
        if (bodyActive || !(Math.abs(sub.x) < SyncTracker.EPS) || !(Math.abs(sub.y) < SyncTracker.EPS) || !(Math.abs(sub.z) < SyncTracker.EPS)) {
            nPos.addLocal(sub.x, sub.y, sub.z);
            entityIn.physicsHandler.setPhysicsPosition(nPos);

            Quaternion nQ = DynamXMath.slerp(1f / step, rotation, quaternion);
            addToOlders(sub, rotation, 1f / step);
            entityIn.physicsHandler.setPhysicsRotation(nQ);
        }
    }
}
