package fr.hermes.api.mc.entities;

import fr.hermes.api.mc.blocks.HmTileEntity;

public interface HmBlockEntityFactory {
    HmBlockEntityLogic createEntityLogic(HmTileEntity blockEntity);
}