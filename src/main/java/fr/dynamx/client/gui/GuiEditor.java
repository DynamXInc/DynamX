package fr.dynamx.client.gui;

import dz.betterlights.BetterLightsMod;
import dz.betterlights.lighting.lightcasters.LightCaster;
import dz.betterlights.lighting.lightcasters.StaticLightCaster;
import fr.dynamx.client.renders.GlobalMatrices;
import fr.dynamx.client.renders.imgui.imlib.core.ImGuiImpl;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.BlockNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.utils.DynamXUtils;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Mode;
import imgui.extension.imguizmo.flag.Operation;
import imgui.flag.*;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static fr.dynamx.client.handlers.ClientEventHandler.MC;
import static fr.dynamx.client.renders.GlobalMatrices.tmpModelView;
import static fr.dynamx.client.renders.GlobalMatrices.tmpProjection;

public class GuiEditor {
    private SceneNode<?, ?> selectedNode;

    public static int currentGizmoOperation = -1;
    private int guizmoMode = Mode.WORLD;
    private boolean useSnap = false;
    private boolean boundSizing = false;

    private static final float[] INPUT_MATRIX_TRANSLATION = new float[3];
    private static final float[] INPUT_MATRIX_SCALE = new float[3];
    private static final float[] INPUT_MATRIX_ROTATION = new float[3];

    public static List<TEDynamXBlock> blocks = new ArrayList<>();
    public static List<LightCaster> lights = new ArrayList<>();

    public static List<List<?>> scene = new ArrayList<>();

    static {
    }


