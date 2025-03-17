package com.sim.element;

public class ACVoltageElm extends VoltageElm
{
    public static final String NAME = "A/C Source (2-terminal)";

    public ACVoltageElm(int xx, int yy)
    {
        super(xx, yy, WF_AC);
    }

    public Class getDumpClass()
    {
        return VoltageElm.class;
    }
}
