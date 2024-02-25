package fr.dynamx.common.contentpack.parts;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dz.betterlights.BetterLightsMod;
import dz.betterlights.dynamx.LightPartGroup;
import dz.betterlights.handlers.IShaderUniformsHandler;
import dz.betterlights.lighting.lightcasters.EntityLightCaster;
import dz.betterlights.lighting.lightcasters.LightCaster;
import dz.betterlights.util.BetterLightsConstants;
import dz.betterlights.util.OptifineUniformsUtil;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.bb.OBBModelBone;
import fr.dynamx.bb.OBBModelBox;
import fr.dynamx.bb.OBBPlayerManager;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.lights.SpotLightObject;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.items.DynamXItem;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

/**
 * Contains multiple {@link LightObject}
 */
@Getter
@RegisteredSubInfoType(name = "MultiLight", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS, SubInfoTypeRegistries.ITEMS, SubInfoTypeRegistries.ARMORS}, strictName = false)
public class PartLightSource extends SubInfoType<ILightOwner<?>> implements ISubInfoTypeOwner<PartLightSource>, IDrawablePart<Object, IModelPackObject>, IModelTextureVariantsSupplier.IModelTextureVariants {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    private final String partName;

    private final List<LightObject> sources = new ArrayList<>();
    @Getter
    private final List<SpotLightObject> spotLights = new ArrayList<>();

