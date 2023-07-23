package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.SoftbodyPhysicsHandler;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SoftbodyEntity extends ModularPhysicsEntity<SoftbodyPhysicsHandler<?>>{

    public SoftbodyEntity(World world) {
        super(world);
    }

    public SoftbodyEntity(World world, Vector3f pos, float spawnRotationAngle) {
        super(world, pos, spawnRotationAngle);
    }

    @Override
    protected SoftbodyPhysicsHandler<?> createPhysicsHandler() {
        return new SoftbodyPhysicsHandler<>(this);
    }

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        //Return empty list
        return Collections.emptyList();
    }

    @Override
    protected void createModules(ModuleListBuilder modules) {

    }

    @Override
    protected void fireCreateModulesEvent(Side side) {

    }

    @Override
    public int getSyncTickRate() {
        return DynamXConfig.propsSyncTickRate;
    }
}
