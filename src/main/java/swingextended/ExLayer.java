package swingextended;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A class that allows the creation of a layer of components, laid out left-to-right, with the components aligned
 * left, right or center within the layer. Layers are meant to be added to a larger container with a vertical layout
 * manager, such as a {@link BoxLayout} with {@link BoxLayout#Y_AXIS} display.
 */
public class ExLayer extends Box {

    // Used to signal alignment of components during instantiation
    public static final int LEFT = 1; /* Signals left alignment of components */
    public static final int RIGHT = 2; /* Signals right alignment of components */
    public static final int CENTER = 3; /* Signals center alignment of components */
    public static final int NONE = 0; /* Signals no alignment; components will fill the layer */

    // Masks used to determine where to place horizontal glue within the layer
    private static final int LEFT_GLUE_MASK = 2, RIGHT_GLUE_MASK = 1;

    // Flags set when glue is placed to the left or to the right of the components for the sake of alignment
    private boolean leftGlue, rightGlue;
    // Flag set after the first non-glue component is added, for space between components
    private boolean notFirst;
    // Defines the amount of space, in pixels, to place between components
    private int componentSpacing;

    /**
     * Creates a new layer with the given alignment and adds all listed components to the layer
     * @param alignment The desired alignment for components within the layer
     * @param components The components to add to the layer
     */
    public ExLayer(int alignment, int componentSpacing, Component ... components) {
        super(BoxLayout.X_AXIS);
        this.componentSpacing = componentSpacing;
        if((alignment & LEFT_GLUE_MASK) > 0) {
            addWithoutSpacing(Box.createHorizontalGlue());
            leftGlue = true;
        }
        // TO-DO: fix this!
        for(Component c : components)
            add(c);
        if((alignment & RIGHT_GLUE_MASK) > 0) {
            addWithoutSpacing(Box.createHorizontalGlue());
            rightGlue = true;
        }
    }



    /**
     * Creates a new, empty later with the given alignment of components
     * @param alignment The desired alignment
     */
    public ExLayer(int alignment, int componentSpacing) {
        super(BoxLayout.X_AXIS);
        this.componentSpacing = componentSpacing;
        if((alignment & LEFT_GLUE_MASK) > 0) {
            addWithoutSpacing(Box.createHorizontalGlue());
            leftGlue = true;
        }
        if((alignment & RIGHT_GLUE_MASK) > 0) {
            addWithoutSpacing(Box.createHorizontalGlue());
            rightGlue = true;
        }
    }

    public Component addWithoutSpacing(Component c) {
        return super.add(c);
    }

    /**
     * Adds a component to the layer. New components are added to the right of existing components
     * @param c
     * @return A reference to the {@link Component} added
     */
    @Override
    public Component add(Component c) {
        int index = getComponentCount();
        if(rightGlue)
            index--;
        if(notFirst) {
            super.add(Box.createHorizontalStrut(componentSpacing), index);
            return super.add(c, index + 1);
        } else {
            notFirst = true;
            return super.add(c, index);
        }
    }
}
