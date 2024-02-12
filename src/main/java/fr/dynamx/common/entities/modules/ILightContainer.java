package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import dz.betterlights.dynamx.LightCasterPartSync;
import dz.betterlights.lighting.lightcasters.LightCaster;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.lights.SpotLightObject;
import fr.dynamx.common.contentpack.parts.lights.VolumetricLightObject;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.common.objloader.data.GltfModelData;
import fr.dynamx.utils.optimization.Vector3fPool;
import org.joml.Matrix4f;

import java.util.Map;

public interface ILightContainer {

    Map<SpotLightObject, LightCaster> getLightCasters();

    Map<Integer, LightCasterPartSync> getLightCastersSync();

    default void setLightOn(String id, boolean state) {
        setLightOn(id.hashCode(), state);
    }

    void setLightOn(int id, boolean state);

    boolean isLightOn(String id);

    boolean isLightOn(int id);


    default void addLight(DxModelData dxModelData, PartLightSource partMultiLightSource, LightCaster lightCaster, SpotLightObject spotLightObject) {
        Vector3fPool.openPool();
        String partName = partMultiLightSource.getObjectName();
        Vector3f center = new Vector3f();
        center.addLocal(spotLightObject.getOffset());


        Vector3f rotation1 = spotLightObject.getRotation();
        Matrix4f rot = new Matrix4f();
        rot.rotate((float) Math.toRadians(rotation1.y), new org.joml.Vector3f(0, 1, 0));
        rot.rotate((float) Math.toRadians(rotation1.x), new org.joml.Vector3f(1, 0, 0));
        rot.rotate((float) Math.toRadians(rotation1.z), new org.joml.Vector3f(0, 0, 1));

        org.joml.Vector3f f = rot.transformDirection(new org.joml.Vector3f(0, 0, 1));
        Vector3f lightRotation = Vector3fPool.get(f.x, f.y, f.z);

        Vector3f color = spotLightObject.getSpotLightColor();
        if (spotLightObject.getSpotLightColor() == null) {
            color = new Vector3f(1, 1, 1);
        }

        lightCaster
                .setBasePosition(center)
                .setBaseRotation(lightRotation)
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
            lightCaster.getVolumetricFogConfig().setIntensity(volumetricLightObject.getIntensity());
        }

        getLightCasters().put(spotLightObject, lightCaster);
        Vector3fPool.closePool();
    }
}
