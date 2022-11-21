package fr.dynamx.common.physics.joints;

import com.jme3.bullet.joints.Constraint;
import fr.aym.acslib.utils.nbtserializer.NBTSerializer;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IEntityJoints;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageJoints;
import fr.dynamx.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base implementation of {@link IEntityJoints}
 *
 * @see AttachModule
 * @see JointHandler
 */
public class EntityJointsHandler implements IPhysicsModule<AbstractEntityPhysicsHandler<?, ?>>, IEntityJoints, IPhysicsModule.IEntityUpdateListener {
    private final PhysicsEntity<?> entity;
    private final List<EntityJoint<?>> joints = new ArrayList<>();
    private List<EntityJoint.CachedJoint> queuedRestorations;
    private int restoreCooldown = -1;
    private boolean dirty;
    private boolean restoringJoints;

    public EntityJointsHandler(PhysicsEntity<?> entity) {
        this.entity = entity;
    }

    /**
     * @return All the active joints of this entity
     */
    public List<EntityJoint<?>> getJoints() {
        return joints;
    }

    private void addJointInternal(PhysicsEntity<?> target, EntityJoint<?> joint) {
        if (joints.contains(joint)) //prevent duplicates
            throw new IllegalStateException("There is already a joint " + joint + " between " + entity + " and " + target + " !");
        DynamXMain.proxy.scheduleTask(entity.world, () -> joints.add(joint));
        setDirty(true);
        if (!restoringJoints && queuedRestorations != null) {
            EntityJoint.CachedJoint rm = null;
            for (EntityJoint.CachedJoint k : queuedRestorations) {
                if (k.getId().equals(target.getPersistentID())) {
                    rm = k;
                    break;
                }
            }
            if (rm != null) {
                queuedRestorations.remove(rm);
            }
        }
    }

    @Override //done in physics thread
    public <C extends Constraint> void addJoint(JointHandler<?, ?, ?> type, C joint, byte id, PhysicsEntity<?> otherEntity) {
        if (otherEntity.getJointsHandler() == null)
            throw new IllegalArgumentException("Entity " + otherEntity + " does not accept joints !");
        EntityJoint<C> j = new EntityJoint<>(type, entity, otherEntity, id, type.getType(), joint);
        //if(joints.contains(j) || otherEntity.getJointsHandler().getJoints().contains(j))
        //  return -1;
        addJointInternal(otherEntity, j);
        if (otherEntity != entity)
            ((EntityJointsHandler) otherEntity.getJointsHandler()).addJointInternal(entity, j);

        if (otherEntity != entity) {
            if (type.isJointOwner(j, entity))
                otherEntity.getSynchronizer().setSimulationHolder(entity.getSynchronizer().getSimulationHolder(), SimulationHolder.UpdateContext.ATTACHED_ENTITIES);
            else
                entity.getSynchronizer().setSimulationHolder(otherEntity.getSynchronizer().getSimulationHolder(), SimulationHolder.UpdateContext.ATTACHED_ENTITIES);
        }

        if (j.getJoint() != null) {
            DynamXContext.getPhysicsWorld().addJoint(j.getJoint());
            entity.physicsHandler.activate();
            if (otherEntity != entity)
                otherEntity.physicsHandler.activate();
        }
    }

    /**
     * Internal function to re-create a joint from a packet
     */
    public void onNewJointSynchronized(EntityJoint.CachedJoint toAdd) {
        if (queuedRestorations == null) {
            queuedRestorations = new ArrayList<>();
        }
        queuedRestorations.add(toAdd);
        restoreCooldown = 1;
    }

    @Override
    public void removeJointWithMe(ResourceLocation jointType, byte jointId) {
        removeJointWith(entity, jointType, jointId);
    }

    @Override
    public void removeJointWith(PhysicsEntity<?> otherEntity, ResourceLocation jointType, byte jointId) {
        DynamXMain.proxy.scheduleTask(entity.world, () -> {
            EntityJoint<?> temp = null;
            EntityJoint<?> toRemove = new EntityJoint<>(null, entity, otherEntity, jointId, jointType, null);
            for (EntityJoint<?> j : joints) {
                if (toRemove.equals(j)) {
                    temp = j;
                    break;
                }
            }
            if (temp != null) {
                onRemoveJointInternal(temp, otherEntity);
                joints.remove(temp);
            } else {
                DynamXMain.log.warn("[JointsHandler] Cannot remove joint between " + entity + " and " + otherEntity + " : joint not found !");
            }
        });
    }