    public void drawGui() {
        scene.clear();
        scene.add(blocks);
        scene.add(lights);
        ImGuiImpl.draw(io -> {
            ImGui.setNextWindowPos(0, 0);
            ImGui.setNextWindowSize(MC.displayWidth, 100);
            int flags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoFocusOnAppearing | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse;
            if (ImGui.begin("DynamXEditor", flags)) {
                int flagsToolBar = ImGuiWindowFlags.NoFocusOnAppearing | ImGuiWindowFlags.NoTitleBar;
                ImGui.setNextWindowSize(650, -1);
                if (ImGui.begin("ToolBar", flagsToolBar)) {
                    if (ImGui.radioButton("World", guizmoMode == Mode.WORLD)) {
                        guizmoMode = Mode.WORLD;
                    }
                    ImGui.sameLine();
                    if (ImGui.radioButton("Local", guizmoMode == Mode.LOCAL)) {
                        guizmoMode = Mode.LOCAL;
                    }
                    ImGui.sameLine();
                    if (ImGui.checkbox("Snap", useSnap)) {
                        useSnap = !useSnap;
                    }
                    ImGui.sameLine();
                    if (ImGui.checkbox("BoundSizing", boundSizing)) {
                        boundSizing = !boundSizing;
                    }

                    ImGui.sameLine();
                    if (ImGui.radioButton("Translate", currentGizmoOperation == Operation.TRANSLATE)) {
                        currentGizmoOperation = Operation.TRANSLATE;
                    }
                    ImGui.sameLine();

                    if (ImGui.radioButton("Rotate", currentGizmoOperation == Operation.ROTATE)) {
                        currentGizmoOperation = Operation.ROTATE;
                    }

                    ImGui.sameLine();

                    if (ImGui.radioButton("Scale", currentGizmoOperation == Operation.SCALE)) {
                        currentGizmoOperation = Operation.SCALE;
                    }

                    ImGui.end();
                }

                ImGui.setNextWindowPos(0, 100);
                ImGui.setNextWindowSize(400, MC.displayHeight);
                if (ImGui.begin("Scene graph", ImGuiWindowFlags.AlwaysAutoResize)) {

                    if (ImGui.beginPopupContextWindow(ImGuiPopupFlags.MouseButtonRight)) {
                        if (ImGui.beginMenu("Lights")) {
                            if (ImGui.menuItem("Spotlight")) {
                                Vector3f forward = new Vector3f(GlobalMatrices.viewMatrix.m02(), GlobalMatrices.viewMatrix.m12(), GlobalMatrices.viewMatrix.m22());
                                Vector3f forwardDir = forward.mul(-7).add((float) MC.player.posX, (float) (MC.player.posY + MC.player.getEyeHeight()), (float) MC.player.posZ);
                                StaticLightCaster spotlight = (StaticLightCaster) new StaticLightCaster()
                                        .pos(DynamXUtils.toVector3f(forwardDir))
                                        .direction(0, -1, 0)
                                        .color(1f, 1f, 1f).setEnabled(true);
                                BetterLightsMod.getLightManager().addLightCaster(MC.world, spotlight, true);
                                lights.add(spotlight);
                            }
                            ImGui.endMenu();
                        }
                        ImGui.endPopup();
                    }

                    scene.forEach(list -> {
                        list.forEach(o -> {
                            if (o instanceof TEDynamXBlock) {
                                BlockNode<?> sceneGraph = (BlockNode<?>) ((TEDynamXBlock) o).getPackInfo().getSceneGraph();
                                BaseRenderContext.BlockRenderContext context = sceneGraph.getContext();
                                if (context != null) {
                                    if (ImGui.treeNode("Block " + ((TEDynamXBlock) o).getPackInfo().getName())) {
                                        if (ImGui.isItemClicked()) {
                                            selectedNode = sceneGraph;
                                            System.out.println(selectedNode);
                                        }
                                        sceneGraph.getLinkedChildren().forEach(node -> {
                                            if (node instanceof BlockNode) {
                                                BlockNode<?> child = (BlockNode<?>) node;
                                                if (ImGui.treeNode("Child ")) {
                                                    ImGui.text("Position: " + child.translation[0] + " " + child.translation[1] + " " + child.translation[2]);
                                                    ImGui.text("Rotation: " + child.rotation[0] + " " + child.rotation[1] + " " + child.rotation[2] + " " + child.rotation[3]);
                                                    ImGui.text("Scale: " + child.scale[0] + " " + child.scale[1] + " " + child.scale[2]);
                                                    ImGui.treePop();
                                                }
                                            }

                                        });
                                        ImGui.treePop();
                                    }
                                }
                            } else if (o instanceof LightCaster) {
                                if(ImGui.menuItem("Light " + ((LightCaster) o).getId())){

                                }
                            }
                        });

                    });
                    ImGui.end();

                    ImGui.setNextWindowPos(MC.displayWidth - 500, 100);
                    ImGui.setNextWindowSize(500, MC.displayHeight);
                    if (ImGui.begin("Properties", ImGuiWindowFlags.AlwaysAutoResize)) {

                        if (selectedNode != null) {

                            BlockNode<?> blockNode = (BlockNode<?>) selectedNode;
                            float x = blockNode.transformDataGuizmo[0], y = blockNode.transformDataGuizmo[1], z = blockNode.transformDataGuizmo[2];
                            float x1 = blockNode.transformDataGuizmo[3], y1 = blockNode.transformDataGuizmo[4], z1 = blockNode.transformDataGuizmo[5];
                            float x2 = blockNode.transformDataGuizmo[6], y2 = blockNode.transformDataGuizmo[7], z2 = blockNode.transformDataGuizmo[8];

                            Vector3f trans = new Vector3f(x, y, z);
                            Vector3f rot = new Vector3f(x1, y1, z1);
                            Vector3f scale = new Vector3f(x2, y2, z2);

                            drawVec3Control("Position", trans, new Vector3f(0, 0, 0));
                            drawVec3Control("Rotation", rot, new Vector3f(0, 0, 0));
                            drawVec3Control("Scale", scale, new Vector3f(1, 1, 1));


                            blockNode.translate(trans);
                            blockNode.rotate(rot);
                            blockNode.scale(scale);
                        }


                        ImGui.end();
                    }

                }
                ImGuizmo.beginFrame();

                ImGuizmo.setEnabled(true);
                ImGuizmo.setRect(0, 0, MC.displayWidth, MC.displayHeight);

                if (selectedNode != null) {
                    BlockNode<?> blockNode = (BlockNode<?>) selectedNode;
                    BaseRenderContext.BlockRenderContext context = blockNode.getContext();
                    if (context != null) {
                        handleGuizmo(context, blockNode);
                    }
                }
                ImGui.end();
            }

        });
    }

