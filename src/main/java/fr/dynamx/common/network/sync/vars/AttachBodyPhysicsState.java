package fr.dynamx.common.network.sync.vars;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds previous states of an entity, to smoothly fix it's position when we receive a sync packet, corresponding to a previous date <br>
 *     Works with {@link PosSynchronizedVariable}
 */
public class AttachBodyPhysicsState extends EntityPhysicsState
{
    public final Map<Byte, RigidBodyTransform> transforms = new HashMap<>();

    public AttachBodyPhysicsState(PhysicsEntity<?> entityIn)
    {
        super(entityIn);
    }

    protected void addToOlders(byte part, Vector3f offsetn, Quaternion offsetQuat, float step)
    {
        Vector3f finalOffsetn = Vector3fPool.get(offsetn);
        PhysicsEntityNetHandler<? extends PhysicsEntity<?>> h = entityIn.getNetwork();
        h.getOldStates().forEach((i, s) -> {
            if(i < ClientPhysicsSyncManager.simulationTime)
            {
                ((AttachBodyPhysicsState)s).transforms.get(part).getPosition().addLocal(finalOffsetn);
                DynamXGeometry.slerp(((AttachBodyPhysicsState)s).transforms.get(part).getRotation(), offsetQuat, ((AttachBodyPhysicsState)s).transforms.get(part).getRotation(), step);
            }
        });
    }

    public void interpolateDeltas(byte part, RigidBodyTransform transform, int step, int pass)
    {
        /*Optional<EntityJoint<?>> joint = entityIn.getJointsHandler().getJoints().stream().filter(e -> e.getJointId() == part).findFirst();
        if(joint.isPresent())
        {
            joint.get().getJoint().getBodyB().setPhysicsLocation(transform.getValue().getPosition());
            joint.get().getJoint().getBodyB().setPhysicsRotation(transform.getValue().getRotation());

            Vector3f nPos = joint.get().getJoint().getBodyB().getPhysicsLocation(Vector3fPool.get());
            Vector3f oldPos = Vector3fPool.get(transforms.get(part).getPosition());
            Vector3f sub = Vector3fPool.get((float) DynamXUtils.interpolateDoubleDelta(transform.getPosition().x, oldPos.x, step), (float) DynamXUtils.interpolateDoubleDelta(transform.getPosition().y, oldPos.y, step), (float) DynamXUtils.interpolateDoubleDelta(transform.getPosition().z, oldPos.z, step));
        /*if(pass != 0)
        {
            sub.y = 0;
        }
        else
            sub.y /= 2;*//*

            nPos.addLocal(sub.x, sub.y, sub.z);
            entityIn.physicsHandler.setPhysicsPosition(nPos);

            Quaternion nQ = Trigonometry.slerp(transforms.get(part).getRotation(), transform.getRotation(), 1f/step);
            addToOlders(sub, transforms.get(part).getRotation(), 1f/step);
            entityIn.physicsHandler.setPhysicsRotation(nQ);
        }*/
        //else we can't do anything
        throw new IllegalStateException("Not implemented");
    }
}
