package fr.hermes.api.mc;

import net.minecraft.entity.Entity;

public interface HmEntity {
    Entity toEntity();

    int getEntityId();

    HmServerWorld getWorld();
}