    private void handleGuizmo(BaseRenderContext.BlockRenderContext context, BlockNode<?> blockNode) {
        float[] originalData = blockNode.transformData;
        float[] copy = new float[16];

        if (blockNode.transformDataGuizmo == null) {
            return;
        }

        System.arraycopy(originalData, 0, copy, 0, 16);

        Vector3f trans = new Vector3f(blockNode.transformDataGuizmo[0], blockNode.transformDataGuizmo[1], blockNode.transformDataGuizmo[2]);

        Vector3f modelTranslation = new Vector3f(originalData[12], originalData[13], originalData[14]);
        ImGuizmo.recomposeMatrixFromComponents(copy, new float[]{modelTranslation.x, modelTranslation.y, modelTranslation.z}, blockNode.rotation, blockNode.scale);
        //ImGuizmo.drawCubes(tmpModelView, tmpProjection, originalData);

        ImGuizmo.manipulate(tmpModelView, tmpProjection, copy, currentGizmoOperation, guizmoMode);

        copy[12] -= modelTranslation.x;
        copy[13] -= modelTranslation.y;
        copy[14] -= modelTranslation.z;

        copy[12] += trans.x;
        copy[13] += trans.y;
        copy[14] += trans.z;

        ImGuizmo.decomposeMatrixToComponents(copy, blockNode.translation, blockNode.rotation, blockNode.scale);

    }

    private void drawVec3Control(String label, Vector3f vec, Vector3f resetValues) {
        float[] x = new float[]{vec.x};
        float[] y = new float[]{vec.y};
        float[] z = new float[]{vec.z};

        ImGui.pushID(label);

        ImGui.columns(2);
        ImGui.setColumnWidth(0, 100);
        ImGui.text(label);
        ImGui.nextColumn();

        float width = ImGui.calcItemWidth() / 3 + 20;
        ImGui.pushItemWidth(width);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);

        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0f;
        ImVec2 buttonSize = new ImVec2(lineHeight + 3.0f, lineHeight);

        ImGui.pushStyleColor(ImGuiCol.Button, 0.8f, 0.1f, 0.15f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.9f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.8f, 0.1f, 0.15f, 1.0f);
        if (ImGui.button("X", buttonSize.x, buttonSize.y)) {
            vec.x = x[0] = resetValues.x;
        }
        ImGui.popStyleColor(3);

        ImGui.sameLine();
        ImGui.dragFloat("##X", x, 0.1f, 0.0f, 0, "%.2f");
        ImGui.popItemWidth();
        ImGui.sameLine();

        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.7f, 0.2f, 1.0f);
        if (ImGui.button("Y", buttonSize.x, buttonSize.y)) {
            vec.y = y[0] = resetValues.y;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        ImGui.pushItemWidth(width);

        ImGui.dragFloat("##Y", y, 0.001f, 0.0f, 0, "%.2f");
        ImGui.popItemWidth();
        ImGui.sameLine();

        ImGui.pushStyleColor(ImGuiCol.Button, 0.1f, 0.25f, 0.8f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.2f, 0.35f, 0.9f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.1f, 0.25f, 0.8f, 1.0f);
        if (ImGui.button("Z", buttonSize.x, buttonSize.y)) {
            vec.z = z[0] = resetValues.z;
        }
        ImGui.popStyleColor(3);

        ImGui.sameLine();
        ImGui.pushItemWidth(width);
        ImGui.dragFloat("##Z", z, 0.1f, 0.0f, 0, "%.2f");
        ImGui.popItemWidth();

        ImGui.popStyleVar();
        ImGui.columns(1);

        ImGui.popID();

        vec.set(x[0], y[0], z[0]);
    }
}
