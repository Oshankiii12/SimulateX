package com.sim.view.menu;

import com.sim.CirSim;
import com.sim.ElementBuilder;
import com.sim.element.*;

import java.awt.*;

public class RMBMenuBuilder
{
    private CirSim sim;
    private String ctrlMetaKey;

    public RMBMenuBuilder(CirSim sim)
    {
        this.sim = sim;
        ctrlMetaKey = "Ctrl";
    }

    public PopupMenu build()
    {
        PopupMenu popupMenu = new PopupMenu();

        popupMenu.add(getClassCheckItem(WireElm.class));
        popupMenu.add(getClassCheckItem(ResistorElm.class));

        Menu passMenu = new Menu("Passive Components");
        popupMenu.add(passMenu);
        passMenu.add(getClassCheckItem(SwitchElm.class));
        passMenu.add(getClassCheckItem(MemristorElm.class));
        passMenu.add(getClassCheckItem(MRAMElm.class));

        Menu inputMenu = new Menu("Inputs/Outputs");
        popupMenu.add(inputMenu);
        inputMenu.add(getClassCheckItem(GroundElm.class));
        inputMenu.add(getClassCheckItem(DCVoltageElm.class));
        inputMenu.add(getClassCheckItem(ACVoltageElm.class));
        inputMenu.add(getClassCheckItem(LEDElm.class));

        Menu activeMenu = new Menu("Active Components");
        popupMenu.add(activeMenu);
        activeMenu.add(getClassCheckItem(DiodeElm.class));
//        activeMenu.add(getClassCheckItem(NMosfetElm.class));
//        activeMenu.add(getClassCheckItem(PMosfetElm.class));

        Menu otherMenu = new Menu("Other");
        otherMenu.add(getCheckItem("Drag All (Alt-drag)", "DragAll"));
        otherMenu.add(getCheckItem("Drag Row (S-right)", "DragRow"));
        otherMenu.add(getCheckItem("Drag Column (C-right)", "DragColumn"));
        otherMenu.add(getCheckItem("Drag Selected", "DragSelected"));
        otherMenu.add(getCheckItem("Drag Post (" + ctrlMetaKey + "-drag)", "DragPost"));

        popupMenu.add(getCheckItem("Select/Drag Selected (space or Shift-drag)", "Select"));

        return popupMenu;
    }

    public CheckboxMenuItem getClassCheckItem(Class elmClass)
    {
        CircuitElm elm = ElementBuilder.build(elmClass, 0, 0);

        String text = defineText(elm, elmClass);

        String classSignature = elmClass.getName();
        return getCheckItem(text, classSignature);
    }

    private String defineText(CircuitElm elm, Class elmClass)
    {
        String text = "Add ";
        try
        {
            text += (String)elmClass.getDeclaredField("NAME").get(null);
        }
        catch (NoSuchFieldException ee)
        {
            text = "Unnamed";
        }
        catch (IllegalAccessException ee)
        {
            text = "Access Error";
        }
        try
        {
            if (elm.hasHotkey() && elm.getDumpClass() == elmClass)
            {
                return text + " (" + (char) elm.getDumpType() + ")";
            }
            elm.delete();
        } catch (Exception ee)
        {
            ee.printStackTrace();
        }
        return text;
    }

    public CheckboxMenuItem getCheckItem(String label, String type)
    {
        CheckboxMenuItem menuItem = new CheckboxMenuItem(label);
        menuItem.addItemListener(sim);
        menuItem.setActionCommand(type);
        return menuItem;
    }
}
