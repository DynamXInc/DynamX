package fr.dynamx.utils.debug;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

public class ProfilingData
{
    private final Profiler.Profiles profileIn;

    private long max,lastDelta;
    private long delta;
    private long medium;
    private int measureCount;

    private long startTime;

    public ProfilingData(Profiler.Profiles profileIn) {
        this.profileIn = profileIn;
    }

    public void start() {
        if(startTime != 0)
            throw new IllegalStateException("Profiling of "+profileIn+" is already started !");
        startTime = System.currentTimeMillis();
    }

    public void end() {
        if(startTime == 0)
            throw new IllegalStateException("Profiling of "+profileIn+" is not started !");
        delta += System.currentTimeMillis()-startTime;
        startTime = 0;
    }

    public boolean isEmpty() {
        return medium == 0;
    }

    public void update() {
        if(startTime != 0)
            throw new IllegalStateException("Profiling of "+profileIn+" is started : cannot update it !");
        if(delta > max)
            max = delta;
        medium = (measureCount*medium+delta)/(measureCount+1);
        lastDelta = delta;
        delta = 0;
        measureCount += 1;

        if(measureCount > 100)
            reset();
    }

    public void reset() {
        if(startTime != 0)
            throw new IllegalStateException("Profiling of "+profileIn+" is started : cannot reset it !");
        delta = 0;
        medium = 0;
        measureCount = 0;
        max = 0;
    }

    @Override
    public String toString() {
        return "ProfilingData "+ profileIn.name()+" : average= "+medium+" ms, max= "+max+" ms on "+measureCount+" measures";
    }

    public Measure save() {
        return new Measure(max, medium, lastDelta);
    }

    public static class Measure
    {
        private final long max,lastDelta;
        private final long medium;

        public Measure(long max, long medium, long lastDelta) {
            this.max = max;
            this.medium = medium;
            this.lastDelta = lastDelta;
        }

        @SideOnly(Side.CLIENT)
        public void draw(int x, int bottom, FontRenderer font, int count)
        {
            bottom -= 9;
            font.drawString(max+"", x, bottom, count%2 == 0 ? Color.CYAN.getRGB() : Color.ORANGE.getRGB());
            bottom -= 2;

            bottom -= 9;
            font.drawString(medium+"", x, bottom, count%2 == 0 ? Color.BLUE.getRGB() : Color.RED.getRGB());
            bottom -= 2;
            //drawBar(x, bottom, (int) max, Color.RED.getRGB());
            //bottom -= 120;

            bottom -= 9;
            font.drawString(lastDelta+"", x, bottom, count%2 == 0 ? Color.GREEN.getRGB() : Color.MAGENTA.getRGB());
            bottom -= 2;
            drawBar(x, bottom, (int) lastDelta, count%2 == 0 ? Color.GREEN.getRGB() : Color.MAGENTA.getRGB());
        }

        @SideOnly(Side.CLIENT)
        private void drawBar(int x, int bottom, int height, int color)
        {
            for(int i=0;i<height;i++)
            {
                Gui.drawRect(x, bottom-1, x+10, bottom, color);
                bottom -=2;
            }
        }
    }
}
