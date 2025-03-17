package com.sim.model;

import com.sim.ElementBuilder;
import com.sim.element.*;
import com.sim.view.Scope;

import java.awt.*;

public class ElementDumpTypesRegistry
{
    public Class dumpTypes[];

    public ElementDumpTypesRegistry()
    {
        dumpTypes = new Class[300];

        reserveScopeCharacters();

        registerAllElements();
    }

    public void register(Class elmClass)
    {
        CircuitElm elm = ElementBuilder.build(elmClass, 0, 0);

        int dumpType = elm.getDumpType();
        if (dumpType == 0)
        {
            System.out.println("no dump type: " + elmClass);
            return;
        }
        Class dumpClass = elm.getDumpClass();
        if (dumpTypes[dumpType] == dumpClass)
            return;
        if (dumpTypes[dumpType] != null)
        {
            System.out.println("dump type conflict: " + elmClass + " " + dumpTypes[dumpType]);
            return;
        }
        dumpTypes[dumpType] = dumpClass;
    }

    private void registerAllElements()
    {
        register(WireElm.class);
        register(ResistorElm.class);

        /** Passive Components */
        register(SwitchElm.class);
        register(MemristorElm.class);

        /** Inputs/Outputs */
        register(GroundElm.class);
        register(DCVoltageElm.class);
        register(ACVoltageElm.class);
        register(LEDElm.class);

        /** Active Components */
        register(DiodeElm.class);
        register(NMosfetElm.class);
        register(PMosfetElm.class);

    }

    private void reserveScopeCharacters()
    {
        dumpTypes[(int) 'o'] = Scope.class;
        dumpTypes[(int) 'h'] = Scope.class;
        dumpTypes[(int) '$'] = Scope.class;
        dumpTypes[(int) '%'] = Scope.class;
        dumpTypes[(int) '?'] = Scope.class;
        dumpTypes[(int) 'B'] = Scope.class;
    }
}
