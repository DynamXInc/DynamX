package fr.dynamx.common.physics.entities.parts.engine;

public class Gear {

    private float start, end;
    private float rpmStart, rpmEnd;

    public Gear(float start, float end, float rpmStart, float rpmEnd) {
        this.start = start;
        this.end = end;
        this.rpmStart = rpmStart;
        this.rpmEnd = rpmEnd;
    }

    public float getStart() {
        return start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public float getEnd() {
        return end;
    }

    public void setEnd(float end) {
        this.end = end;
    }

    public float getRpmStart() {
        return rpmStart;
    }

    public float getRpmEnd() {
        return rpmEnd;
    }

    public void setRpmStart(float rpmStart) {
        this.rpmStart = rpmStart;
    }

    public void setRpmEnd(float rpmEnd) {
        this.rpmEnd = rpmEnd;
    }
}
