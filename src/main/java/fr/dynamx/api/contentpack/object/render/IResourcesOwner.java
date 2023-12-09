package fr.dynamx.api.contentpack.object.render;

import net.minecraft.item.Item;

import javax.annotation.Nullable;

public interface IResourcesOwner {
    String getJsonName(int meta);

    default int getMaxMeta() {
        return 1;
    }

    default boolean createTranslation() {
        return true;
    }

    default boolean createJson() {
        return getDxModel() == null;
    }

    @Nullable
    IModelPackObject getDxModel();

    default Item getItem() {
        return this instanceof Item ? (Item) this : null;
    }

    static IResourcesOwner of(Item item) {
        return item instanceof IResourcesOwner ? (IResourcesOwner) item : new DummyResourceOwner(item);
    }

    class DummyResourceOwner implements IResourcesOwner {
        private final Item item;

        private DummyResourceOwner(Item item) {
            this.item = item;
        }

        @Override
        public String getJsonName(int meta) {
            return item.getTranslationKey().replace("item.dynamxmod.", "");
        }

        @Override
        public boolean createTranslation() {
            return false;
        }

        @Override
        public boolean createJson() {
            return false;
        }

        @Override
        public IModelPackObject getDxModel() {
            return null;
        }

        @Override
        public Item getItem() {
            return item;
        }
    }
}
