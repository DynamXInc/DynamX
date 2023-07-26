package fr.dynamx.client.renders.mesh;

import fr.dynamx.client.shaders.ShaderManager;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BatchMesh {

    private int vao = -1;
    private int indicesVbo = -1;
    private int positionsVbo = -1;
    private int normalsVbo = -1;
    private int texCoordsVbo = -1;

    private int indexSize, positionSize, normalSize, texCoordsSize;

    private int numIndices, numPositions, numNormals, numTexCoords;
    private boolean isValid, isInit;

    private int glTextureId;

    private IntBuffer fullIndexBuffer;
    private FloatBuffer fullPositionBuffer;
    private FloatBuffer fullNormalsBuffer;
    private FloatBuffer fullTexcoordsBuffer;

    public BatchMesh(ObjModelData objModelData, int maxMeshes, int glTextureId) {
        this(objModelData.getAllMeshIndices().length, objModelData.getVerticesPos().length,
                objModelData.getNormals().length, objModelData.getTexCoords().length, maxMeshes, glTextureId);
    }

    public BatchMesh(int indexSize, int positionSize, int normalSize, int texCoordsSize, int maxMeshes, int glTextureId) {
        //long totalSize = (long) (indexSize + positionSize + normalSize + texCoordsSize) * maxMeshes;
        long indexSizeMax = (long) indexSize * maxMeshes;
        long positionSizeMax = (long) (positionSize + (positionSize / 3)) * maxMeshes;
        long normalSizeMax = (long) normalSize * maxMeshes;
        long texCoordsSizeMax = (long) texCoordsSize * maxMeshes;
        if (indexSizeMax >= Integer.MAX_VALUE || positionSizeMax >= Integer.MAX_VALUE || normalSizeMax >= Integer.MAX_VALUE || texCoordsSizeMax >= Integer.MAX_VALUE) {
            DynamXMain.log.error("Size is too big. It should be less than Integer.MAX_VALUE (2 147 483 647)");
            throw new BufferOverflowException();
        }
        this.indexSize = (int) indexSizeMax;
        this.positionSize = (int) positionSizeMax;
        this.normalSize = (int) normalSizeMax;
        this.texCoordsSize = (int) texCoordsSizeMax;
        this.glTextureId = glTextureId;
       /* fullIndexBuffer = BufferUtils.createIntBuffer(this.indexSize);
        fullPositionBuffer = BufferUtils.createFloatBuffer(this.positionSize);
        fullNormalsBuffer = BufferUtils.createFloatBuffer(this.normalSize);
        fullTexcoordsBuffer = BufferUtils.createFloatBuffer(this.texCoordsSize);*/
    }

    public void init() {
        if (vao != -1) {
            delete();
        }
        vao = DynamXRenderUtils.genVertexArrays();
        DynamXRenderUtils.bindVertexArray(vao);

        indicesVbo = GL15.glGenBuffers();

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesVbo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (long) indexSize * Integer.BYTES, GL15.GL_DYNAMIC_DRAW);

        positionsVbo = GL15.glGenBuffers();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionsVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) positionSize * Float.BYTES, GL15.GL_DYNAMIC_DRAW);


        normalsVbo = GL15.glGenBuffers();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalsVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) normalSize * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

        texCoordsVbo = GL15.glGenBuffers();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, texCoordsVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) texCoordsSize * Float.BYTES, GL15.GL_DYNAMIC_DRAW);


        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        DynamXRenderUtils.bindVertexArray(0);
        isValid = true;
    }

    public void addMesh(ObjModelData modelData, Matrix4f transform, int light) {
        float[] verticesPos = modelData.getVerticesPos();
        IntBuffer indexBuffer = DynamXUtils.createIntBuffer(modelData.getAllMeshIndices());
        FloatBuffer positionBuffer = DynamXUtils.createFloatBuffer(verticesPos);
        FloatBuffer normalBuffer = DynamXUtils.createFloatBuffer(modelData.getNormals());
        FloatBuffer texCoords = DynamXUtils.createFloatBuffer(modelData.getTexCoords());


        FloatBuffer buffer = transformPositions(transform, positionBuffer, light);

        updateVBO(EnumGLIndex.INDEX, indexBuffer);
        updateVBO(EnumGLIndex.POSITION, buffer);
        updateVBO(EnumGLIndex.NORMAL, normalBuffer);
        updateVBO(EnumGLIndex.TEX_COORDS, texCoords);

    }

    public void updateVBO(EnumGLIndex glIndex, Buffer buffer) {
        if (vao == -1 || buffer == null || !isValid) {
            return;
        }
        DynamXRenderUtils.bindVertexArray(vao);


        switch (glIndex) {
            case INDEX:
                if (buffer.limit() > indexSize) {
                    DynamXMain.log.error("Index buffer is too big. It should be less than " + indexSize + " bytes");
                    return;
                }
                IntBuffer indexBuffer = (IntBuffer) buffer;

                IntBuffer tmp = BufferUtils.createIntBuffer(indexBuffer.capacity());
                for (int i = 0; i < tmp.capacity(); i++) {
                    int index = indexBuffer.get(i);
                    tmp.put(i, index + numPositions);
                }
                indexBuffer.rewind();
                tmp.rewind();


                //fullIndexBuffer.put(tmp);

                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesVbo);
                DynamXRenderUtils.checkForOglError("glBindBuffer indices");

                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, (long) numIndices * Integer.BYTES, tmp);
                DynamXRenderUtils.checkForOglError("glBufferSubData indices");

                numIndices += buffer.capacity();
                break;
            case POSITION:
                if (buffer.limit() > positionSize) {
                    DynamXMain.log.error("Position buffer is too big. It should be less than " + positionSize + " bytes");
                    return;
                }
                //fullPositionBuffer.put((FloatBuffer) buffer);

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionsVbo);

                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, numPositions * 4L * Float.BYTES, (FloatBuffer) buffer);

                GL11.glVertexPointer(3, GL11.GL_FLOAT, 4 * Float.BYTES, 0);
                GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, 4 * Float.BYTES, 3 * Float.BYTES);
                GL20.glEnableVertexAttribArray(5);

                DynamXRenderUtils.checkForOglError("add::glBufferSubData position");


                numPositions += buffer.capacity() / 4;
                break;
            case NORMAL:
                if (buffer.limit() > normalSize) {
                    DynamXMain.log.error("Normal buffer is too big. It should be less than " + normalSize + " bytes");
                    return;
                }
                // fullNormalsBuffer.put((FloatBuffer) buffer);

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalsVbo);

                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, numNormals * 3L * Float.BYTES, (FloatBuffer) buffer);

                GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);
                DynamXRenderUtils.checkForOglError("add::glBufferSubData normal");

                numNormals += buffer.capacity() / 3;
                break;
            case TEX_COORDS:
                if (buffer.limit() > texCoordsSize) {
                    DynamXMain.log.error("Tex coords buffer is too big. It should be less than " + texCoordsSize + " bytes");
                    return;
                }
                //fullTexcoordsBuffer.put((FloatBuffer) buffer);

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, texCoordsVbo);

                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, numTexCoords * 2L * Float.BYTES, (FloatBuffer) buffer);

                GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);
                DynamXRenderUtils.checkForOglError("add::glBufferSubData texcoords");

                numTexCoords += buffer.capacity() / 2;
                break;
        }


        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        DynamXRenderUtils.bindVertexArray(0);
    }

    public void render() {
        if (vao == -1 || !isValid) {
            return;
        }
        DynamXRenderUtils.bindVertexArray(vao);

        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);


        ShaderManager.test.setUniform("diffuseMap", 0);
        if (glTextureId != -1)
            GlStateManager.bindTexture(glTextureId);

        ShaderManager.test.setUniform("lightMap", 1);


        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesVbo);
        GL11.glDrawElements(GL11.GL_TRIANGLES, numIndices, GL11.GL_UNSIGNED_INT, 0);

        if (glTextureId != -1)
            GlStateManager.bindTexture(OpenGlHelper.defaultTexUnit);


        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);


        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        DynamXRenderUtils.bindVertexArray(0);
    }

    public void clean() {
        delete();
        init();
    }

    public void delete() {
        if (vao == -1) {
            return;
        }
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(indicesVbo);
        GL15.glDeleteBuffers(positionsVbo);
        GL15.glDeleteBuffers(normalsVbo);
        GL15.glDeleteBuffers(texCoordsVbo);
        vao = -1;
        indicesVbo = positionsVbo = normalsVbo = texCoordsVbo = -1;
        numIndices = numPositions = numNormals = numTexCoords = 0;
    }

    private FloatBuffer transformPositions(Matrix4f transform, FloatBuffer positionBuffer, float light) {
        Vector3f pos = new Vector3f();
        int capacity = positionBuffer.capacity();
        FloatBuffer translatedPosBuf = BufferUtils.createFloatBuffer(capacity + (capacity / 3));
        int positionCount = positionBuffer.limit() / 3;
        for (int l = 0; l < positionCount; l++) {
            pos.x = positionBuffer.get();
            pos.y = positionBuffer.get();
            pos.z = positionBuffer.get();

            mulPosition(pos, transform);

            translatedPosBuf.put(pos.x);
            translatedPosBuf.put(pos.y);
            translatedPosBuf.put(pos.z);

            translatedPosBuf.put(light);
        }
        translatedPosBuf.flip();
        positionBuffer.rewind();

        return translatedPosBuf;
    }

    public void mulPosition(Vector3f pos, Matrix4f mat) {
        float x = pos.x, y = pos.y, z = pos.z;
        pos.x = fma(mat.m00, x, fma(mat.m10, y, fma(mat.m20, z, mat.m30)));
        pos.y = fma(mat.m01, x, fma(mat.m11, y, fma(mat.m21, z, mat.m31)));
        pos.z = fma(mat.m02, x, fma(mat.m12, y, fma(mat.m22, z, mat.m32)));
    }

    public float fma(float a, float b, float c) {
        return a * b + c;
    }

}
