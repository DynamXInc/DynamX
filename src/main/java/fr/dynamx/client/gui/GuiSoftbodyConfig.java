package fr.dynamx.client.gui;

import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.GuiFloatField;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.dynamx.client.renders.mesh.shapes.FacesMesh;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import fr.dynamx.utils.physics.PhysicsRaycastResult;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyVector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiSoftbodyConfig extends GuiFrame {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/softbody_custom.css");

    private FacesMesh mesh;
    private PhysicsSoftBody softBody;
    private final GuiPanel preview;

    private final GuiFloatField translationX = new GuiFloatField(-100, 100);
    private final GuiFloatField translationY = new GuiFloatField(-100, 100);
    private final GuiFloatField translationZ = new GuiFloatField(-100, 100);
    private final GuiFloatField scaleX = new GuiFloatField(0, 100);
    private final GuiFloatField scaleY = new GuiFloatField(0, 100);
    private final GuiFloatField scaleZ = new GuiFloatField(0, 100);
    private final GuiFloatField rotationX = new GuiFloatField(-360, 360);
    private final GuiFloatField rotationY = new GuiFloatField(-360, 360);
    private final GuiFloatField rotationZ = new GuiFloatField(-360, 360);

    private float angleX = -27, angleY = 18;
    private float scale = 20;
    private int scroll = 0;

    private List<Vector3f> aabbs = new ArrayList<>();

    public GuiSoftbodyConfig(FacesMesh mesh, PhysicsSoftBody te) {
        super(new GuiScaler.Identity());

        softBody = te;
        this.mesh = mesh;

        IntBuffer faces = softBody.copyFaces(null);
        FloatBuffer nodeLocations = softBody.copyLocations(null);
        int numFaces = softBody.countFaces();
        Color col = new Color(0, 155, 0);
        for (int i = 0; i < numFaces; i++) {
            int vi1 = faces.get(3 * i);
            int vi2 = faces.get(3 * i + 1);
            int vi3 = faces.get(3 * i + 2);
            Vector3f nodePos1 = new Vector3f();
            Vector3f nodePos2 = new Vector3f();
            Vector3f nodePos3 = new Vector3f();
            MyBuffer.get(nodeLocations, 3 * vi1, nodePos1);
            MyBuffer.get(nodeLocations, 3 * vi2, nodePos2);
            MyBuffer.get(nodeLocations, 3 * vi3, nodePos3);
            Vector3f[] x = new Vector3f[]{nodePos1, nodePos2, nodePos3};
            Vector3f c = x[0].add(x[1]).add(x[2]).divideLocal(3);

            aabbs.add(c);
        }

        //model = DynamXContext.getObjModelRegistry().getModel(softBody.getBlockObjectInfo().getModel());
        setCssClass("root");

        preview = new GuiPanel() {
            @Override
            public void drawForeground(int mouseX, int mouseY, float partialTicks) {
                super.drawForeground(mouseX, mouseY, partialTicks);
                int x = preview.getRenderMinX() + preview.getWidth()  / 2;
                int y = preview.getRenderMinY() + preview.getHeight() / 2;
                drawModelOnScreen(x, y, mouseX, mouseY, preview.getWidth(), preview.getHeight());

            }
        };
        preview.setCssClass("preview");
        GuiLabel rotationLabel = new GuiLabel("Rotation :");
        rotationLabel.setCssClass("rotation");
        GuiLabel scaleLabel = new GuiLabel("Scale :");
        scaleLabel.setCssClass("scale");
        GuiLabel translationLabel = new GuiLabel("Translation :");
        translationLabel.setCssClass("translation");

        Transform transform = softBody.getTransform(null);
        translationX.setCssClass("translationX");
        translationX.setText(String.valueOf(transform.getTranslation().x));
        translationY.setCssClass("translationY");
        translationY.setText(String.valueOf(transform.getTranslation().y));
        translationZ.setCssClass("translationZ");
        translationZ.setText(String.valueOf(transform.getTranslation().z));

        scaleX.setCssClass("scaleX");
        scaleX.setText(transform.getScale().x != 0 ? String.valueOf(transform.getScale().x) : String.valueOf(1));
        scaleY.setCssClass("scaleY");
        scaleY.setText(transform.getScale().y != 0 ? String.valueOf(transform.getScale().y) : String.valueOf(1));
        scaleZ.setCssClass("scaleZ");
        scaleZ.setText(transform.getScale().z != 0 ? String.valueOf(transform.getScale().z) : String.valueOf(1));
        Vector3f angles = DynamXGeometry.toAngles(transform.getRotation());
        rotationX.setCssClass("rotationX");
        rotationX.setText(String.valueOf(angles.x));
        rotationY.setCssClass("rotationY");
        rotationY.setText(String.valueOf(angles.y));
        rotationZ.setCssClass("rotationZ");
        rotationZ.setText(String.valueOf(angles.z));

        GuiLabel confirm = new GuiLabel("Confirm");
        confirm.setCssClass("confirm");

        confirm.addClickListener((mx, my, button) -> {
            Vector3f relativeTrans = new Vector3f(translationX.getValue(), translationY.getValue(), translationZ.getValue());
            Vector3f relativeScale = new Vector3f(scaleX.getValue(), scaleY.getValue(), scaleZ.getValue());
            Vector3f relativeRotation = new Vector3f(rotationX.getValue(), rotationY.getValue(), rotationZ.getValue());
            //DynamXContext.getNetwork().sendToServer(new MessageSyncBlockCustomization(softBody.getPos(), relativeTrans, relativeScale, relativeRotation));
            Transform transform1 = new Transform(relativeTrans, DynamXGeometry.eulerToQuaternion(relativeRotation.x, relativeRotation.y, relativeRotation.z), relativeScale);
            softBody.applyTransform(transform1);
        });


        add(rotationLabel);
        add(scaleLabel);
        add(translationLabel);
        add(translationX);
        add(translationY);
        add(translationZ);
        add(rotationX);
        add(rotationY);
        add(rotationZ);
        add(scaleX);
        add(scaleY);
        add(scaleZ);
        add(confirm);
        add(preview);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void drawForeground(int mouseX, int mouseY, float partialTicks) {
        super.drawForeground(mouseX, mouseY, partialTicks);
    }

    BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();


    private final FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projectionInverseMatrix = BufferUtils.createFloatBuffer(16);
    public static final FloatBuffer modelViewInverse = BufferUtils.createFloatBuffer(16);
    public static final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private static final float[] tmpProjectionInverse = new float[16];
    private static final float[] tmpProjectionMatrix = new float[16];
    private static final float[] tmpModelViewInverse = new float[16];
    private static final float[] tmpModelView = new float[16];

    Vector3f test;

    public void drawModelOnScreen(float posX, float posY, float mouseX, float mouseY, float screenWidth, float screenHeight) {
        handleScaleAndRotation();
        unbindLayerBounds();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();


        //GlStateManager.translate(posX + getWidth(), posY + getHeight(), 400);


        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, (FloatBuffer) modelView.position(0));
        DynamXMath.invertMat4((FloatBuffer) modelViewInverse.position(0), (FloatBuffer) modelView.position(0),
                tmpModelViewInverse, tmpModelView);
        modelView.position(0);
        modelViewInverse.position(0);


        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, (FloatBuffer) projectionMatrix.position(0));
        DynamXMath.invertMat4((FloatBuffer) projectionInverseMatrix.position(0),
                (FloatBuffer) projectionMatrix.position(0),
                tmpProjectionInverse, tmpProjectionMatrix);
        projectionMatrix.position(0);
        projectionInverseMatrix.position(0);


        //System.out.println((        style.getWidth().getRawValue()) +" " + (        style.getHeight().getRawValue()));
        /*float mouseXPreview = mouseX;
        float mouseYPreview = mouseY;*/
        float centerX = -(mouseX - (preview.getRenderMinX() + (preview.getWidth() / 2f))) / ((50 + scale) / 5);
        float centerY = (mouseY - (preview.getRenderMinY() + (preview.getHeight() / 2f))) /((50 + scale) / 5);
        Vector3f rotatedPoint = DynamXGeometry.getRotatedPoint(new Vector3f(centerX, centerY, 0), angleX, -angleY, 0);
        //System.out.println(rotatedPoint);
        Vector3f rotatedPoint1 = DynamXGeometry.getRotatedPoint(new Vector3f(0, centerY, 0), angleX, -angleY, 0);
        float mouseXPreview = rotatedPoint.x
                + (preview.getRenderMinX() + (preview.getWidth() / 2f));
        float mouseYPreview = (rotatedPoint.y)
                + (preview.getRenderMinY() + (preview.getHeight() / 2f));
        //System.out.println(mouseXPreview + " " + mouseYPreview);
        Vector3f start = getWorldCoordinates(mouseXPreview, mouseYPreview, 0);
        start.z = 0;
        Vector3f far = getWorldCoordinates(mouseXPreview, mouseYPreview, 1);
        Vector3f direction = far.subtract(start);
        MyVector3f.normalizeLocal(direction);
        Vector3f fin$ = start.add(direction.multLocal(500));

        //test = start;
        //System.out.println(test);

        /*if (test != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(test.x, test.y, 400);

            GlStateManager.scale(10, 10, 10);
            GlStateManager.color(1, 0, 0, 1f);
            DynamXRenderUtils.icosphereMeshToRender.render();
            GlStateManager.popMatrix();
        }*/

        PhysicsRaycastResult physicsRaycastResult = DynamXPhysicsHelper.castRay(DynamXContext.getPhysicsWorld(Minecraft.getMinecraft().world), start, fin$, null);
        Vector3f min = softBody.boundingBox(null).getMin(null);
        Vector3f max = softBody.boundingBox(null).getMax(null);
        if(physicsRaycastResult != null && physicsRaycastResult.hitBody instanceof PhysicsSoftBody)
        {
            System.out.println(physicsRaycastResult);
            test = physicsRaycastResult.hitPos;

        }
        //System.out.println(physicsRaycastResult +" from :" + start +" to:" + fin$ +" bb min:" + min +" max:" +max);

        GlStateManager.translate(posX, posY, 400);
        GlStateManager.scale(20 + scale, 20 + scale, 20 + scale);
        renderGrid();

        //Wtf negative scale, don't remove it
        GlStateManager.scale(-1 / 5f, 1 / 5f, 1 / 5f);

        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.rotate(angleX, 0, 1, 0);
        GlStateManager.rotate(-angleY, 1, 0, 0);


        renderModel();


        GlStateManager.popMatrix();

    }

    public void handleScaleAndRotation() {
        int i = Mouse.getEventDWheel() / 100;
        if (i != 0) {
            scroll += i;
            int maxScroll = 1000;
            scroll = Math.max(Math.min(scroll, maxScroll), 0);
        }
        scale = scroll / 2f;
        if (Mouse.isButtonDown(0)) {
            if (mouseX > preview.getRenderMinX() && mouseX < preview.getRenderMaxX()) {
                if (mouseY > preview.getRenderMinY() && mouseY < preview.getRenderMaxY()) {
                    angleX += Mouse.getDX() / 2f;
                    angleY -= Mouse.getDY() / 2f;
                }
            }
        }
        if (angleX >= 360) angleX = 0;
        if (angleY <= -360) angleY = 0;
    }

    private Vector3f getWorldCoordinates(float mouseX, float mouseY, float projZ) {

        float[] projMatArray = new float[16];
        projectionMatrix.get(projMatArray);
        projectionMatrix.position(0);

        Matrix4f projMat = new Matrix4f();
        set(projMat, projMatArray, true);


        float[] modelViewArray = new float[16];
        modelView.get(modelViewArray);
        modelView.position(0);

        Matrix4f modelViewMat = new Matrix4f();
        set(modelViewMat, modelViewArray, true);

        Matrix4f viewProjMat = mult(projMat, modelViewMat, null);
        invertLocal(viewProjMat);

        float viewPortTop = preview.getRenderMinY();
        float viewPortBottom = preview.getRenderMaxY();
        float viewPortLeft = preview.getRenderMinX();
        float viewPortRight = preview.getRenderMaxX();

        Vector3f store = new Vector3f(
                (mouseX - preview.getRenderMinX())/(preview.getWidth()) * 2 - 1,
                (mouseY - preview.getRenderMinY())/(preview.getHeight()) * 2 - 1,
                projZ * 2 - 1);

        float w = multProj(viewProjMat, store, store);
        //store.multLocal(1f / w);


        return store;
    }

    public void renderGrid() {

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();


        GlStateManager.rotate(angleX, 0, 1, 0);
        GlStateManager.rotate(-angleY, 1, 0, 0);

        GlStateManager.color(1, 1, 1, 1);

        GlStateManager.color(1, 0, 0, 1);
        GlStateManager.glLineWidth(5);
        DynamXRenderUtils.arrowMeshX.render();
        GlStateManager.color(0, 1, 0, 1);
        DynamXRenderUtils.arrowMeshY.render();
        GlStateManager.color(0, 0, 1, 1);
        DynamXRenderUtils.arrowMeshZ.render();

        GlStateManager.translate(-1f, 0f, -1f);

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.glLineWidth(2);
        DynamXRenderUtils.gridMesh.render();


        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

    }

    public void renderModel() {


        GlStateManager.disableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1f);

        mesh.render();

        if (test != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(test.x, test.y, test.z);

            GlStateManager.scale(0.1, 0.1, 0.1);
            GlStateManager.color(1, 0, 0, 1f);
            DynamXRenderUtils.icosphereMeshToRender.render();
            GlStateManager.popMatrix();
        }

        /*for (int i = 0; i < aabbs.size(); i++) {
            Vector3f pos = aabbs.get(i);
            GlStateManager.pushMatrix();
            DynamXRenderUtils.glTranslate(pos);
            GlStateManager.scale(0.03,0.03,0.03);
            GlStateManager.color(0,0,0, 0.4f);

            //DynamXRenderUtils.glTranslate(Vector3fPool.get(pos).multLocal(10));
            DynamXRenderUtils.icosphereMeshToRender.render();
            //DynamXRenderUtils.glTranslate(Vector3fPool.get(pos).multLocal(10).negateLocal());
            GlStateManager.popMatrix();


        }*/
        GlStateManager.disableDepth();

        GlStateManager.enableTexture2D();


    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(STYLE);
    }

    @Override
    public boolean doesPauseGame() {
        return false;
    }

    @Override
    public boolean needsCssReload() {
        return false;
    }


    /**
     * Load all elements from the specified array.
     *
     * @param matrix   the source array (not null, length=16, unaffected)
     * @param rowMajor true if the source array is in row-major order, false if
     *                 it's in column-major order
     */
    public void set(Matrix4f mat, float[] matrix, boolean rowMajor) {
        if (matrix.length != 16) {
            throw new IllegalArgumentException(
                    "Array must be of size 16.");
        }

        if (rowMajor) {
            mat.m00 = matrix[0];
            mat.m01 = matrix[1];
            mat.m02 = matrix[2];
            mat.m03 = matrix[3];
            mat.m10 = matrix[4];
            mat.m11 = matrix[5];
            mat.m12 = matrix[6];
            mat.m13 = matrix[7];
            mat.m20 = matrix[8];
            mat.m21 = matrix[9];
            mat.m22 = matrix[10];
            mat.m23 = matrix[11];
            mat.m30 = matrix[12];
            mat.m31 = matrix[13];
            mat.m32 = matrix[14];
            mat.m33 = matrix[15];
        } else {
            mat.m00 = matrix[0];
            mat.m01 = matrix[4];
            mat.m02 = matrix[8];
            mat.m03 = matrix[12];
            mat.m10 = matrix[1];
            mat.m11 = matrix[5];
            mat.m12 = matrix[9];
            mat.m13 = matrix[13];
            mat.m20 = matrix[2];
            mat.m21 = matrix[6];
            mat.m22 = matrix[10];
            mat.m23 = matrix[14];
            mat.m30 = matrix[3];
            mat.m31 = matrix[7];
            mat.m32 = matrix[11];
            mat.m33 = matrix[15];
        }
    }


    /**
     * Right-multiply by the specified matrix. (This matrix is the left factor.)
     *
     * @param in2   the right factor (not null)
     * @param store storage for the result (modified) or null to create a new
     *              matrix. It is safe for in2 and store to be the same object.
     * @return the product, this times in2 (either store or a new instance)
     */
    public Matrix4f mult(Matrix4f in1, Matrix4f in2, Matrix4f store) {
        if (store == null) {
            store = new Matrix4f();
        }

        float[] m = new float[16];

        m[0] = in1.m00 * in2.m00
                + in1.m01 * in2.m10
                + in1.m02 * in2.m20
                + in1.m03 * in2.m30;
        m[1] = in1.m00 * in2.m01
                + in1.m01 * in2.m11
                + in1.m02 * in2.m21
                + in1.m03 * in2.m31;
        m[2] = in1.m00 * in2.m02
                + in1.m01 * in2.m12
                + in1.m02 * in2.m22
                + in1.m03 * in2.m32;
        m[3] = in1.m00 * in2.m03
                + in1.m01 * in2.m13
                + in1.m02 * in2.m23
                + in1.m03 * in2.m33;

        m[4] = in1.m10 * in2.m00
                + in1.m11 * in2.m10
                + in1.m12 * in2.m20
                + in1.m13 * in2.m30;
        m[5] = in1.m10 * in2.m01
                + in1.m11 * in2.m11
                + in1.m12 * in2.m21
                + in1.m13 * in2.m31;
        m[6] = in1.m10 * in2.m02
                + in1.m11 * in2.m12
                + in1.m12 * in2.m22
                + in1.m13 * in2.m32;
        m[7] = in1.m10 * in2.m03
                + in1.m11 * in2.m13
                + in1.m12 * in2.m23
                + in1.m13 * in2.m33;

        m[8] = in1.m20 * in2.m00
                + in1.m21 * in2.m10
                + in1.m22 * in2.m20
                + in1.m23 * in2.m30;
        m[9] = in1.m20 * in2.m01
                + in1.m21 * in2.m11
                + in1.m22 * in2.m21
                + in1.m23 * in2.m31;
        m[10] = in1.m20 * in2.m02
                + in1.m21 * in2.m12
                + in1.m22 * in2.m22
                + in1.m23 * in2.m32;
        m[11] = in1.m20 * in2.m03
                + in1.m21 * in2.m13
                + in1.m22 * in2.m23
                + in1.m23 * in2.m33;

        m[12] = in1.m30 * in2.m00
                + in1.m31 * in2.m10
                + in1.m32 * in2.m20
                + in1.m33 * in2.m30;
        m[13] = in1.m30 * in2.m01
                + in1.m31 * in2.m11
                + in1.m32 * in2.m21
                + in1.m33 * in2.m31;
        m[14] = in1.m30 * in2.m02
                + in1.m31 * in2.m12
                + in1.m32 * in2.m22
                + in1.m33 * in2.m32;
        m[15] = in1.m30 * in2.m03
                + in1.m31 * in2.m13
                + in1.m32 * in2.m23
                + in1.m33 * in2.m33;

        store.m00 = m[0];
        store.m01 = m[1];
        store.m02 = m[2];
        store.m03 = m[3];
        store.m10 = m[4];
        store.m11 = m[5];
        store.m12 = m[6];
        store.m13 = m[7];
        store.m20 = m[8];
        store.m21 = m[9];
        store.m22 = m[10];
        store.m23 = m[11];
        store.m30 = m[12];
        store.m31 = m[13];
        store.m32 = m[14];
        store.m33 = m[15];
        return store;
    }

    public Matrix4f invertLocal(Matrix4f mat) {

        float fA0 = mat.m00 * mat.m11 - mat.m01 * mat.m10;
        float fA1 = mat.m00 * mat.m12 - mat.m02 * mat.m10;
        float fA2 = mat.m00 * mat.m13 - mat.m03 * mat.m10;
        float fA3 = mat.m01 * mat.m12 - mat.m02 * mat.m11;
        float fA4 = mat.m01 * mat.m13 - mat.m03 * mat.m11;
        float fA5 = mat.m02 * mat.m13 - mat.m03 * mat.m12;
        float fB0 = mat.m20 * mat.m31 - mat.m21 * mat.m30;
        float fB1 = mat.m20 * mat.m32 - mat.m22 * mat.m30;
        float fB2 = mat.m20 * mat.m33 - mat.m23 * mat.m30;
        float fB3 = mat.m21 * mat.m32 - mat.m22 * mat.m31;
        float fB4 = mat.m21 * mat.m33 - mat.m23 * mat.m31;
        float fB5 = mat.m22 * mat.m33 - mat.m23 * mat.m32;
        float fDet = fA0 * fB5 - fA1 * fB4 + fA2 * fB3 + fA3 * fB2 - fA4 * fB1 + fA5 * fB0;

        if (FastMath.abs(fDet) <= 0f) {
            return zero(mat);
        }

        float f00 = +mat.m11 * fB5 - mat.m12 * fB4 + mat.m13 * fB3;
        float f10 = -mat.m10 * fB5 + mat.m12 * fB2 - mat.m13 * fB1;
        float f20 = +mat.m10 * fB4 - mat.m11 * fB2 + mat.m13 * fB0;
        float f30 = -mat.m10 * fB3 + mat.m11 * fB1 - mat.m12 * fB0;
        float f01 = -mat.m01 * fB5 + mat.m02 * fB4 - mat.m03 * fB3;
        float f11 = +mat.m00 * fB5 - mat.m02 * fB2 + mat.m03 * fB1;
        float f21 = -mat.m00 * fB4 + mat.m01 * fB2 - mat.m03 * fB0;
        float f31 = +mat.m00 * fB3 - mat.m01 * fB1 + mat.m02 * fB0;
        float f02 = +mat.m31 * fA5 - mat.m32 * fA4 + mat.m33 * fA3;
        float f12 = -mat.m30 * fA5 + mat.m32 * fA2 - mat.m33 * fA1;
        float f22 = +mat.m30 * fA4 - mat.m31 * fA2 + mat.m33 * fA0;
        float f32 = -mat.m30 * fA3 + mat.m31 * fA1 - mat.m32 * fA0;
        float f03 = -mat.m21 * fA5 + mat.m22 * fA4 - mat.m23 * fA3;
        float f13 = +mat.m20 * fA5 - mat.m22 * fA2 + mat.m23 * fA1;
        float f23 = -mat.m20 * fA4 + mat.m21 * fA2 - mat.m23 * fA0;
        float f33 = +mat.m20 * fA3 - mat.m21 * fA1 + mat.m22 * fA0;

        mat.m00 = f00;
        mat.m01 = f01;
        mat.m02 = f02;
        mat.m03 = f03;
        mat.m10 = f10;
        mat.m11 = f11;
        mat.m12 = f12;
        mat.m13 = f13;
        mat.m20 = f20;
        mat.m21 = f21;
        mat.m22 = f22;
        mat.m23 = f23;
        mat.m30 = f30;
        mat.m31 = f31;
        mat.m32 = f32;
        mat.m33 = f33;

        float fInvDet = 1.0f / fDet;
        mat.multLocal(fInvDet);

        return mat;
    }

    public Matrix4f zero(Matrix4f mat) {
        mat.m00 = mat.m01 = mat.m02 = mat.m03 = 0.0f;
        mat.m10 = mat.m11 = mat.m12 = mat.m13 = 0.0f;
        mat.m20 = mat.m21 = mat.m22 = mat.m23 = 0.0f;
        mat.m30 = mat.m31 = mat.m32 = mat.m33 = 0.0f;
        return mat;
    }

    public float multProj(Matrix4f mat, Vector3f vec, Vector3f store) {
        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = mat.m00 * vx + mat.m01 * vy + mat.m02 * vz + mat.m03;
        store.y = mat.m10 * vx + mat.m11 * vy + mat.m12 * vz + mat.m13;
        store.z = mat.m20 * vx + mat.m21 * vy + mat.m22 * vz + mat.m23;
        return mat.m30 * vx + mat.m31 * vy + mat.m32 * vz + mat.m33;
    }
}
