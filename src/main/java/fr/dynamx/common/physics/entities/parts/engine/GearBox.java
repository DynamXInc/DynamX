package fr.dynamx.common.physics.entities.parts.engine;

import fr.hermes.forge1122.dynamx.DynamXConfig;
import fr.dynamx.utils.maths.DynamXMath;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class GearBox {
    /**
     * -1 : reverse
     * 0 : neutral
     * 1,2,3,4,5,6 : other speeds
     */
    private int activeGear;
    private final GearData[] gears;
    private int gearChangeCounter;

    public GearBox(int gearCount) {
        this.gears = new GearData[gearCount];

        for (int i = 0; i < gearCount; i++) {
            this.gears[i] = new GearData(0, 0, 0, 0);
        }
    }

    public void setGear(int gearNum, float start, float end, float rpmStart, float rpmEnd) {

        GearData gear = this.gears[gearNum];

        gear.setStart(start);
        gear.setEnd(end);
        gear.setRpmStart(rpmStart);
        gear.setRpmEnd(rpmEnd);
    }

    public GearData getActiveGear() {
        return this.gears[activeGear + 1];
    }

    public boolean increaseGear() {
        if (getActiveGearNum() >= 1 && getActiveGearNum() + 2 < getGearCount()) //on est en marche avant
        {
            setActiveGearNum(getActiveGearNum() + 1);
            return true;
        }
        return false;
    }

    public boolean decreaseGear() {
        if (getActiveGearNum() >= 1) //on est en marche avant
        {
            setActiveGearNum(getActiveGearNum() - 1);
            return true;
        } else if (getActiveGearNum() < 0) {
            setActiveGearNum(getActiveGearNum() + 1);
            return true;
        }
        return false;
    }


    /**
     * -1 : reverse
     * 0 : neutral
     * 1,2,3,4,5,6 : other speeds
     */
    public int getActiveGearNum() {
        return activeGear;
    }

    /**
     * -1 : reverse
     * 0 : neutral
     * 1,2,3,4,5,6 : other speeds
     */
    public void setActiveGearNum(int activeGear) {
        if (activeGear != 0)
            gearChangeCounter = getGearChangeTime();
        else
            gearChangeCounter = 0;
        this.activeGear = activeGear;
    }

    public void syncActiveGearNum(int activeGear) {
        this.activeGear = activeGear;
    }

    public int getGearCount() {
        return this.gears.length;
    }

    public int updateGearChangeCounter() {
        if (gearChangeCounter > 0)
            gearChangeCounter--;
        return gearChangeCounter;
    }

    public int getGearChangeTime() {
        return getActiveGearNum() == 0 ? 0 : DynamXConfig.gearChangeDelay;
    }

    public float getRPM(Engine engine, float speed) {
        GearData gear = getActiveGear();
        float revs = DynamXMath.normalize(speed, gear.getStart(), gear.getEnd());
        revs *= (gear.getRpmEnd() - gear.getRpmStart()) / engine.getMaxRevs();
        revs += gear.getRpmStart() / engine.getMaxRevs(); //on ajoute les tours moteurs minimaux (irl si on tombe dessous on cale donc avec une boite auto pas possible)
        return revs;
    }

    @AllArgsConstructor
    public static class GearData{
        @Getter
        @Setter
        private float start, end;
        @Getter
        @Setter
        private float rpmStart, rpmEnd;
    }
}
