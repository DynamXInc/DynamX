package fr.hermes.api.mc.entities;

import fr.hermes.api.mc.world.HmWorld;

public interface HmEntityFactory {
    HmEntityLogic createEntityLogic(HmWorld world, HmEntity entity);
}