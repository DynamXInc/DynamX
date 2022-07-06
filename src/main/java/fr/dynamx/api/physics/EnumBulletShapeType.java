package fr.dynamx.api.physics;

import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.player.PlayerPhysicsHandler;

/**
 * Bullet shape types are used in {@link BulletShapeType} class <br>
 *     They describe the type of a physics object, used for collision handling with players <br>
 *         <ul>
 *             <li>VEHICLE : a vehicle, as {@link BaseVehicleEntity}</li>
 *             <li>TEST : used for testing, has no specific behavior</li>
 *             <li>PLAYER : used for player collision shape, see {@link PlayerPhysicsHandler}</li>
 *             <li>TERRAIN : used for terrain shape, see {@link fr.dynamx.common.physics.terrain.element.CompoundBoxTerrainElement}</li>
 *             <li>SLOPE : used for terrain shape, see {@link fr.dynamx.common.physics.terrain.element.SlopeTerrainElement}</li>
 *             <li>BULLET_ENTITY : used for non-vehicle entities, see {@link AbstractEntityPhysicsHandler}</li>
 *         </ul>
 */
public enum EnumBulletShapeType
{
    /**
     * See EnumBulletShapeType javadoc for info
     */
    VEHICLE, TEST, PLAYER, TERRAIN, SLOPE, BULLET_ENTITY;

    public boolean isTerrain()
    {
        return this == TERRAIN || this == SLOPE;
    }
    public boolean isPlayer()
    {
        return this == PLAYER;
    }
    public boolean isBulletEntity()
    {
        return this == VEHICLE || this == BULLET_ENTITY;
    }
    public boolean isEntity()
    {
        return this == VEHICLE || this == BULLET_ENTITY || this == PLAYER;
    }
}