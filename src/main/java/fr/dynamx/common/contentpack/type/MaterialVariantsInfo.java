package fr.dynamx.common.contentpack.type;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@RegisteredSubInfoType(name = "MaterialVariants", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.WHEELS, SubInfoTypeRegistries.ARMORS, SubInfoTypeRegistries.BLOCKS_AND_PROPS})
public class MaterialVariantsInfo<T extends ISubInfoTypeOwner<T>> extends SubInfoType<T> implements IModelTextureVariantsSupplier.IModelTextureVariants {
    @PackFileProperty(configNames = "BaseMaterial", required = false)
    private String baseMaterial = "Default";
    @PackFileProperty(configNames = "Variants", defaultValue = "\"DynamX1 DynamX2\"")
    private String[] texturesArray;
    /* todo use @Getter
    @PackFileProperty(configNames = "ItemIcons", required = false, defaultValue = "\"Default DynamX1 DynamX2\"")
    private String[] itemIcons;*/
    @Getter
    private final Map<Byte, TextureVariantData> variantsMap = new HashMap<>();

    public MaterialVariantsInfo(ISubInfoTypeOwner<T> owner) {
        super(owner);
    }

    /**
     * Backward compatibility
     */
    @Deprecated
    public MaterialVariantsInfo(ISubInfoTypeOwner<T> owner, String[][] texturesArray) {
        super(owner);
        this.texturesArray = new String[texturesArray.length];
        for (int i = 0; i < texturesArray.length; i++) {
            String[] info = texturesArray[i];
            this.texturesArray[i] = info[0];
        }
    }

    @Override
    public String getName() {
        return "MaterialVariantsInfo";
    }

    @Override
    public void appendTo(T owner) {
        variantsMap.put((byte) 0, new TextureVariantData(baseMaterial, (byte) 0));
        byte id = 1;
        for (String info : texturesArray) {
            TextureVariantData variant = new TextureVariantData(info, id);
            variantsMap.put(id, variant);
            id++;
        }
        owner.addSubProperty(this);
    }

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
}
