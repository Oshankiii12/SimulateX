package com.sim;

import com.sim.element.CircuitElm;
import java.io.Serializable;
import java.lang.reflect.Constructor;

public class ElementBuilder implements Serializable
{
    /**
     * Create a new CircuitElm by calling its constructor with two int's.
     * This is a utility method to make it easier to create new
     * CircuitElm's, since they usually have the same constructor
     * signature.
     *
     * @param elmClass is the Class of the CircuitElm to create.
     * @param x the first argument to the constructor
     * @param y the second argument to the constructor
     * @return a newly created CircuitElm
     */
    public static CircuitElm build(Class elmClass, int x, int y)
    {
        // The constructor we're looking for takes two int's.  Create
        // Class objects to represent these types.
        Class constructorArgumentClasses[] = new Class[2];
        constructorArgumentClasses[0] = constructorArgumentClasses[1] = int.class;

        // Find the constructor in the class that takes two int's.
        Constructor constructor = null;
        try
        {
            constructor = elmClass.getConstructor(constructorArgumentClasses);
        }
        catch (NoSuchMethodException ee)
        {
            // If the class doesn't have a constructor that takes two
            // int's, then we can't create it.  Print an error message
            // and return null.
            System.out.println("caught NoSuchMethodException " + elmClass);
            return null;
        }
        catch (Exception ee)
        {
            // Catch any other exceptions and print a stack trace.
            // This is unexpected, but it's better than crashing the
            // program.
            ee.printStackTrace();
            return null;
        }

        // Create an array of objects to pass to the constructor.
        // The array must have the same length as the number of
        // arguments in the constructor, and the elements must be
        // of the same type as the constructor's arguments.
        Object arguments[] = new Object[2];
        arguments[0] = new Integer(x);
        arguments[1] = new Integer(y);

        try
        {
            // Call the constructor, passing the arguments we created.
            // The constructor will return a new object of type
            // CircuitElm.
            return (CircuitElm) constructor.newInstance(arguments);
        }
        catch (Exception ee)
        {
            // Catch any exceptions that occur while calling the
            // constructor.  This is unexpected, so print a stack
            // trace and return null.
            ee.printStackTrace();
        }

        // If we get here, then something went wrong.  Return null.
        return null;
    }
}
