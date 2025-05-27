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

    /**
     * Registers an element class so that it can be instantiated from a dump string.
     * @param elmClass the class of the element to register
     */
    public void register(Class elmClass)
    {
        // Create a temporary element to obtain its dump type
        CircuitElm elm = ElementBuilder.build(elmClass, 0, 0);

        int dumpType = elm.getDumpType();
        if (dumpType == 0)
        {
            // The element didn't set a dump type, so we can't register it
            System.out.println("no dump type: " + elmClass);
            return;
        }

        // Get the class that is used to create the element from a dump string
        Class dumpClass = elm.getDumpClass();

        // Check if we've already registered an element with the same dump type
        if (dumpTypes[dumpType] == dumpClass)
        {
            // If so, then we don't need to do anything else
            return;
        }

        // Check if we've already registered a different element with the same dump type
        if (dumpTypes[dumpType] != null)
        {
            // If so, then there's a conflict, so we print a message and do nothing else
            System.out.println("dump type conflict: " + elmClass + " " + dumpTypes[dumpType]);
            return;
        }

        // If no element with the same dump type has been registered, then register this one
        dumpTypes[dumpType] = dumpClass;
    }

    private void registerAllElements()
    {
        // Register the element classes with the registry.

        // First, register the Wire element class.
        register(WireElm.class);

        // Next, register the Resistor element class.
        register(ResistorElm.class);

        /**
         * Register the passive components.
         * Passive components are components that don't amplify a signal.
         * They just modify the signal in some way.
         * Examples of passive components are switches, resistors, and capacitors.
         */
        register(SwitchElm.class);
        register(MemristorElm.class);
        register(MRAMElm.class);

        /**
         * Register the input/output components.
         * Input/output components are components that are used to input or
         * output signals.
         * Examples of input/output components are ground, voltage sources, and LEDs.
         */
        register(GroundElm.class);
        register(DCVoltageElm.class);
        register(ACVoltageElm.class);
        register(LEDElm.class);

        /**
         * Register the active components.
         * Active components are components that can amplify a signal.
         * Examples of active components are diodes, transistors, and capacitors.
         */
        register(DiodeElm.class);
        register(NMosfetElm.class);
        register(PMosfetElm.class);
    }

    private void reserveScopeCharacters()
    {
        // Reserve certain characters so that they can be used by the Scope
        // class to dump its state.
        //
        // The characters we're reserving are:
        //  - 'o' (the default scope character)
        //  - 'h' (the default scope character for a horizontal scope)
        //  - '$' (the character for a scope that displays its voltage)
        //  - '%' (the character for a scope that displays its current)
        //  - '?' (the character for a scope that displays its power)
        //  - 'B' (the character for a scope that displays its energy)
        //
        // What we're doing here is assigning the Scope class to the dumpTypes
        // array for each of these characters. This will ensure that the Scope
        // class is used when reading a dump string that starts with one of these
        // characters.
        dumpTypes[(int) 'o'] = Scope.class;
        dumpTypes[(int) 'h'] = Scope.class;
        dumpTypes[(int) '$'] = Scope.class;
        dumpTypes[(int) '%'] = Scope.class;
        dumpTypes[(int) '?'] = Scope.class;
        dumpTypes[(int) 'B'] = Scope.class;
    }
}
