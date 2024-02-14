package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.api.events.DynamXArmorEvent;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.DynamXModelRegistry;
import fr.dynamx.client.renders.model.MissingObjModel;
import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.utils.client.ClientDynamXUtils;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;

public class ArmorRenderer extends ModelRenderer {
    private final ModelObjArmor model;
    private final DxModelRenderer objModel;
    private ObjObjectRenderer objObjectRenderer;

    public ArmorRenderer(DxModelRenderer objModel, ModelObjArmor model, String partName) {
        super(model, partName);
        this.model = model;
        this.objModel = objModel;
        if (objModel != DynamXModelRegistry.MISSING_MODEL) {
            switch (objModel.getFormat()) {
                case OBJ:
                    for (ObjObjectRenderer objObjectRenderer1 : ((ObjModelRenderer) objModel).getObjObjects()) {
                        if (objObjectRenderer1.getObjObjectData().getName().equalsIgnoreCase(partName)) {
                            objObjectRenderer = objObjectRenderer1;
                            break;
                        }
                    }
                    break;
            }

        }
        if (objObjectRenderer == null) {
            objObjectRenderer = MissingObjModel.getEmptyPart();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void render(float scale) {
        if (!this.isHidden) {
            if (this.showModel) {
                if (MinecraftForge.EVENT_BUS.post(new DynamXArmorEvent.Render(model, objModel, objObjectRenderer, PhysicsEntityEvent.Phase.PRE, DynamXArmorEvent.Render.Type.NORMAL)))
                    return;
                GlStateManager.pushMatrix();
                GlStateManager.translate(this.rotationPointX, this.rotationPointY, this.rotationPointZ);
                GlStateManager.rotate(180, 1, 0, 0);
                rotateXYZ(false);
                GlStateManager.translate(-this.offsetX, this.offsetY, -this.offsetZ);
                switch (objModel.getFormat()) {
                    case OBJ:
                        ((ObjModelRenderer) objModel).renderGroup(objObjectRenderer, model.getActiveTextureId());
                        break;
                    //TODO YANIS: GLTF
                }
                GlStateManager.popMatrix();
            }
        }
    }

    /**
     * Renders the model with the given transformation matrix <br>
     * Like {@link ModelRenderer#render(float)}, but with a transformation matrix
     *
     * @param transform The transformation matrix to use, modified by the model's transformations
     */
    @SideOnly(Side.CLIENT)
    public void render(Matrix4f transform) {
        if (!this.isHidden) {
            if (this.showModel) {
                if (MinecraftForge.EVENT_BUS.post(new DynamXArmorEvent.Render(model, objModel, objObjectRenderer, PhysicsEntityEvent.Phase.PRE, DynamXArmorEvent.Render.Type.NORMAL)))
                    return;
                transform.translate(this.rotationPointX, this.rotationPointY, this.rotationPointZ);
                transform.rotate((float) Math.PI, 1, 0, 0);
                if (this.rotateAngleZ != 0.0F) {
                    transform.rotate(this.rotateAngleZ, 0.0F, 0.0F, 1.0F);
                }
                if (this.rotateAngleY != 0.0F) {
                    transform.rotate(this.rotateAngleY, 0.0F, 1.0F, 0.0F);
                }
                if (this.rotateAngleX != 0.0F) {
                    transform.rotate(this.rotateAngleX, 1.0F, 0.0F, 0.0F);
                }
                transform.translate(-this.offsetX, this.offsetY, -this.offsetZ);
                GlStateManager.pushMatrix();
                GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
                switch (objModel.getFormat()) {
                    case OBJ:
                        ((ObjModelRenderer) objModel).renderGroup(objObjectRenderer, model.getActiveTextureId());
                        break;
                    //TODO YANIS: GLTF
                }
                GlStateManager.popMatrix();
            }
        }
    }


    @Override
    @SideOnly(Side.CLIENT)
    public void renderWithRotation(float scale) {
        if (!this.isHidden) {
            if (this.showModel) {
                if (MinecraftForge.EVENT_BUS.post(new DynamXArmorEvent.Render(model, objModel, objObjectRenderer, PhysicsEntityEvent.Phase.PRE, DynamXArmorEvent.Render.Type.WITH_ROTATION)))
                    return;
                GlStateManager.pushMatrix();
                GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
                rotateXYZ(true);
                switch (objModel.getFormat()) {
                    case OBJ:
                        ((ObjModelRenderer) objModel).renderGroup(objObjectRenderer, model.getActiveTextureId());
                        break;
                    //TODO YANIS: GLTF
                }
                GlStateManager.popMatrix();
            }
        }
    }

    /**
     * Allows the changing of Angles after a box has been rendered
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void postRender(float scale) {
        if (!this.isHidden) {
            if (this.showModel) {
                if (MinecraftForge.EVENT_BUS.post(new DynamXArmorEvent.Render(model, objModel, objObjectRenderer, PhysicsEntityEvent.Phase.POST, DynamXArmorEvent.Render.Type.NORMAL)))
                    return;
                if (this.rotateAngleX == 0.0F && this.rotateAngleY == 0.0F && this.rotateAngleZ == 0.0F) {
                    if (this.rotationPointX != 0.0F || this.rotationPointY != 0.0F || this.rotationPointZ != 0.0F) {
                        GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
                    }
                } else {
                    GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
                    rotateXYZ(false);
                }
            }
        }
    }

    private void rotateXYZ(boolean yFirst) {
        if (!yFirst) {
            if (this.rotateAngleZ != 0.0F) {
                GlStateManager.rotate(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
            }
        }

        if (this.rotateAngleY != 0.0F) {
            GlStateManager.rotate(this.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
        }

        if (this.rotateAngleX != 0.0F) {
            GlStateManager.rotate(this.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
        }
        if (yFirst) {
            if (this.rotateAngleZ != 0.0F) {
                GlStateManager.rotate(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
            }
        }
    }
}
