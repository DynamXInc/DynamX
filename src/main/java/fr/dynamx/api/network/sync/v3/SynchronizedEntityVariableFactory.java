package fr.dynamx.api.network.sync.v3;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import io.netty.buffer.ByteBuf;

public class SynchronizedEntityVariableFactory {
    public static final SynchronizedVariableSerializer<Byte> byteSerializer = new SynchronizedVariableSerializer<Byte>() {
        @Override
        public void writeObject(ByteBuf buffer, Byte object) {
            buffer.writeByte(object);
        }

        @Override
        public Byte readObject(ByteBuf buffer, Byte currentValue) {
            return buffer.readByte();
        }
    };
    public static final SynchronizedVariableSerializer<Integer> intSerializer = new SynchronizedVariableSerializer<Integer>() {
        @Override
        public void writeObject(ByteBuf buffer, Integer object) {
            buffer.writeInt(object);
        }

        @Override
        public Integer readObject(ByteBuf buffer, Integer currentValue) {
            return buffer.readInt();
        }
    };
    public static final SynchronizedVariableSerializer<Float> floatSerializer = new SynchronizedVariableSerializer<Float>() {
        @Override
        public void writeObject(ByteBuf buffer, Float object) {
            buffer.writeFloat(object);
        }

        @Override
        public Float readObject(ByteBuf buffer, Float currentValue) {
            return buffer.readFloat();
        }
    };
    public static final SynchronizedVariableSerializer<Boolean> booleanSerializer = new SynchronizedVariableSerializer<Boolean>() {
        @Override
        public void writeObject(ByteBuf buffer, Boolean object) {
            buffer.writeBoolean(object);
        }

        @Override
        public Boolean readObject(ByteBuf buffer, Boolean currentValue) {
            return buffer.readBoolean();
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
        public float[] readObject(ByteBuf buffer, float[] currentValue) {
            int size = buffer.readInt();
            for (int i = 0; i < size; i++)
                currentValue[i] = buffer.readFloat();
            return currentValue;
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
        public Vector3f readObject(ByteBuf buffer, Vector3f currentValue) {
            return currentValue.set(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
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
        public Quaternion readObject(ByteBuf buffer, Quaternion currentValue) {
            return currentValue.set(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }
    };

    public static <T> SynchronizedVariableSerializer<T> getSerializer(Class<T> dataType) {
        if(dataType == Integer.class)
            return (SynchronizedVariableSerializer<T>) intSerializer;
        if(dataType == Float.class)
            return (SynchronizedVariableSerializer<T>) floatSerializer;
        if(dataType == float[].class)
            return (SynchronizedVariableSerializer<T>) floatArraySerializer;
        throw new IllegalArgumentException("Not serializable " + dataType);
    }
}
