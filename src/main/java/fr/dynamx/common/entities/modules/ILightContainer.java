package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import dz.betterlights.dynamx.LightPartGroup;
import dz.betterlights.lighting.lightcasters.LightCaster;
import fr.dynamx.common.contentpack.parts.lights.SpotLightObject;
import fr.dynamx.common.contentpack.parts.lights.VolumetricLightObject;
import fr.dynamx.utils.optimization.Vector3fPool;
import org.joml.Matrix4f;

import java.util.Map;

public interface ILightContainer {

    Map<SpotLightObject, LightCaster> getLightCasters();

    Map<Integer, LightPartGroup> getLightCastersSync();

    default void setLightOn(String id, boolean state) {
        setLightOn(id.hashCode(), state);
    }

    void setLightOn(int id, boolean state);

    boolean isLightOn(String id);

    boolean isLightOn(int id);


    default void createLightCaster(LightCaster lightCaster, SpotLightObject spotLightObject) {
        Vector3fPool.openPool();
        Vector3f offset = new Vector3f();
        offset.addLocal(spotLightObject.getOffset());


        Vector3f baseRotation = spotLightObject.getRotation();
        Matrix4f rot = new Matrix4f();
        rot.rotate((float) Math.toRadians(baseRotation.y), new org.joml.Vector3f(0, 1, 0));
        rot.rotate((float) Math.toRadians(baseRotation.x), new org.joml.Vector3f(1, 0, 0));
        rot.rotate((float) Math.toRadians(baseRotation.z), new org.joml.Vector3f(0, 0, 1));

        org.joml.Vector3f f = rot.transformDirection(new org.joml.Vector3f(0, 0, 1));
        Vector3f lightRotation = Vector3fPool.get(f.x, f.y, f.z);

        Vector3f color = spotLightObject.getSpotLightColor();
        lightCaster.setPersistenceType(LightCaster.EnumPersistenceType.TEMPORARY);

        lightCaster
                .setBasePosition(offset)
                .setBaseRotation(baseRotation)
                .angle(spotLightObject.getInnerAngle(), spotLightObject.getOuterAngle())
                .intensity(spotLightObject.getIntensity())
                .distance(spotLightObject.getDistance())
                .color(color).setEnabled(true);
        lightCaster.getFlareConfig().setEnabled(false);
        lightCaster.getVolumetricFogConfig().setEnabled(!spotLightObject.getVolumetricLightObjects().isEmpty());
        if (!spotLightObject.getVolumetricLightObjects().isEmpty()) {
            VolumetricLightObject volumetricLightObject = spotLightObject.getVolumetricLightObjects().get(0);
            lightCaster.getVolumetricFogConfig().setScattering(volumetricLightObject.getScattering());
            lightCaster.getVolumetricFogConfig().setSampleCount(volumetricLightObject.getSampleCount());
            lightCaster.getVolumetricFogConfig().setDensity(volumetricLightObject.getIntensity());
        }

        getLightCasters().put(spotLightObject, lightCaster);
        Vector3fPool.closePool();
    }
}
