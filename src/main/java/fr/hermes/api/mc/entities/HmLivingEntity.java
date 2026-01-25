package fr.hermes.api.mc.entities;

public interface HmLivingEntity extends HmEntity {
    float getLimbSwing();
    void setLimbSwing(float limbSwing);
    float getLimbSwingAmount(); // TODO UNDERSTAND THIS AND FIND A BETTER NAME
    void setLimbSwingAmount(float amount);
    float getPrevLimbSwingAmount();
    void setPrevLimbSwingAmount(float amount);

    float hm$getFallDistance();
}
