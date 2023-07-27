package fr.dynamx.client.gui;

import fr.aym.acsguis.component.layout.GridLayout;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.dynamx.client.DynamXModelRegistry;
import fr.dynamx.client.renders.mesh.BatchMesh;
import fr.dynamx.common.blocks.TEDynamXBlock;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;

import java.util.List;

public class GuiBatchConfig extends GuiFrame {
    public GuiBatchConfig() {
        super(new GuiScaler.Identity());
        GuiPanel panelBatch = new GuiPanel();
        panelBatch.setCssClass("panelBatch");
        panelBatch.setLayout(new GridLayout(-1, 25, 0, GridLayout.GridDirection.HORIZONTAL, 1));

        DynamXModelRegistry.getMODELS().forEach((resourceLocation, model) -> {
            if(model.getLocation().shouldBeBatched){
                GuiLabel label = new GuiLabel(resourceLocation.toString());
                GuiLabel meshCount = new GuiLabel("Starting mesh count : " + 1000);
                int count = 1000;
                long indexSize = (long) model.getObjModelData().getAllMeshIndices().length * Integer.BYTES * count;
                long vertexSize = (long) model.getObjModelData().getVerticesPos().length * Float.BYTES * count;
                long normalSize = (long) model.getObjModelData().getNormals().length * Float.BYTES * count;
                long uvSize = (long) model.getObjModelData().getTexCoords().length * Float.BYTES * count;

                long totalSize = indexSize + vertexSize + normalSize + uvSize;

                GuiLabel batchSize = new GuiLabel("Total batch size : " + FileUtils.byteCountToDisplaySize(totalSize));
                batchSize.setCssClass("batchSize");

                boolean hasBatch = model.getBatch().batchMesh != null;
                GuiLabel confirmBatch = new GuiLabel(hasBatch ? "Update Batch ?" : "Batch ?");
                confirmBatch.addClickListener((mx, my, button) -> {
                    model.getBatch().createBatch(count);
                });
                confirmBatch.setCssClass("confirmBatch");

                panelBatch.add(label);
                panelBatch.add(meshCount);
                panelBatch.add(batchSize);
                panelBatch.add(confirmBatch);

                if(hasBatch) {
                    GuiLabel deleteBatch = new GuiLabel("A batch already exists. Delete Batch ?");
                    deleteBatch.addClickListener((mx, my, button) -> {
                        model.getBatch().deleteBatch();
                    });
                    deleteBatch.setCssClass("deleteBatch");
                    panelBatch.add(deleteBatch);
                }



            }
        });


    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return null;
    }
}
