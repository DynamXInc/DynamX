package fr.dynamx.client.renders.model;

import fr.dynamx.api.events.ArmorEvent;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.client.DynamXModelRegistry;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ArmorRenderer extends ModelRenderer {
    private final ModelObjArmor model;
    private final ObjModelClient objModel;
    private IObjObject objObject;

    public ArmorRenderer(ObjModelClient objModel, ModelObjArmor model, String partName) {
        super(model, partName);
        this.model = model;
        this.objModel = objModel;
        if (objModel != DynamXModelRegistry.MISSING_MODEL) {
            for (IObjObject objObject1 : objModel.objObjects) {
                if (objObject1.getName().equalsIgnoreCase(partName)) {
                    objObject = objObject1;
                    break;
                }
            }
        }
        if (objObject == null) {
            objObject = MissingObjModel.getEmptyPart();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void render(float scale) {
        if (!this.isHidden) {
            if (this.showModel) {
                if (MinecraftForge.EVENT_BUS.post(new ArmorEvent.Render(model, objModel, objObject, PhysicsEntityEvent.Phase.PRE, ArmorEvent.Render.Type.NORMAL)))
                    return;
                GlStateManager.pushMatrix();
                GlStateManager.translate(this.rotationPointX, this.rotationPointY, this.rotationPointZ);
                GlStateManager.rotate(180, 1, 0, 0);

                if (this.rotateAngleZ != 0.0F) {
                    GlStateManager.rotate(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
                }

                if (this.rotateAngleY != 0.0F) {
                    GlStateManager.rotate(this.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                }

                if (this.rotateAngleX != 0.0F) {
                    GlStateManager.rotate(this.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
                }
                GlStateManager.translate(-this.offsetX, this.offsetY, -this.offsetZ);
                objModel.renderGroup(objObject, model.getActiveTextureId());

                GlStateManager.popMatrix();
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderWithRotation(float scale) {
        if (!this.isHidden) {
            if (this.showModel) {
                if (MinecraftForge.EVENT_BUS.post(new ArmorEvent.Render(model, objModel, objObject, PhysicsEntityEvent.Phase.PRE, ArmorEvent.Render.Type.WITH_ROTATION)))
                    return;
                GlStateManager.pushMatrix();
                GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);

                if (this.rotateAngleY != 0.0F) {
                    GlStateManager.rotate(this.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                }

                if (this.rotateAngleX != 0.0F) {
                    GlStateManager.rotate(this.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
                }

                if (this.rotateAngleZ != 0.0F) {
                    GlStateManager.rotate(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
                }

                objModel.renderGroup(objObject, model.getActiveTextureId());
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
                if (MinecraftForge.EVENT_BUS.post(new ArmorEvent.Render(model, objModel, objObject, PhysicsEntityEvent.Phase.POST, ArmorEvent.Render.Type.NORMAL)))
                    return;
                if (this.rotateAngleX == 0.0F && this.rotateAngleY == 0.0F && this.rotateAngleZ == 0.0F) {
                    if (this.rotationPointX != 0.0F || this.rotationPointY != 0.0F || this.rotationPointZ != 0.0F) {
                        GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
                    }
                } else {
                    GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);

                    if (this.rotateAngleZ != 0.0F) {
                        GlStateManager.rotate(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
                    }

                    if (this.rotateAngleY != 0.0F) {
                        GlStateManager.rotate(this.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                    }

                    if (this.rotateAngleX != 0.0F) {
                        GlStateManager.rotate(this.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
                    }
                }
            }
        }
    }
}
