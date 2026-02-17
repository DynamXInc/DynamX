package fr.hermes.api.mc.entities;

public interface HmModEntity<TLogic extends HmEntityLogic> {
    TLogic getLogic();
}
