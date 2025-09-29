package fr.dynamx.core.common.physics.joints;

import com.jme3.bullet.joints.Constraint;
import fr.aym.acslib.utils.DeserializedData;
import fr.aym.acslib.utils.nbtserializer.ISerializable;
import fr.aym.acslib.utils.nbtserializer.NBTSerializer;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.hermes.api.mc.HmResourceLocation;
import fr.hermes.api.mod.McObjectBinder;
import lombok.Getter;

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
    @Getter
    private final JointHandler<?, ?, ?> handler;
    @Getter
    private final PhysicsEntity<?> entity1, entity2;
    @Getter
    private final HmResourceLocation type;
    @Getter
    private final byte jointId;
    private final T joint;

    public EntityJoint(JointHandler<?, ?, ?> handler, PhysicsEntity<?> entity1, PhysicsEntity<?> entity2, byte jointId, HmResourceLocation type, T joint) {
        this.handler = handler;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.jointId = jointId;
        this.type = type;
        this.joint = joint;
    }

    public PhysicsEntity<?> getOtherEntity(PhysicsEntity<?> from) {
        return entity1 != from ? entity1 : entity2;
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
    @Getter
    public static class CachedJoint implements ISerializable {
        private UUID id;
        private byte jid;
        private HmResourceLocation type;
        private boolean jointOwner;

        public CachedJoint() {
        }

        public CachedJoint(UUID id, byte jid, HmResourceLocation type, boolean jointOwner) {
            this.id = id;
            this.jid = jid;
            this.type = type;
            this.jointOwner = jointOwner;
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
        public void populateWithSavedObjects(DeserializedData objects) {
            id = objects.next();
            type = McObjectBinder.instance.newResourceLocation(objects.next());
            jid = objects.next();
            jointOwner = NBTSerializer.convert(objects.next());
        }
    }
}
