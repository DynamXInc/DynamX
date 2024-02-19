package fr.dynamx.client.gui;

import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.GuiFloatField;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.dynamx.client.renders.mesh.shapes.FacesMesh;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.items.vehicle.ItemSoftbody;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import fr.dynamx.utils.physics.PhysicsRaycastResult;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyVector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

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

    private final List<Node> nodes = new ArrayList<>();

    public GuiSoftbodyConfig(ItemSoftbody<?> itemSoftbody, FacesMesh mesh, PhysicsSoftBody te) {
        super(new GuiScaler.Identity());

        softBody = te;
        this.mesh = mesh;

        IntBuffer faces = softBody.copyFaces(null);
        FloatBuffer nodeLocations = softBody.copyLocations(null);
        int numFaces = softBody.countFaces();
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
            Vector3f c = x[0].addLocal(x[1]).addLocal(x[2]).divideLocal(3);

            Node node = new Node(c, i, 0.03f);
            nodes.add(node);
        }

        //model = DynamXContext.getObjModelRegistry().getModel(softBody.getBlockObjectInfo().getModel());
        setCssClass("root");

        preview = new GuiPanel() {
            @Override
            public void drawForeground(int mouseX, int mouseY, float partialTicks) {
                super.drawForeground(mouseX, mouseY, partialTicks);
                int x = preview.getRenderMinX() + preview.getWidth() / 2;
                int y = preview.getRenderMinY() + preview.getHeight() / 2;
                drawModelOnScreen(x, y, mouseX, mouseY);

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

            itemSoftbody.softBody = softBody;
            itemSoftbody.changed = true;

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
    Vector3f test;

    public void drawModelOnScreen(float posX, float posY, float mouseX, float mouseY) {
        handleScaleAndRotation();
        Vector3fPool.openPool();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();

        //Point où est là souris relativement au centre du dessin (point 0, 0)
        float centerX = -(mouseX - (posX)) / ((200 + scale) / 2.5f);
        float centerY = (mouseY - (posY)) / ((200 + scale) / 2.5f);

        //Rotation du point à l'inverse de la rotation du gui (pour se mettre dans le repère du world, rotation 0, 0, 0)
        //Obligé de le faire en 2 fois pour que les rotations soient faites dans le bon ordre
        Vector3f p1 = DynamXGeometry.getRotatedPoint(Vector3fPool.get(centerX, centerY, 1), 0, -angleX, 0);
        Vector3f start = DynamXGeometry.getRotatedPoint(p1, -180 + angleY, 0, 0);

        //Idem pour la direction du rayon de raycast
        Vector3f p2 = DynamXGeometry.getRotatedPoint(Vector3fPool.get(0, 0, -1), 0, -angleX, 0);
        Vector3f direction = DynamXGeometry.getRotatedPoint(p2, -180 + angleY, 0, 0);
        MyVector3f.normalizeLocal(direction);
        Vector3f end = start.add(direction.multLocal(500));

        PhysicsRaycastResult physicsRaycastResult = DynamXPhysicsHelper.castRay(DynamXContext.getPhysicsWorld(Minecraft.getMinecraft().world),
                start, end, null);
        if (physicsRaycastResult != null && physicsRaycastResult.hitBody instanceof PhysicsSoftBody) {
            test = physicsRaycastResult.hitPos;
        }

        GlStateManager.translate(posX, posY, 400);
        GlStateManager.scale(200 + scale, 200 + scale, 200 + scale);
        renderGrid();

        //Wtf negative scale, don't remove it
        GlStateManager.scale(-1 / 2.5f, 1 / 2.5f, 1 / 2.5f);

        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.rotate(angleX, 0, 1, 0);
        GlStateManager.rotate(-angleY, 1, 0, 0);


        renderModel();


        GlStateManager.popMatrix();
        Vector3fPool.closePool();
    }

    public void handleScaleAndRotation() {
        int i = Mouse.getEventDWheel() / 100;
        if (i != 0) {
            scroll += i;
            int maxScroll = 1000;
            scroll = Math.max(Math.min(scroll, maxScroll), 0);
        }
        scale = scroll / 2f;
        if (Mouse.isButtonDown(2)) {
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

    public void renderGrid() {

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();


        GlStateManager.rotate(angleX, 0, 1, 0);
        GlStateManager.rotate(-angleY, 1, 0, 0);

        GlStateManager.color(1, 1, 1, 1);

        GlStateManager.color(1, 0, 0, 1);
        GlStateManager.glLineWidth(5);
        DynamXRenderUtils.arrowMeshX.render(1);
        GlStateManager.color(0, 1, 0, 1);
        DynamXRenderUtils.arrowMeshY.render(1);
        GlStateManager.color(0, 0, 1, 1);
        DynamXRenderUtils.arrowMeshZ.render(1);

        GlStateManager.translate(-1f, 0f, -1f);

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.glLineWidth(2);
        DynamXRenderUtils.gridMesh.render(1);


        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

    }

    public void renderModel() {


        GlStateManager.disableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1f);

        mesh.render(1);

        if (test != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(test.x, test.y, test.z);

            GlStateManager.scale(0.03, 0.03, 0.03);
            GlStateManager.color(1, 0, 1, 0.4f);
            DynamXRenderUtils.icosphereMeshToRender.render(1);
            GlStateManager.popMatrix();
        }

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            GlStateManager.pushMatrix();
            GlStateManager.translate(node.pos.x, node.pos.y, node.pos.z);
            GlStateManager.scale(0.03, 0.03, 0.03);
            if (test != null && node.aabb.contains(test)) {
                GlStateManager.color(0, 1, 0, 1f);
                if (Mouse.isButtonDown(0)) {
                    node.isFixed = true;
                }
                if (Mouse.isButtonDown(1)) {
                    node.isFixed = false;
                }
            } else {
                GlStateManager.color(1, 0, 0, 1f);
            }
            if (node.isFixed) {
                GlStateManager.color(0, 0, 1, 1f);
            }
            DynamXRenderUtils.icosphereMeshToRender.render(1);
            GlStateManager.popMatrix();

        }

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

    static class Node {
        Vector3f pos;
        int index;
        MutableBoundingBox aabb;
        boolean isFixed;

        Node(Vector3f pos, int index, float size) {
            this.pos = pos;
            this.index = index;
            aabb = new MutableBoundingBox(pos.x - size, pos.y - size, pos.z - size, pos.x + size, pos.y + size, pos.z + size);
        }
    }

}
