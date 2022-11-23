package fr.dynamx.api.network.sync.v3;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.network.packets.MessageForcePlayerPos;
import fr.dynamx.common.network.sync.v3.DynamXSynchronizedVariables;
import fr.dynamx.common.network.sync.vars.AttachedBodySynchronizedVariable;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.debug.SyncTracker;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static fr.dynamx.common.network.sync.vars.PosSynchronizedVariable.*;

public class TransformsSynchronizedVariable extends ListeningSynchronizedEntityVariable<Map<Byte, RigidBodyTransform>>
{
    public TransformsSynchronizedVariable(PhysicsEntity<?> entity, AttachedBodySynchronizedVariable.AttachedBodySynchronizer synchronizer) {
        super(((entityPositionDataSynchronizedEntityVariable, transforms) -> {
//TODO INTPERPOLATION ETC :c
            if(entity.getSynchronizer().getSimulationHolder().isSinglePlayer()) {
                if (!entity.world.isRemote) //Solo mode
                {
                    Map<Byte, SynchronizedRigidBodyTransform> tar = synchronizer.getTransforms();
                    transforms.forEach((b, t) -> {
                        if (!tar.containsKey(b))
                            tar.put(b, new SynchronizedRigidBodyTransform(t));
                        else
                            tar.get(b).getPhysicTransform().set(t);
                    });
                    if (tar.size() != transforms.size()) {
                        tar.keySet().removeIf(b -> !transforms.containsKey(b));
                    }
                } else
                    DynamXMain.log.error("Incorrect simulation holder in client set pos value : " + entity.getSynchronizer().getSimulationHolder());
            } else //Physics "receiver" side
            {
                for (Map.Entry<Byte, RigidBodyTransform> transform : transforms.entrySet()) {
                    synchronizer.setPhysicsTransform(transform.getKey(), transform.getValue());
                }
            }
        }), SynchronizationRules.PHYSICS_TO_SPECTATORS, DynamXSynchronizedVariables.transformsSerializer, new Callable<Map<Byte, RigidBodyTransform>>() {
            private final Map<Byte, RigidBodyTransform> transforms = new HashMap<>();

            @Override
            public Map<Byte, RigidBodyTransform> call() {
                boolean changed = !transforms.isEmpty() && entity.ticksExisted % 20 == 0; //Keep low-rate sync while not moving, if we have joints to sync
                if (!changed) {
                    for (Map.Entry<Byte, SynchronizedRigidBodyTransform> transform : synchronizer.getTransforms().entrySet()) {
                        if (!transforms.containsKey(transform.getKey())) {
                            changed = true;
                            transforms.put(transform.getKey(), new RigidBodyTransform(transform.getValue().getPhysicTransform()));
                        } else {
                            //TODO CLEAN
                            RigidBodyTransform pos = transform.getValue().getPhysicTransform();
                            RigidBodyTransform ctransform = transforms.get(transform.getKey());
                            if (SyncTracker.different(pos.getPosition().x, ctransform.getPosition().x) || SyncTracker.different(pos.getPosition().y, ctransform.getPosition().y) || SyncTracker.different(pos.getPosition().z, ctransform.getPosition().z)) {
                                changed = true;
                            } else if (SyncTracker.different(pos.getRotation().getX(), ctransform.getRotation().getX()) || SyncTracker.different(pos.getRotation().getY(), ctransform.getRotation().getY()) ||
                                    SyncTracker.different(pos.getRotation().getZ(), ctransform.getRotation().getZ()) || SyncTracker.different(pos.getRotation().getW(), ctransform.getRotation().getW())) {
                                changed = true;
                            }
                        }
                    }
                    if (synchronizer.getTransforms().size() != transforms.size()) {
                        changed = true;
                        transforms.keySet().removeIf(tr -> !synchronizer.getTransforms().containsKey(tr));
                    }
                }
                if (changed) {
                    for (Map.Entry<Byte, SynchronizedRigidBodyTransform> transform : synchronizer.getTransforms().entrySet()) {
                        if (transforms.get(transform.getKey()) != null) {
                            transforms.get(transform.getKey()).set(transform.getValue().getPhysicTransform());
                        }
                    }
                    //TODO FIX entity.synchronizedPosition.setChanged(true);
                }
                return transforms;
            }
        }, "pos");
        this.set(new HashMap<>());
    }
}
