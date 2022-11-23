package fr.dynamx.api.network.sync.v3;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

//TODO IMPROVE THIS
public class SynchronizedEntityVariableFactory {
    public static final SynchronizedVariableSerializer<Byte> byteSerializer = new SynchronizedVariableSerializer<Byte>() {
        @Override
        public void writeObject(ByteBuf buffer, Byte object) {
            buffer.writeByte(object);
        }

        @Override
        public Byte readObject(ByteBuf buffer) {
            return buffer.readByte();
        }

        @Override
        public String toString() {
            return "byte";
        }
    };
    public static final SynchronizedVariableSerializer<Integer> intSerializer = new SynchronizedVariableSerializer<Integer>() {
        @Override
        public void writeObject(ByteBuf buffer, Integer object) {
            buffer.writeInt(object);
        }

        @Override
        public Integer readObject(ByteBuf buffer) {
            return buffer.readInt();
        }

        @Override
        public String toString() {
            return "int";
        }
    };
    public static final SynchronizedVariableSerializer<Float> floatSerializer = new SynchronizedVariableSerializer<Float>() {
        @Override
        public void writeObject(ByteBuf buffer, Float object) {
            buffer.writeFloat(object);
        }

        @Override
        public Float readObject(ByteBuf buffer) {
            return buffer.readFloat();
        }

        @Override
        public String toString() {
            return "float";
        }
    };
    public static final SynchronizedVariableSerializer<Boolean> booleanSerializer = new SynchronizedVariableSerializer<Boolean>() {
        @Override
        public void writeObject(ByteBuf buffer, Boolean object) {
            buffer.writeBoolean(object);
        }

        @Override
        public Boolean readObject(ByteBuf buffer) {
            return buffer.readBoolean();
        }

        @Override
        public String toString() {
            return "boolean";
        }
    };
    public static final SynchronizedVariableSerializer<float[]> floatArraySerializer = new SynchronizedVariableSerializer<float[]>() {
        @Override
        public void writeObject(ByteBuf buffer, float[] object) {
            buffer.writeInt(object.length);
            for (float f : object)
                buffer.writeFloat(f);
        }

        @Override
        public float[] readObject(ByteBuf buffer) {
            int size = buffer.readInt();
            float[] currentValue = new float[size];
            for (int i = 0; i < size; i++)
                currentValue[i] = buffer.readFloat();
            return currentValue;
        }

        @Override
        public String toString() {
            return "float[]";
        }
    };
    public static final SynchronizedVariableSerializer<Vector3f> vector3fSerializer = new SynchronizedVariableSerializer<Vector3f>() {
        @Override
        public void writeObject(ByteBuf buffer, Vector3f object) {
            buffer.writeFloat(object.x);
            buffer.writeFloat(object.y);
            buffer.writeFloat(object.z);
        }

        @Override
        public Vector3f readObject(ByteBuf buffer) {
            return new Vector3f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }

        @Override
        public String toString() {
            return "vector3f";
        }
    };
    public static final SynchronizedVariableSerializer<Quaternion> quaternionSerializer = new SynchronizedVariableSerializer<Quaternion>() {
        @Override
        public void writeObject(ByteBuf buffer, Quaternion object) {
            buffer.writeFloat(object.getX());
            buffer.writeFloat(object.getY());
            buffer.writeFloat(object.getZ());
            buffer.writeFloat(object.getW());
        }

        @Override
        public Quaternion readObject(ByteBuf buffer) {
            return new Quaternion(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }

        @Override
        public String toString() {
            return "quaternion";
        }
    };

    public static final SynchronizedVariableSerializer<EntityPlayer> playerSerializer = new SynchronizedVariableSerializer<EntityPlayer>() {
        @Override
        public void writeObject(ByteBuf buffer, EntityPlayer object) {
            buffer.writeInt(object == null ? -1 : object.getEntityId());
        }

        @Override
        public EntityPlayer readObject(ByteBuf buffer) {
            int id = buffer.readInt();
            if(id == -1)
                return null;
            Entity e = Minecraft.getMinecraft().world.getEntityByID(id);
            return e instanceof EntityPlayer ? (EntityPlayer) e : null;
        }
    };

    public static final SynchronizedVariableSerializer<PhysicsEntity<?>> physicsEntitySerializer = new SynchronizedVariableSerializer<PhysicsEntity<?>>() {
        @Override
        public void writeObject(ByteBuf buffer, PhysicsEntity<?> object) {
            buffer.writeInt(object == null ? -1 : object.getEntityId());
        }

        @Override
        public PhysicsEntity<?> readObject(ByteBuf buffer) {
            int id = buffer.readInt();
            if(id == -1)
                return null;
            Entity e = Minecraft.getMinecraft().world.getEntityByID(id);
            return e instanceof PhysicsEntity ? (PhysicsEntity<?>) e : null;
        }
    };
    public static final SynchronizedVariableSerializer<String> stringSerializer = new SynchronizedVariableSerializer<String>() {
        @Override
        public void writeObject(ByteBuf buffer, String object) {
            ByteBufUtils.writeUTF8String(buffer, object);
        }

        @Override
        public String readObject(ByteBuf buffer) {
            return ByteBufUtils.readUTF8String(buffer);
        }

        @Override
        public String toString() {
            return "string";
        }
    };

    public static <T> SynchronizedVariableSerializer<T> getSerializer(Class<T> dataType) {
        if(dataType == Integer.class)
            return (SynchronizedVariableSerializer<T>) intSerializer;
        if(dataType == Float.class)
            return (SynchronizedVariableSerializer<T>) floatSerializer;
        if(dataType == float[].class)
            return (SynchronizedVariableSerializer<T>) floatArraySerializer;
        if(dataType == Boolean.class)
            return (SynchronizedVariableSerializer<T>) booleanSerializer;
        if(dataType == Vector3f.class)
            return (SynchronizedVariableSerializer<T>) vector3fSerializer;
        if(dataType == Quaternion.class)
            return (SynchronizedVariableSerializer<T>) quaternionSerializer;
        throw new IllegalArgumentException("Not serializable " + dataType);
    }
}
