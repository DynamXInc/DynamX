package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;

import javax.annotation.Nullable;
import java.util.List;


@Getter
@Setter
@RegisteredSubInfoType(name = "handle", registries = {SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartHandle extends BasePart<ModularVehicleInfo> implements IDrawablePart<BaseVehicleEntity<?>, ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    @PackFileProperty(configNames = "ObjectName")
    protected String objectName = "handle";

    public PartHandle(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        readPositionFromModel(owner.getModel(), getObjectName(), false, false);
        super.appendTo(owner);
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.HANDLES;
    }

    @Override
    public String getName() {
        return "PartHandle named " + getPartName();
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo> createSceneGraph(Vector3f modelScale, List<SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo>> childGraph) {
        return new PartHandleNode<>(this, modelScale, childGraph);
    }

    class PartHandleNode<T extends BaseVehicleEntity<?>, A extends ModularVehicleInfo> extends SceneGraph.Node<T, A> {
        public PartHandleNode(PartHandle part, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(part.getPosition(), null, PartHandle.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (!context.getModel().containsObjectOrNode(getObjectName()))
                return;
            GlStateManager.pushMatrix();
            transformToRotationPoint();
            if (entity != null && entity.hasModuleOfType(HelicopterEngineModule.class)) {
                HelicopterEngineModule engine = entity.getModuleByType(HelicopterEngineModule.class);
                // Rotating the handle with Dx and Dy
                float dx = engine.getRollControls().get(0);
                float dy = engine.getRollControls().get(1);
                GlStateManager.rotate(dx, 0, 0, dx > 0 ? 0.5f : -0.5f);
                GlStateManager.rotate(dy, dy > 0 ? 0.5f : -0.5f, 0, 0);
            }
            context.getModel().renderGroup(getObjectName(), context.getTextureId(), context.isUseVanillaRender());
            renderChildren(entity, context, packInfo);
            GlStateManager.popMatrix();
        }
    }
}
