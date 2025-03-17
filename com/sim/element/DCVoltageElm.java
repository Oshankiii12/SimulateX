package com.sim.element;

public class DCVoltageElm extends VoltageElm
{
    public static final String NAME = "Voltage Source (2-terminal)";

    public DCVoltageElm(int xx, int yy)
    {
        super(xx, yy, WF_DC);
    }

    public Class getDumpClass()
    {
        return VoltageElm.class;
    }
}
