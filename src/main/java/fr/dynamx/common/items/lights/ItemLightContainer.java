package fr.dynamx.common.items.lights;

import dz.betterlights.dynamx.LightCasterPartSync;
import dz.betterlights.lighting.lightcasters.LightCaster;
import dz.betterlights.network.EnumPacketType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.lights.SpotLightObject;
import fr.dynamx.common.entities.modules.ILightContainer;
import fr.dynamx.common.network.lights.PacketSyncPartLights;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class ItemLightContainer implements ILightContainer {

    @Getter
    private final Map<Integer, LightCasterPartSync> lightCasterPartSyncs = new HashMap<>();
    public final Map<SpotLightObject, LightCaster> lightCasters = new HashMap<>();

    @Override
    public Map<SpotLightObject, LightCaster> getLightCasters() {
        return lightCasters;
    }

    @Override
    public Map<Integer, LightCasterPartSync> getLightCastersSync() {
        return lightCasterPartSyncs;
    }

    @Override
    public void setLightOn(int id, boolean state) {
        if (lightCasterPartSyncs.containsKey(id)) {
            lightCasterPartSyncs.get(id).isEnabled = state;
            DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new PacketSyncPartLights(lightCasterPartSyncs.get(id), EnumPacketType.UPDATE), EnumPacketTarget.ALL, null);
        }
    }

    @Override
    public boolean isLightOn(String id) {
        return isLightOn(id.hashCode());
    }

    @Override
    public boolean isLightOn(int id) {
        return lightCasterPartSyncs.containsKey(id) && lightCasterPartSyncs.get(id).isEnabled;
    }

}