    @PackFileProperty(configNames = "ObjectName")
    protected String objectName;
    @PackFileProperty(configNames = "BaseMaterial", required = false)
    protected String baseMaterial = "default";
    /**
     * The position of this part, relative to the 3D model. <br>
     * If null, it will be read from the 3D model (if possible, see readPositionFromModel method).
     */
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false, defaultValue = "From model")
    protected Vector3f position;
    /**
     * The rotation of this part, relative to the 3D model. <br>
     * If null, it will be read from the 3D model (if possible, see readPositionFromModel method).
     */
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "From model")
    protected Quaternion rotation;
    /**
     * Indicates if the position and rotation were read from the 3D model, or set by the user. <br>
     * Changes the behavior of the rendering.
     */
    protected boolean isAutomaticPosition;
    @PackFileProperty(configNames = "DependsOnNode", required = false, description = "PartLightSource.DependsOnNode")
    protected String nodeDependingOnName;

    public PartLightSource(ISubInfoTypeOwner<ILightOwner<?>> owner, String partName) {
        super(owner);
        this.partName = partName;
    }

    /**
     * If this is a rotating light, this method reads the position and rotation from the 3D model owning this part. <br>
     * If the configured position is null, this method reads the position. <br>
     * If the configured position and rotation are null, this method also reads the rotation (only for GLTF models). <br>
     * <br>
     * If this isn't a rotating light, we don't need to do any transform to render it, so we don't need its position and rotation.
     *
     * @param model The 3D model owning this part
     */
    public void readPositionFromModel(ResourceLocation model) {
        if (getPosition() == null) {
            DxModelData modelData = DynamXContext.getDxModelDataFromCache(DynamXUtils.getModelPath(getPackName(), model));
            if (modelData != null) {
                position = DynamXUtils.readPartPosition(modelData, getObjectName(), true);
                if (getRotation() == null && position != null)
                    rotation = DynamXUtils.readPartRotation(modelData, getObjectName());
            }
            if (getPosition() == null) {
                DynamXErrorManager.addPackError(getPackName(), "position_not_found_in_model", ErrorLevel.HIGH, owner.getName(), "3D object " + getObjectName() + " for part " + getName());
            } else {
                isAutomaticPosition = true;
            }
        }
    }

    @Override
    public void appendTo(ILightOwner<?> owner) {
        if (owner instanceof AbstractItemObject)
            readPositionFromModel(((AbstractItemObject) owner).getModel());
        if (position == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "required_property", ErrorLevel.HIGH, parent.getName(), "Position in " + getName());
            position = new Vector3f();
        } else {
            if (owner instanceof ICollisionsContainer)
                position.multLocal(((ICollisionsContainer) owner).getScaleModifier());
            else if (owner instanceof AbstractItemObject)
                position.multLocal(((AbstractItemObject) owner).getItemScale());
        }
        owner.addLightSource(this);
    }

    @Nullable
    @Override
    public ILightOwner<?> getOwner() {
        return owner;
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(AbstractLightsModule.class)) {
            if (entity instanceof TrailerEntity)
                modules.add(new AbstractLightsModule.TrailerLightsModule(getOwner(), entity));
            else
                modules.add(new AbstractLightsModule.EntityLightsModule(entity, getOwner()));
        }
    }

    @Override
    public String getName() {
        return "PartLightSource with name " + getPartName();
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public void addToSceneGraph(IModelPackObject packInfo, SceneBuilder<IRenderContext, IModelPackObject> sceneBuilder) {
        if (nodeDependingOnName != null) {
            sceneBuilder.addNode(packInfo, this, nodeDependingOnName);
        } else {
            sceneBuilder.addNode(packInfo, this);
        }
    }

    @Override
    public SceneNode<IRenderContext, IModelPackObject> createSceneGraph(Vector3f modelScale, List<SceneNode<IRenderContext, IModelPackObject>> childGraph) {
        return new PartLightNode<>(this, modelScale, (List) childGraph);
    }

    private final Map<Byte, TextureVariantData> variantsMap = new HashMap<>();

    @Override
    public TextureVariantData getDefaultVariant() {
        return variantsMap.get((byte) 0);
    }

    @Override
    public TextureVariantData getVariant(byte variantId) {
        return variantsMap.getOrDefault(variantId, getDefaultVariant());
    }

    @Override
    public Map<Byte, TextureVariantData> getTextureVariants() {
        return variantsMap;
    }

    /**
     * Post loads this light (computes texture variants)
     */
    public void postLoad() {
        Map<String, TextureVariantData> nameToVariant = new HashMap<>();
        byte nextTextureId = 0;
        TextureVariantData data = new TextureVariantData(baseMaterial, nextTextureId);
        variantsMap.put(data.getId(), data);
        nameToVariant.put(baseMaterial, data);

        List<LightObject> sources = getSources();
        for (LightObject source : sources) {
            if (source.getTextures() != null) {
                source.getBlinkTextures().clear();
                for (int j = 0; j < source.getTextures().length; j++) {
                    String name = source.getTextures()[j];
                    if (nameToVariant.containsKey(name)) {
                        source.getBlinkTextures().add(nameToVariant.get(name));
                    } else {
                        data = new TextureVariantData(name, ++nextTextureId);
                        source.getBlinkTextures().add(data);
                        variantsMap.put(data.getId(), data);
                        nameToVariant.put(name, data);
                    }
                }
            }
        }
    }

    public void addLightSource(LightObject object) {
        sources.add(object);
    }

    public void addSpotLight(SpotLightObject object) {
        spotLights.add(object);
    }

    @Override
    public void addSubProperty(ISubInfoType<PartLightSource> property) {
        throw new IllegalStateException("Cannot add sub property to a light");
    }

    @Override
    public List<ISubInfoType<PartLightSource>> getSubProperties() {
        return Collections.emptyList();
    }

    class PartLightNode<A extends IModelPackObject> extends SimpleNode<IRenderContext, A> {
        public PartLightNode(PartLightSource lightSource, Vector3f scale, List<SceneNode<IRenderContext, A>> linkedChilds) {
            super(lightSource.getPosition(), lightSource.getRotation() != null ? GlQuaternionPool.newGlQuaternion(lightSource.getRotation()) : null, PartLightSource.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void render(IRenderContext context, A packInfo) {
            /* Rendering light sources */
            boolean isEntity = context instanceof BaseRenderContext.EntityRenderContext && ((BaseRenderContext.EntityRenderContext) context).getEntity() != null;
            boolean isItem = context instanceof BaseRenderContext.ItemRenderContext && ((BaseRenderContext.ItemRenderContext) context).getStack() != null;
            boolean isBlock = context instanceof BaseRenderContext.BlockRenderContext && ((BaseRenderContext.BlockRenderContext) context).getTileEntity() != null;
            boolean isArmor = context instanceof BaseRenderContext.ArmorRenderContext && ((BaseRenderContext.ArmorRenderContext) context).getArmorModel() != null;
            AbstractLightsModule lights = null;
            if (isEntity)
                lights = ((BaseRenderContext.EntityRenderContext) context).getEntity().getModuleByType(AbstractLightsModule.class);
            else if (isBlock)
                lights = ((BaseRenderContext.BlockRenderContext) context).getTileEntity().getLightsModule();
            else if (isItem)
                lights = DynamXItem.getLightContainer(((BaseRenderContext.ItemRenderContext) context).getStack());
            else if (isArmor) {
                BaseRenderContext.ArmorRenderContext armorRenderContext = (BaseRenderContext.ArmorRenderContext) context;
                ItemStack stack = armorRenderContext.getEntity().getItemStackFromSlot(armorRenderContext.getEquipmentSlot());
                lights = DynamXItem.getLightContainer(stack);
            }
            transformToRotationPoint();
            /* Rendering light source */
            LightObject onLightObject = null;
            if (lights != null) {
                lights.setLightOn(10, true);
                // Find the first light object that is on
                for (LightObject source : getSources()) {
                    if (lights.isLightOn(source.getLightId())) {
                        onLightObject = source;
                        break;
                    }
                }
            }
            boolean isOn = true;
            if (onLightObject == null) {
                isOn = false;
                onLightObject = getSources().get(0);
            }
            // Do blinking
            int activeStep = 0;
            if (isOn && onLightObject.getBlinkSequence() != null) {
                int[] seq = onLightObject.getBlinkSequence();
                int mod = ClientEventHandler.MC.getRenderViewEntity().ticksExisted % seq[seq.length - 1];
                isOn = false; //Default state
                for (int i = seq.length - 1; i >= 0; i--) {
                    if (mod > seq[i]) {
                        isOn = i % 2 == 0;
                        activeStep = (byte) (i + 1);
                        break;
                    }
                }
            }
            //for testing only isOn = onLightObject != null;
            byte texId = 0;
            if (isOn && !onLightObject.getBlinkTextures().isEmpty()) {
                activeStep = activeStep % onLightObject.getBlinkTextures().size();
                texId = onLightObject.getBlinkTextures().get(activeStep).getId();
            }
            Vector3fPool.openPool();
            //Set luminescent

            if (isOn) {

                if (lights.getLightCastersSync().containsKey(onLightObject.getLightId())) {
                    LightPartGroup partSync = lights.getLightCastersSync().get(onLightObject.getLightId());
                    for (Map.Entry<String, LightCaster> entry : partSync.getLightCasters().entrySet()) {

                        if (entry.getKey() != null && entry.getKey().equals(getObjectName())) {
                            if(isItem) {
                                renderLightFirstPerson(entry.getValue(), transform, (BaseRenderContext) context);
                            }else
                                renderLight(entry.getValue(), transform);
                        }
                    }
                }

                int i = 15728880;
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
            Vector3fPool.closePool();

            //Render the light
            if (lights != null && lights.isLightOn(onLightObject.getLightId()) && onLightObject.getRotateDuration() > 0) {
                float step = ((float) (ClientEventHandler.MC.getRenderViewEntity().ticksExisted % onLightObject.getRotateDuration())) / onLightObject.getRotateDuration();
                step = step * (FastMath.PI * 2);
                transform.rotate(step, 0, 1, 0);
            }
            GlStateManager.pushMatrix();
            GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
            transformToPartPos();
            context.getModel().renderGroup(getObjectName(), texId, context.isUseVanillaRender());
            if (isEntity && isOn) {
                int i = ((BaseRenderContext.EntityRenderContext) context).getEntity().getBrightnessForRender();
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
            GlStateManager.popMatrix();
            renderChildren(context, packInfo);
        }

        @Override
        public void renderDebug(IRenderContext context, A packInfo) {
            if (DynamXDebugOptions.LIGHTS.isActive()) {
                GlStateManager.pushMatrix();
                transformForDebug();
                RenderGlobal.drawBoundingBox(-0.05f, -0.05f, -0.05f, 0.05f, 0.05f, 0.05f,
                        1, 1, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(context, packInfo);
        }
    }

    public static void renderLight(LightCaster lightCaster, Matrix4f transform) {
        Matrix4f transformCopy = new Matrix4f(transform);

        org.joml.Vector3f center = DynamXUtils.toVector3f(lightCaster.getBasePosition());
        transformCopy.transformPosition(center);

        Vector3f baseRotation = lightCaster.getBaseRotation();
        transformCopy.rotate((float) Math.toRadians(baseRotation.y), new org.joml.Vector3f(0, 1, 0));
        transformCopy.rotate((float) Math.toRadians(baseRotation.x), new org.joml.Vector3f(1, 0, 0));
        transformCopy.rotate((float) Math.toRadians(baseRotation.z), new org.joml.Vector3f(0, 0, 1));
        org.joml.Vector3f direction = new org.joml.Vector3f(0,0,1);
        transformCopy.transformDirection(direction);


        EntityPlayerSP player = Minecraft.getMinecraft().player;
        //interpolate
        float x = (float) (player.prevPosX + (float) (player.posX - player.prevPosX) * Minecraft.getMinecraft().getRenderPartialTicks());
        float y = (float) (player.prevPosY + (float) (player.posY - player.prevPosY) * Minecraft.getMinecraft().getRenderPartialTicks());
        float z = (float) (player.prevPosZ + (float) (player.posZ - player.prevPosZ) * Minecraft.getMinecraft().getRenderPartialTicks());

        // System.out.println(lightCaster +" " + center);
        lightCaster
                .pos(new Vector3f(center.x, center.y, center.z).add(x, y, z))
                .direction(new Vector3f(direction.x, direction.y, direction.z))
                .setEnabled(true);
    }
    private static final Matrix4f itemModelViewMatrix = new Matrix4f();
    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public static void renderLightFirstPerson(LightCaster lightCaster, Matrix4f transform, BaseRenderContext context){
        if(OptifineUniformsUtil.isOptifineShadowPass()){
            return;
        }
        float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
        EntityPlayerSP playerSp = Minecraft.getMinecraft().player;
        if (!(lightCaster instanceof EntityLightCaster)) {
            return;
        }
        UUID entityId = ((EntityLightCaster) lightCaster).getEntityId();
        Entity entityHolder = playerSp.world.loadedEntityList.stream().filter(e -> e.getPersistentID().equals(entityId)).findFirst().orElse(null);
        if(!(entityHolder instanceof EntityPlayer)){
            return;
        }
        EntityPlayer playerHolder = (EntityPlayer) entityHolder;
        if (!(playerHolder.getHeldItemMainhand().getItem() instanceof DynamXItem)) {
            return;
        }
        Vector3fPool.openPool();

        float x = (float) (playerSp.lastTickPosX + (playerSp.posX - playerSp.lastTickPosX) * partialTicks);
        float y = (float) (playerSp.lastTickPosY + (playerSp.posY - playerSp.lastTickPosY) * partialTicks);
        float z = (float) (playerSp.lastTickPosZ + (playerSp.posZ - playerSp.lastTickPosZ) * partialTicks);

        GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, (FloatBuffer) matrixBuffer.position(0));

        itemModelViewMatrix.set((FloatBuffer) matrixBuffer.position(0));
        Matrix4f modelViewInverse = new Matrix4f(IShaderUniformsHandler.modelViewInverse);

        Vector3f itemPosition = DynamXUtils.toVector3f(itemModelViewMatrix.transformPosition(DynamXUtils.toVector3f(lightCaster.getBasePosition())));
        Vector4f positionPlayerSpace = modelViewInverse.transform(new Vector4f(itemPosition.x, itemPosition.y, itemPosition.z, 1));

        Vector3f itemPositionWorld = Vector3fPool.get(positionPlayerSpace.x, positionPlayerSpace.y, positionPlayerSpace.z).addLocal(x, y, z);
        if(context instanceof BaseRenderContext.ItemRenderContext) {
            BaseRenderContext.ItemRenderContext itemRenderContext = (BaseRenderContext.ItemRenderContext) context;
            if (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0 && itemRenderContext.getRenderType().equals(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND)) {
                updateLightThirdPerson(playerHolder, lightCaster, itemPositionWorld, itemRenderContext);
            }
            if (!(playerHolder instanceof EntityOtherPlayerMP) && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && itemRenderContext.getRenderType().equals(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND)) {
                org.joml.Vector3f playerDir = DynamXUtils.toVector3fJoml(playerSp.getLook(partialTicks));
                itemModelViewMatrix.transformDirection(playerDir);

                //org.joml.Vector3f dir = new org.joml.Vector3f(0,0,1);
                //itemModelViewMatrix.transformDirection(dir);
                //Vector3f forwardVector = Vector3fPool.get(itemModelViewMatrix.m20(), itemModelViewMatrix.m21(), itemModelViewMatrix.m22());
                lightCaster
                        .pos(itemPositionWorld)
                        .direction(Vector3fPool.get(-playerDir.x, playerDir.y, -playerDir.z).normalize());
            }
        }
        Vector3fPool.closePool();
    }

    private static void updateLightThirdPerson(EntityPlayer player, LightCaster lightCaster, Vector3f itemPosition, BaseRenderContext.ItemRenderContext renderType){
        OBBPlayerManager.PlayerOBBModelObject playerOBBObject = OBBPlayerManager.getPlayerOBBObject(player.getName());
        Optional<Map.Entry<OBBModelBox, OBBModelBone>> rightArm = playerOBBObject.boneBinding.entrySet().stream().filter(entry -> entry.getKey().name.contains("rightArm")).findFirst();
        Map.Entry<OBBModelBox, OBBModelBone> rightArmEntry = rightArm.get();
        org.lwjgl.util.vector.Matrix4f handRot = rightArmEntry.getValue().currentPose;
        Vector3f forwardVector = Vector3fPool.get(-handRot.m20, -handRot.m21, -handRot.m22);
        lightCaster
                .pos(itemPosition)
                .direction(forwardVector.normalize());
    }

    private static void updateLightArmor(EntityPlayer player, LightCaster lightCaster, Vector3f itemPosition, ItemStack stack, BaseRenderContext.ArmorRenderContext context){
        String armorType = context.getEquipmentSlot().getName();
        switch (context.getEquipmentSlot()) {
            case HEAD:
                break;
            case CHEST:
                armorType = "body";
                break;
            case LEGS:
                break;
            case FEET:
                break;
        }

    }
}
