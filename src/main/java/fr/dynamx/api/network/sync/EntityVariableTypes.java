package fr.dynamx.api.network.sync;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.network.sync.variables.EntityPosVariable;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.HashMapPool;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public enum EntityVariableTypes {
    BYTE(Byte.class, new EntityVariableSerializer<Byte>() {
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
    }),
    INT(Integer.class, new EntityVariableSerializer<Integer>() {
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
    }),
    FLOAT(Float.class, new EntityVariableSerializer<Float>() {
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
    }),
    BOOLEAN(Boolean.class, new EntityVariableSerializer<Boolean>() {
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
    }),
    FLOAT_ARRAY(float[].class, new EntityVariableSerializer<float[]>() {
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
    }),
    VECTOR3F(Vector3f.class, new EntityVariableSerializer<Vector3f>() {
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
    }),
    QUATERNION(Quaternion.class, new EntityVariableSerializer<Quaternion>() {
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
    }),
    PLAYER(EntityPlayer.class, new EntityVariableSerializer<EntityPlayer>() {
        @Override
        public void writeObject(ByteBuf buffer, EntityPlayer object) {
            buffer.writeInt(object == null ? -1 : object.getEntityId());
        }

        @Override
        public EntityPlayer readObject(ByteBuf buffer) {
            int id = buffer.readInt();
            if (id == -1)
                return null;
            Entity e = Minecraft.getMinecraft().world.getEntityByID(id);
            return e instanceof EntityPlayer ? (EntityPlayer) e : null;
        }
    }),
    PHYSICS_ENTITY(PhysicsEntity.class, new EntityVariableSerializer<PhysicsEntity>() {
        @Override
        public void writeObject(ByteBuf buffer, PhysicsEntity object) {
            buffer.writeInt(object == null ? -1 : object.getEntityId());
        }

        @Override
        public PhysicsEntity<?> readObject(ByteBuf buffer) {
            int id = buffer.readInt();
            if (id == -1)
                return null;
            Entity e = Minecraft.getMinecraft().world.getEntityByID(id);
            return e instanceof PhysicsEntity ? (PhysicsEntity<?>) e : null;
        }
    }),
    STRING(String.class, new EntityVariableSerializer<String>() {
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
    }),
    POS(EntityPosVariable.EntityPositionData.class, new EntityVariableSerializer<EntityPosVariable.EntityPositionData>() {
        @Override
        public void writeObject(ByteBuf buf, EntityPosVariable.EntityPositionData object) {
            buf.writeBoolean(object.isBodyActive());
            DynamXUtils.writeVector3f(buf, object.getPosition());
            DynamXUtils.writeQuaternion(buf, object.getRotation());
            if (object.isBodyActive()) {
                DynamXUtils.writeVector3f(buf, object.getLinearVel());
                DynamXUtils.writeVector3f(buf, object.getRotationalVel());
            }
        }

        @Override
        public EntityPosVariable.EntityPositionData readObject(ByteBuf buffer) {
            //TODO PAS COOL NEW
            EntityPosVariable.EntityPositionData result = new EntityPosVariable.EntityPositionData(buffer.readBoolean(), DynamXUtils.readVector3f(buffer), DynamXUtils.readQuaternion(buffer));
            if (result.isBodyActive()) {
                result.getLinearVel().set(DynamXUtils.readVector3f(buffer));
                result.getRotationalVel().set(DynamXUtils.readVector3f(buffer));
            }
            return result;
        }
    }),
    WHEELS_STATES(WheelsModule.WheelState[].class, new EntityVariableSerializer<WheelsModule.WheelState[]>() {
        @Override
        public void writeObject(ByteBuf buf, WheelsModule.WheelState[] object) {
            buf.writeInt(object.length);
            for (WheelsModule.WheelState f : object) {
                buf.writeByte(f.ordinal());
            }
        }

        @Override
        public WheelsModule.WheelState[] readObject(ByteBuf buf) {
            WheelsModule.WheelState[] currentValue = new WheelsModule.WheelState[buf.readInt()];
            for (byte i = 0; i < currentValue.length; i++) {
                currentValue[i] = WheelsModule.WheelState.values()[buf.readByte()];
            }
            return currentValue;
        }
    }),
    WHEELS_INFOS(CustomType.mapType(Byte.class, PartWheelInfo.class),  new EntityVariableSerializer<Map<Byte, PartWheelInfo>>() {
        @Override
        public void writeObject(ByteBuf buf, Map<Byte, PartWheelInfo> object) {
            buf.writeInt(object.size());
            object.forEach((id, info) -> {
                buf.writeByte(id);
                ByteBufUtils.writeUTF8String(buf, info.getFullName());
            });
        }

        @Override
        public Map<Byte, PartWheelInfo> readObject(ByteBuf buf) {
            Map<Byte, PartWheelInfo> currentValue = HashMapPool.get();
            int size = buf.readInt();
            for (byte i = 0; i < size; i++)
                currentValue.put(buf.readByte(), DynamXObjectLoaders.WHEELS.findInfo(ByteBufUtils.readUTF8String(buf)));
            return currentValue;
        }
    }),
    DOOR_STATES(CustomType.mapType(Byte.class, DoorsModule.DoorState.class), new EntityVariableSerializer<Map<Byte, DoorsModule.DoorState>>() {
        @Override
        public void writeObject(ByteBuf buf, Map<Byte, DoorsModule.DoorState> doorsState) {
            buf.writeByte(doorsState.size());
            doorsState.forEach((i, s) -> {
                buf.writeByte(i);
                buf.writeInt(s.ordinal());
            });
        }

        @Override
        public Map<Byte, DoorsModule.DoorState> readObject(ByteBuf buf) {
            Map<Byte, DoorsModule.DoorState> currentValue = HashMapPool.get();
            int size = buf.readByte();
            for (int i = 0; i < size; i++) {
                currentValue.put(buf.readByte(), DoorsModule.DoorState.values()[buf.readInt()]);
            }
            return currentValue;
        }
    }),
    TRANSFORMS(CustomType.mapType(Byte.class, RigidBodyTransform.class), new EntityVariableSerializer<Map<Byte, RigidBodyTransform>>() {
        //TODO COMPRESSION : opnly keep chest for ragdolls
        @Override
        public void writeObject(ByteBuf buf, Map<Byte, RigidBodyTransform> object) {
            buf.writeInt(object.size());
            object.forEach((id, transform) -> {
                buf.writeByte(id);
                buf.writeFloat(transform.getPosition().x);
                buf.writeFloat(transform.getPosition().y);
                buf.writeFloat(transform.getPosition().z);

                buf.writeFloat(transform.getRotation().getX());
                buf.writeFloat(transform.getRotation().getY());
                buf.writeFloat(transform.getRotation().getZ());
                buf.writeFloat(transform.getRotation().getW());
            });
        }

        @Override
        public Map<Byte, RigidBodyTransform> readObject(ByteBuf buf) {
            Map<Byte, RigidBodyTransform> currentValue = HashMapPool.get();
            int size = buf.readInt();
            for (byte i = 0; i < size; i++) {
                byte tr = buf.readByte();
                RigidBodyTransform transform = new RigidBodyTransform();
                transform.getPosition().set(buf.readFloat(), buf.readFloat(), buf.readFloat());
                transform.getRotation().set(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
                currentValue.put(tr, transform);
            }
            return currentValue;
        }
    });


    @Getter
    private static final Map<Type, EntityVariableSerializer<?>> serializerRegistry = new HashMap<>();
    private final Type type;
    private final EntityVariableSerializer<?> serializer;

    <T> EntityVariableTypes(Class<T> type, EntityVariableSerializer<T> serializer) {
        this.type = type;
        this.serializer = serializer;
    }

    <A, B> EntityVariableTypes(CustomType type, EntityVariableSerializer<Map<A, B>> serializer) {
        this.type = type;
        this.serializer = serializer;
    }

    public static void registerSerializer(Type type, EntityVariableSerializer<?> serializer) {
        serializerRegistry.put(type, serializer);
    }

    static {
        for (EntityVariableTypes type : values()) {
            serializerRegistry.put(type.type, type.serializer);
        }
    }

    public static class CustomType implements ParameterizedType {
        private final Type[] actualTypeArguments;
        private final Class<?> rawType;

        public CustomType(Type[] actualTypeArguments, Class<?> rawType) {
            this.actualTypeArguments = actualTypeArguments;
            this.rawType = rawType;
        }

        public static CustomType mapType(Class<?> type1, Class<?> type2) {
            return new CustomType(new Type[]{type1, type2}, Map.class);
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public boolean equals(Object var1) {
            if (var1 instanceof ParameterizedType) {
                ParameterizedType var2 = (ParameterizedType)var1;
                if (this == var2) {
                    return true;
                } else {
                    Type var3 = var2.getOwnerType();
                    Type var4 = var2.getRawType();
                    return var3 == null && Objects.equals(this.rawType, var4) && Arrays.equals(this.actualTypeArguments, var2.getActualTypeArguments());
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.actualTypeArguments) ^ Objects.hashCode(null) ^ Objects.hashCode(this.rawType);
        }

        @Override
        public String toString() {
            StringBuilder var1 = new StringBuilder();
            var1.append(this.rawType.getName());

            if (this.actualTypeArguments != null && this.actualTypeArguments.length > 0) {
                var1.append("<");
                boolean var2 = true;
                Type[] var3 = this.actualTypeArguments;
                int var4 = var3.length;

                for(int var5 = 0; var5 < var4; ++var5) {
                    Type var6 = var3[var5];
                    if (!var2) {
                        var1.append(", ");
                    }

                    var1.append(var6.getTypeName());
                    var2 = false;
                }

                var1.append(">");
            }

            return var1.toString();
        }
    }
}
