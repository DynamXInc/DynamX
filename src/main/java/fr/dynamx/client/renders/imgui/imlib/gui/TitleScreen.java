package fr.dynamx.client.renders.imgui.imlib.gui;

import net.minecraft.client.gui.GuiScreen;

import java.awt.*;
import java.io.IOException;

public class TitleScreen extends GuiScreen {

    public Color background = new Color(0.45f, 0.55f, 0.60f, 1.00f);
    float[] copy = new float[16];

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        /*ImGuiImpl.draw(io -> {
            if (ImGui.begin("Scene graph")) {
                ImGui.setNextWindowPos(0, 0);
                ImGui.setNextWindowSize(200, MC.displayHeight);

                ImGui.end();
            }


            if (ImGui.begin("Demo", ImGuiWindowFlags.AlwaysAutoResize)) {
                ImGuizmo.beginFrame();

                ImGuizmo.setEnabled(true);
                ImGuizmo.setRect(0, 0, MC.displayWidth, MC.displayHeight);


                if (!blocks.isEmpty()) {

                    TEDynamXBlock teDynamXBlock = blocks.get(0);
                    float[] originak = ((BlockNode) teDynamXBlock.getPackInfo().getSceneGraph()).transformData;

                    if (copy != null) {
                        System.arraycopy(originak, 0, copy, 0, 16);
                        BlockNode sceneGraph = (BlockNode) teDynamXBlock.getPackInfo().getSceneGraph();
                        BaseRenderContext.BlockRenderContext context = sceneGraph.getContext();

                        ImGuizmo.recomposeMatrixFromComponents(copy, sceneGraph.transformDataGuizmo, sceneGraph.rotation, sceneGraph.scale);
                        //ImGuizmo.drawCubes(tmpModelView, tmpProjection, originak);
                        if (context != null) {
                            copy[12] += context.getRenderPosition().x + 0.5f;
                            copy[13] += context.getRenderPosition().y + 0.5f;
                            copy[14] += context.getRenderPosition().z + 0.5f;
                        }
                        ImGuizmo.manipulate(tmpModelView, tmpProjection, copy, -1, Mode.WORLD);


                        if (context != null) {
                            copy[12] -= context.getRenderPosition().x - 0.5f;
                            copy[13] -= context.getRenderPosition().y - 0.5f;
                            copy[14] -= context.getRenderPosition().z - 0.5f;
                        }
                        ImGuizmo.decomposeMatrixToComponents(copy, sceneGraph.translation, sceneGraph.rotation, sceneGraph.scale);
                    }
                }
            }
            ImGui.end();
        });*/

    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        //ImGuiImpl.handleKey();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        //ImGuiImpl.handleMouse();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
