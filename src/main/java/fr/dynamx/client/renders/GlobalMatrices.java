package fr.dynamx.client.renders;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class GlobalMatrices {
    public static final FloatBuffer viewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    public static final FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
    public static final FloatBuffer invViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    public static final FloatBuffer invProjectionMatrixBuffer = BufferUtils.createFloatBuffer(16);

    public static final Matrix4f viewMatrix = new Matrix4f();
    public static final Matrix4f projectionMatrix = new Matrix4f();
    public static final Matrix4f invViewMatrix = new Matrix4f();
    public static final Matrix4f invProjectionMatrix = new Matrix4f();

    public static final float[] tmpModelView = new float[16];
    public static final float[] tmpProjection = new float[16];


}
