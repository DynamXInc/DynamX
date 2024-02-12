package fr.dynamx.common.contentpack.parts.lights;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class LightSourceFile extends SubInfoTypeOwner<LightSourceFile> implements ILightOwner<LightSourceFile> {

    private final String packName;
    private final String partName;

    /**
     * The light sources of this block
     */
    @Getter
    protected final Map<String, PartLightSource> lightSources = new HashMap<>();

    public LightSourceFile(String packName, String partName) {
        this.packName = packName;
        this.partName = partName;
    }



    @Override
    public String getName() {
        return partName;
    }

    @Override
    public String getPackName() {
        return packName;
    }


    public <A extends ISubInfoTypeOwner<?>> void appendTo(AbstractItemObject<?, ?> owner) {
        owner.getLightSources().putAll(lightSources);
    }

    @Override
    public void addLightSource(PartLightSource source) {
        lightSources.put(source.getPartName(), source);
    }

    @Override
    public PartLightSource getLightSource(String partName) {
        return lightSources.get(partName);
    }
}
