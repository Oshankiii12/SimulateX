package com.sim.element;

import com.sim.view.edit.EditInfo;

import java.awt.*;
import java.util.StringTokenizer;

public class DiodeElm extends CircuitElm
{
    public static final String NAME = "Diode";

    public final Diode diode;
    public static final int FLAG_FWDROP = 1;
    public final double defaultdrop = .805904783;
    public double fwdrop, zvoltage;

    public DiodeElm(int xx, int yy)
    {
        super(xx, yy);
        diode = new Diode(sim);
        fwdrop = defaultdrop;
        zvoltage = 0;
        setup();
    }

    public DiodeElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st)
    {
        super(xa, ya, xb, yb, f);
        diode = new Diode(sim);
        fwdrop = defaultdrop;
        zvoltage = 0;
        if ((f & FLAG_FWDROP) > 0)
        {
            try
            {
                fwdrop = Double.parseDouble(st.nextToken());
            } catch (Exception e)
            {
            }
        }
        setup();
    }

    public boolean nonLinear()
    {
        return true;
    }

    public void setup()
    {
        diode.setup(fwdrop, zvoltage);
    }

    public int getDumpType()
    {
        return DUMP_TYPE;
    }

    public static final int DUMP_TYPE = 'd';


    public String dump()
    {
        flags |= FLAG_FWDROP;
        return super.dump() + " " + fwdrop;
    }

    public final int hs = 8;
    public Polygon poly;
    public Point cathode[];

    public void setPoints()
    {
        super.setPoints();
        calcLeads(16);
        cathode = newPointArray(2);
        Point pa[] = newPointArray(2);
        interpPoint2(lead1, lead2, pa[0], pa[1], 0, hs);
        interpPoint2(lead1, lead2, cathode[0], cathode[1], 1, hs);
        poly = createPolygon(pa[0], pa[1], lead2);
    }

    public void draw(Graphics g)
    {
        drawDiode(g);
        doDots(g);
        drawPosts(g);
    }

    public void reset()
    {
        diode.reset();
        volts[0] = volts[1] = curcount = 0;
    }

    public void drawDiode(Graphics g)
    {
        setBbox(point1, point2, hs);

        double v1 = volts[0];
        double v2 = volts[1];

        draw2Leads(g);

        // draw arrow thingy
        setPowerColor(g, true);
        setVoltageColor(g, v1);
        g.fillPolygon(poly);

        // draw thing arrow is pointing to
        setVoltageColor(g, v2);
        drawThickLine(g, cathode[0], cathode[1]);
    }

    public void stamp()
    {
        diode.stamp(nodes[0], nodes[1]);
    }

    public void doStep()
    {
        diode.doStep(volts[0] - volts[1]);
    }

    public void calculateCurrent()
    {
        current = diode.calculateCurrent(volts[0] - volts[1]);
    }

    public void getInfo(String arr[])
    {
        arr[0] = "diode";
        arr[1] = "I = " + getCurrentText(getCurrent());
        arr[2] = "Vd = " + getVoltageText(getVoltageDiff());
        arr[3] = "P = " + getUnitText(getPower(), "W");
        arr[4] = "Vf = " + getVoltageText(fwdrop);
    }

    public EditInfo getEditInfo(int n)
    {
        if (n == 0)
            return new EditInfo("Fwd Voltage @ 1A", fwdrop, 10, 1000);
        return null;
    }

    public void setEditValue(int n, EditInfo ei)
    {
        fwdrop = ei.value;
        setup();
    }

    public boolean hasHotkey()
    {
        return getClass() == DiodeElm.class;
    }
}
