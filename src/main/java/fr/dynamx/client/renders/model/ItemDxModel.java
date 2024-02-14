package fr.dynamx.client.renders.model;

import com.google.common.collect.ImmutableList;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.client.renders.model.renderer.DxItemModelLoader;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.common.ProgressManager;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ItemDxModel implements IModel {
    private final ResourceLocation location;
    private IModelPackObject owner;

    private IModel gui;
    private IBakedModel guiBaked;

    public ItemDxModel(ResourceLocation location, IModelPackObject owner) throws Exception {
        this.location = location;
        this.owner = owner;
        if (owner.get3DItemRenderLocation() != Enum3DRenderLocation.ALL) {
            gui = ModelLoaderRegistry.getModel(new ResourceLocation(location.getNamespace(), "item/" + location.getPath().replace(".obj", "")));
        }
    }

    public void setOwner(IModelPackObject owner) {
        this.owner = owner;
    }

    public IModelPackObject getOwner() {
        return owner;
    }

    public IBakedModel getGuiBaked() {
        return guiBaked;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return gui == null ? ImmutableList.of() : gui.getTextures();
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return gui == null ? ImmutableList.of() : gui.getDependencies();
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        if (gui != null) {
            ProgressManager.ProgressBar bar = ProgressManager.push("Loading item model " + location, 1);
            bar.step("Baking gui model");
            guiBaked = gui.bake(state, format, bakedTextureGetter);
            ProgressManager.pop(bar);
        }
        return new IBakedModel() {
            private final ItemOverrideList overrideList = new ItemOverrideList(Collections.EMPTY_LIST);

            @Override
            public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
                return Collections.EMPTY_LIST;
            }

            @Override
            public boolean isAmbientOcclusion() {
                return false;
            }

            @Override
            public boolean isGui3d() {
                return true;
            }

            @Override
            public boolean isBuiltInRenderer() {
                return true;
            }

            @Override
            public TextureAtlasSprite getParticleTexture() {
                return null;
            }

            @Override
            public ItemOverrideList getOverrides() {
                return overrideList;
            }

            @Override
            public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
                DxItemModelLoader.renderType = cameraTransformType;
                return ForgeHooksClient.handlePerspective(this, cameraTransformType);
            }
        };
    }
}
