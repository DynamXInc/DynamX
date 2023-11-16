package fr.dynamx.common.contentpack.type.vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine contained in an engine file
 */
public class BoatEngineInfo extends BaseEngineInfo {
    public List<GearInfo> gears = new ArrayList<>();

    byte i = 0;

    public BoatEngineInfo(String packName, String name) {
        super(packName, name);
    }

    @Override
    public void addGear(GearInfo gear) {
        gear.setId(i);
        gears.add(i, gear);
        i++;
    }
}

