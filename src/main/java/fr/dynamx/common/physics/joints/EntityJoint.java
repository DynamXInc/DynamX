package fr.dynamx.common.physics.joints;

import com.jme3.bullet.joints.Constraint;
import fr.aym.acslib.utils.nbtserializer.ISerializable;
import fr.aym.acslib.utils.nbtserializer.NBTSerializer;
import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a joint (a {@link Constraint} in the physics engine) between two entities
 *
 * @param <T> The Constraint type
 * @see EntityJointsHandler
 */
public class EntityJoint<T extends Constraint> {
    private final JointHandler<?, ?, ?> handler;
    private final PhysicsEntity<?> entity1, entity2;
    private final ResourceLocation type;
    private final byte jointId;
    private final T joint;

    public EntityJoint(JointHandler<?, ?, ?> handler, PhysicsEntity<?> entity1, PhysicsEntity<?> entity2, byte jointId, ResourceLocation type, T joint) {
        this.handler = handler;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.jointId = jointId;
        this.type = type;
        this.joint = joint;
    }

    public JointHandler<?, ?, ?> getHandler() {
        return handler;
    }

    public PhysicsEntity<?> getOtherEntity(PhysicsEntity<?> from) {
        return entity1 != from ? entity1 : entity2;
    }

    public PhysicsEntity<?> getEntity1() {
        return entity1;
    }

    public PhysicsEntity<?> getEntity2() {
        return entity2;
    }

    /**
     * Can be null to simulate joints on server side in solo (and force sync to the client using physics)
     *
     * @return The physics joint
     */
    @Nullable
    public T getJoint() {
        return joint;
    }

    public ResourceLocation getType() {
        return type;
    }

    public byte getJointId() {
        return jointId;
    }

    @Override
    public String toString() {
        return "EntityJoint{" +
                "jointId=" + jointId +
                ", entity1=" + entity1 +
                ", entity2=" + entity2 +
                ", type=" + type +
                ", joint=" + joint +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityJoint<?> that = (EntityJoint<?>) o;
        return (that.entity1 == entity1 && that.entity2 == entity2 && that.jointId == jointId && that.type.equals(type));
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity1, entity2, type, jointId);
    }

    /**
     * A serialized {@link EntityJoint}, ready to be saved
     */
    public static class CachedJoint implements ISerializable {
        private UUID id;
        private byte jid;
        private ResourceLocation type;
        private boolean jointOwner;

        public CachedJoint() {
        }

        public CachedJoint(UUID id, byte jid, ResourceLocation type, boolean jointOwner) {
            this.id = id;
            this.jid = jid;
            this.type = type;
            this.jointOwner = jointOwner;
        }

        public UUID getId() {
            return id;
        }

        public byte getJid() {
            return jid;
        }

        public ResourceLocation getType() {
            return type;
        }

        public boolean isJointOwner() {
            return jointOwner;
        }

        @Override
        public String toString() {
            return "CachedJoint{" +
                    "id=" + id +
                    ", jid=" + jid +
                    ", type=" + type +
                    ", jointOwner=" + jointOwner +
                    '}';
        }

        @Override
        public int getVersion() {
            return 4;
        }

        @Override
        public Object[] getObjectsToSave() {
            return new Object[]{id, type.toString(), jid, jointOwner};
        }

        @Override
        public void populateWithSavedObjects(Object[] objects) {
            id = (UUID) objects[0];
            type = new ResourceLocation((String) objects[1]);
            jid = (byte) objects[2];
            jointOwner = NBTSerializer.convert((Byte) objects[3]);
        }
    }
}
