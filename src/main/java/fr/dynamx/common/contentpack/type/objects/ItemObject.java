package fr.dynamx.common.contentpack.type.objects;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.api.events.client.BuildSceneGraphEvent;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.scene.node.ItemNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.items.DynamXItem;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class ItemObject<T extends ItemObject<?>> extends AbstractItemObject<T, T> {
    @Getter
    @Setter
    @PackFileProperty(configNames = "MaxItemStackSize", required = false, defaultValue = "1")
    protected int maxItemStackSize = 1;

    protected SceneNode<?, ?> sceneNode;

    public ItemObject(String packName, String fileName) {
        super(packName, fileName);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected IDynamXItem<T> createItem(InfoList<T> loader) {
        CreatePackItemEvent.SimpleItem<T, ?> event = new CreatePackItemEvent.SimpleItem(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return event.getObjectItem();
        } else {
            return new DynamXItem(this);
        }
    }

    @Override
    public String toString() {
        return "ItemObject named " + getFullName();
    }

    @Nullable
    @Override
    public IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
        // variants not supported on items
        return null;
    }

    @Override
    public SceneNode<?, ?> getSceneGraph() {
        if (sceneNode == null) {
            if (isModelValid()) {
                BuildSceneGraphEvent.BuildItemScene event = new BuildSceneGraphEvent.BuildItemScene(this, (List) getDrawableParts());
                MinecraftForge.EVENT_BUS.post(event);
                sceneNode = event.getSceneGraphResult();
            } else
                sceneNode = new ItemNode<>(Collections.EMPTY_LIST);
        }
        return sceneNode;
    }
}
