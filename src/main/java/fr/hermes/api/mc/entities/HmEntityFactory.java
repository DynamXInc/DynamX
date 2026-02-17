package fr.hermes.api.mc.entities;

import fr.hermes.api.mc.world.HmWorld;

public interface HmEntityFactory<TLogic extends HmEntityLogic> {
    TLogic createEntityLogic(HmWorld world, HmEntity entity);
}