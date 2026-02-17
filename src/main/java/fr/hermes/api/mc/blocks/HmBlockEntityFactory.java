package fr.hermes.api.mc.blocks;

public interface HmBlockEntityFactory {
    HmBlockEntityLogic createEntityLogic(HmBlockEntity blockEntity);
}