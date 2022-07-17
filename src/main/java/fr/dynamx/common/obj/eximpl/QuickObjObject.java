package fr.dynamx.common.obj.eximpl;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.obj.Material;
import fr.dynamx.common.obj.Mesh;
import fr.dynamx.common.obj.Vertex;
import fr.dynamx.common.obj.texture.MaterialTexture;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.utils.DynamXLoadingTasks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

public class QuickObjObject implements IObjObject {
    // Use THIS instead of GlStateManager, it has weird issues due to last bind texture memory and display lists
    private static int bindTexture;

    /**
     * Binds the texture, if not already bound
     */
    private static void bindTexture(int id) {
        if (id != bindTexture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            bindTexture = id;
        }
    }

    /**
     * Resets memory of last bind texture
     */
    private static void startDrawing() {
        bindTexture = -1;
    }

    private final Mesh mesh = new Mesh();
    private Vector3f center;
    private final String name;

    private final Map<Byte, Integer> modelDisplayList = new HashMap<>();

    public QuickObjObject(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Vector3f getCenter() {
        return center;
    }

    @Override
    public void setCenter(Vector3f center) {
        this.center = center;
    }

    @Override
    public Mesh getMesh() {
        return mesh;
    }

    @Override
    public void clearDisplayLists() {
        if (!modelDisplayList.isEmpty()) {
            modelDisplayList.forEach((textureID, displayList) -> {
                // If the list was created previously, we free the GPU memory
                GlStateManager.glDeleteLists(displayList, 1);
            });
            modelDisplayList.clear();
        }
    }

    @Override
    public void createList(TextureData useDefault, TextureData textureData, ObjModelClient model, boolean logIfNotFound) {
        if (!isMaterialValid(model, mesh.materials[0]))
            return;
        boolean isCustom = textureData.getId() == 0; //0 is the default, base texture
        if (!isCustom) {
            for (Material m : mesh.materials) {
                if (m.diffuseTexture.containsKey(textureData.getName())) {
                    isCustom = true;
                    break;
                }
            }
        }
        if (isCustom) {
            // Create an empty display list
            int id = GlStateManager.glGenLists(1);
            // Start the compilation of the list, this will fill the list with every vertex rendered onwards
            GlStateManager.glNewList(id, GL11.GL_COMPILE);
            //Do render
            renderCPU(model, useDefault.getName(), textureData.getName());
            // Finish the compilation process
            GlStateManager.glEndList();
            modelDisplayList.put(textureData.getId(), id);
            //log.info("Compile list for "+textureData+" (default is "+useDefault+") for part "+getName()+" of "+model.getLocation());
        } else {
            if (logIfNotFound)
                log.error("Failed to find custom texture for skin " + textureData.getName() + " of " + model.getLocation() + " in part " + getName());
            modelDisplayList.put(textureData.getId(), modelDisplayList.get(useDefault.getId()));
        }
    }

    @Override
    public void createDefaultList(ObjModelClient model) {
        if (!isMaterialValid(model, mesh.materials[0]))
            return;
        // Create an empty display list
        int id = GlStateManager.glGenLists(1);
        // Start the compilation of the list, this will fill the list with every vertex rendered onwards
        GlStateManager.glNewList(id, GL11.GL_COMPILE);
        //Do render
        renderCPU(model, "Default", "Default");
        // Finish the compilation process
        GlStateManager.glEndList();
        //log.info("Compile default list for part "+getName()+" of "+model.getLocation());
        modelDisplayList.put((byte) 0, id);
    }

    @Override
    public void render(ObjModelClient model, byte textureDataId) {
        if (mesh.materials.length == 0 || !isMaterialValid(model, mesh.materials[0]))
            return;
        if (!modelDisplayList.containsKey(textureDataId)) {
            GlStateManager.color(1, 0, 0);
            GlStateManager.callList(modelDisplayList.get((byte) 0));
            GlStateManager.color(1, 1, 1);
        } else
            GlStateManager.callList(modelDisplayList.get(textureDataId));
        //GL11.glBindTexture(GL11.GL_TEXTURE_2D, Minecraft.getMinecraft().getTextureMapBlocks().getGlTextureId());
        GlStateManager.bindTexture(Minecraft.getMinecraft().getTextureMapBlocks().getGlTextureId()); //Confirm to mc that used texture has changed
    }

    private String getExistingTexture(Material material, String first, String second) {
        return material.diffuseTexture.containsKey(first) ? first : material.diffuseTexture.containsKey(second) ? second : "Default";
    }

    /**
     * Model is assumed to be not empty
     */
    private void renderCPU(ObjModelClient model, String useDefault, String textureName) {
        //Reset bind texture
        startDrawing();
        // The model is not compiled, this shouldn't happen if the setup is correct
        Tessellator tess = Tessellator.getInstance();
        // WorldRenderer renderer = tess.getWorldRenderer();
        BufferBuilder renderer = tess.getBuffer();
        Vector3f color = new Vector3f(1, 1, 1);
        float alpha = 1f;
        Material bind = null;
        bind = mesh.materials[0];
        String bindName = getExistingTexture(mesh.materials[0], textureName, useDefault);
        MaterialTexture materialMultipleTextures = mesh.materials[0].diffuseTexture.get(bindName);
        if (materialMultipleTextures != null) {
            bindTexture(mesh.materials[0].diffuseTexture.get(bindName).getGlTextureId());
        } else {
            log.error("Failed to load Default texture of " + getName() + " in " + model.getLocation() + " in material " + mesh.materials[0].getName());
        }
        int[] indices = mesh.indices;
        Vertex[] vertices = mesh.vertices;

        boolean begining = true, drawing = false;
        for (int i = 0; i < indices.length; i += 3) {
            int i0 = indices[i];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];
            Vertex v0 = vertices[i0];
            Vertex v1 = vertices[i1];
            Vertex v2 = vertices[i2];

            if (isMaterialValid(model, mesh.materials[i / 3]) && mesh.materials[i / 3] != bind) {
                bind = mesh.materials[i / 3];
                //System.out.println("Bind material " + bind.getName() + " " + bind.diffuseTex + " " + bind.diffuseTexture + " in model " + this.filename);
                if (drawing)
                    tess.draw();
                bindName = getExistingTexture(mesh.materials[i / 3], textureName, useDefault);
                materialMultipleTextures = mesh.materials[i / 3].diffuseTexture.get(bindName);
                if (materialMultipleTextures != null) {
                    bindTexture(mesh.materials[i / 3].diffuseTexture.get(bindName).getGlTextureId());
                } else {
                    log.error("Failed to load Default texture of " + getName() + " in " + model.getLocation() + " in material " + mesh.materials[i / 3].getName());
                }
                begining = true;
            }
            if (begining) {
                begining = false;
                renderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
                drawing = true;
            }
            renderer.pos(v0.getPos().x, v0.getPos().y, v0.getPos().z).tex(v0.getTexCoords().x, 1f - v0.getTexCoords().y).normal(v0.getNormal().x, v0.getNormal().y, v0.getNormal().z).endVertex();
            renderer.pos(v1.getPos().x, v1.getPos().y, v1.getPos().z).tex(v1.getTexCoords().x, 1f - v1.getTexCoords().y).normal(v1.getNormal().x, v1.getNormal().y, v1.getNormal().z).endVertex();
            renderer.pos(v2.getPos().x, v2.getPos().y, v2.getPos().z).tex(v2.getTexCoords().x, 1f - v2.getTexCoords().y).normal(v2.getNormal().x, v2.getNormal().y, v2.getNormal().z).endVertex();
        }
        if (drawing)
            tess.draw();
        bindTexture(Minecraft.getMinecraft().getTextureMapBlocks().getGlTextureId());
    }

    private boolean isMaterialValid(ObjModelClient model, Material material) {
        if (material == null)
            return false;
        if (material.getName().equals("none")) //BlockBench uses "none" materials, this is a bug
        {
            if (!model.hasNoneMaterials) {
                log.error("Invalid object " + getName() + " in model " + model.getLocation() + " : uses 'none' material of BlockBench");
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.MODEL, model.getCustomTextures() != null ? model.getCustomTextures().getPackName() : "Non-pack model", model.getLocation().toString(),
                        "Invalid object " + getName() + " : uses 'none' material of BlockBench", ErrorTrackingService.TrackedErrorLevel.LOW);
            }
            model.hasNoneMaterials = true;
            return false;
        }
        return true;
    }
}