    @Override
    public void removeJointsOfType(ResourceLocation jointType, byte jointId) {
        DynamXMain.proxy.scheduleTask(entity.world, () -> {
            EntityJoint<?> temp = null;
            for (EntityJoint<?> j : joints) {
                if (j.getJointId() == jointId && (j.getEntity1() == entity || j.getEntity2() == entity) && jointType.equals(j.getType())) {
                    temp = j;
                    break;
                }
            }
            if (temp != null) {
                onRemoveJointInternal(temp, temp.getOtherEntity(entity));
                joints.remove(temp);
            } else {
                DynamXMain.log.warn("[JointsHandler] Cannot remove joints of type " + jointType + " / " + jointId + " of " + entity + " : joints not found !");
            }
        });
    }

    /**
     * Internal function remove a joint on entity death or from a packet
     */
    @Override
    public void onRemoveJoint(EntityJoint<?> joint) {
        onRemoveJointInternal(joint, joint.getOtherEntity(entity));
    }

    /**
     * Internal function remove a joint
     */
    private void onRemoveJointInternal(EntityJoint<?> joint, PhysicsEntity<?> otherEntity) {
        JointHandler<?, ?, ?> handler = joint.getHandler();
        if (entity != otherEntity) {
            if (handler.isJointOwner(joint, entity)) {
                otherEntity.getSynchronizer().setSimulationHolder(otherEntity.getSynchronizer().getDefaultSimulationHolder(), SimulationHolder.UpdateContext.ATTACHED_ENTITIES);
            } else {
                entity.getSynchronizer().setSimulationHolder(otherEntity.getSynchronizer().getDefaultSimulationHolder(), SimulationHolder.UpdateContext.ATTACHED_ENTITIES);
            }
        }
        handler.onDestroy(joint, entity);
        setDirty(true);
        if (entity != otherEntity && otherEntity.getJointsHandler() != null) {
            handler.onDestroy(joint, otherEntity);
            //common use
            ((EntityJointsHandler) otherEntity.getJointsHandler()).getJoints().remove(joint);
            ((EntityJointsHandler) otherEntity.getJointsHandler()).setDirty(true);
        }
        if (joint.getJoint() != null) {
            DynamXContext.getPhysicsWorld().removeJoint(joint.getJoint());
            entity.physicsHandler.activate();
            if (otherEntity != entity) {
                otherEntity.physicsHandler.activate();
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList jointst = new NBTTagList();
        if (!joints.isEmpty()) {
            joints.forEach(j -> jointst.appendTag(NBTSerializer.serialize(new EntityJoint.CachedJoint(j.getOtherEntity(entity).getPersistentID(), j.getJointId(), j.getType(), JointHandlerRegistry.getHandler(j.getType()).isJointOwner(j, entity)))));
        }
        tag.setTag("joints", jointst);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        joints.clear();
        NBTTagList jointst = tag.getTagList("joints", Constants.NBT.TAG_COMPOUND);
        if (!jointst.isEmpty()) {
            queuedRestorations = new ArrayList<>();
            for (int i = 0; i < jointst.tagCount(); i++) {
                EntityJoint.CachedJoint j = new EntityJoint.CachedJoint();
                NBTSerializer.unserialize(jointst.getCompoundTagAt(i), j);
                queuedRestorations.add(j);
            }
            restoreCooldown = 20;
        }
    }

    @Override
    public void updateEntity() {
        if (DynamXMain.proxy.shouldUseBulletSimulation(entity.world)) {
            if (restoreCooldown > 0) {
                restoreCooldown--;
                if (restoreCooldown == 0) {
                    restoringJoints = true;
                    i:
                    for (EntityJoint.CachedJoint j : queuedRestorations) {
                        boolean found = false;
                        for (Entity e : entity.world.loadedEntityList) {
                            if (e.getPersistentID().equals(j.getId())) {
                                found = true;
                                if (e instanceof PhysicsEntity<?>) {
                                    JointHandler<?, ?, ?> jointHandler = JointHandlerRegistry.getHandlerUnsafe(j.getType());
                                    if (jointHandler != null) {
                                        for (EntityJoint<?> joint : joints) {
                                            if (joint.getJointId() == j.getJid() && j.getType().equals(joint.getType()) && (joint.getEntity1() == e || joint.getEntity2() == e)) {
                                                DynamXMain.log.warn("TRYING TO ADD DUPLICATED JOINT " + j + " " + entity + " " + joints + " " + e + ". Sync cancelled");
                                                continue i;
                                            }
                                        }
                                        jointHandler.createJoint(entity, (PhysicsEntity<?>) e, j.getJid());
                                    } else {
                                        DynamXMain.log.warn("[Joints NBT Load] Failed to re attach " + entity + " to " + e + " : joint handler " + j.getType() + " not found !");
                                    }
                                } else
                                    DynamXMain.log.warn("[Joints NBT Load] Failed to re attach " + entity + " to entity with uuid " + j.getId() + " : wrong entity type : " + e);
                                break;
                            }
                        }
                        if (!found)
                            DynamXMain.log.warn("[Joints NBT Load] Failed to re attach " + entity + " to entity with uuid " + j.getId() + " : not found");
                    }
                    restoringJoints = false;
                    queuedRestorations.clear();
                    queuedRestorations = null;
                    restoreCooldown = -1;
                }
            }
            joints.removeIf(j -> { //in common thread
                //Broken joint (by the physics engine)
                if (j.getJoint() != null && !j.getJoint().isEnabled()) {
                    //If we are in solo and the joint just broke, also, remove it on the server
                    syncRemovedJoint(j);
                    //And remove the joint here too
                    onRemoveJoint(j);
                    return true;
                }
                return false;
            });
        }

        if (isDirty()) {
            if (!entity.world.isRemote && entity.getSynchronizer().doesOtherSideUsesPhysics()) {
                DynamXContext.getNetwork().sendToClient(new MessageJoints(entity, computeCachedJoints()), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
            }
            setDirty(false);
        }
    }

    /**
     * Internal function for the sync of the joints
     */
    public List<EntityJoint.CachedJoint> computeCachedJoints() {
        List<EntityJoint.CachedJoint> sendList = new ArrayList<>();
        for (EntityJoint<?> g : joints) {
            sendList.add(new EntityJoint.CachedJoint(g.getOtherEntity(entity).getPersistentID(), g.getJointId(), g.getType(), g.getHandler().isJointOwner(g, entity)));
        }
        return sendList;
    }

    protected void syncRemovedJoint(EntityJoint<?> joint) {
        if (entity.world.isRemote && entity.getSynchronizer().getSimulationHolder().isSinglePlayer()) {
            Entity e = ((SPPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).getOtherSideEntity();
            if (e instanceof PhysicsEntity) {
                Optional<EntityJoint<?>> other = ((PhysicsEntity<?>) e).getJointsHandler().getJoints().stream().filter(j2 -> j2.getType().equals(joint.getType()) && j2.getJointId() == joint.getJointId()).findFirst();
                if (other.isPresent()) {
                    ((PhysicsEntity<?>) e).getJointsHandler().getJoints().remove(other.get());
                    ((PhysicsEntity<?>) e).getJointsHandler().onRemoveJoint(other.get());
                }
            }
        }
    }

    @Override
    public void onRemovedFromWorld() {
        joints.forEach(this::onRemoveJoint);
        joints.clear();
    }

    @Override
    public void setSimulationHolderOnJointedEntities(SimulationHolder holder) {
        for (EntityJoint<?> j : joints) {
            if (j.getHandler().isJointOwner(j, entity)) {
                //NB : le false empêche de faire des remorques attachées à d'autres remorques
                j.getOtherEntity(entity).getSynchronizer().setSimulationHolder(holder, SimulationHolder.UpdateContext.ATTACHED_ENTITIES);
            }
        }
    }

    @Override
    public void sync(EntityPlayerMP target) {
        if (!getJoints().isEmpty())
            DynamXContext.getNetwork().sendToClient(new MessageJoints(entity, computeCachedJoints()), EnumPacketTarget.PLAYER, target);
    }

    /**
     * @return The entity owning this joints
     */
    public PhysicsEntity<?> getEntity() {
        return entity;
    }

    protected boolean isDirty() {
        return dirty;
    }

    /**
     * Marks this module to synchronize it for clients
     */
    protected void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
