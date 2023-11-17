package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;

import java.util.Map;

public interface ILightOwner<T extends ISubInfoTypeOwner<?>> extends ISubInfoTypeOwner<T>
{
    void addLightSource(PartLightSource partLightSource);

    PartLightSource getLightSource(String objectName);

    Map<String, PartLightSource> getLightSources();
}
