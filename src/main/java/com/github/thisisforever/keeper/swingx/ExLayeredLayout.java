package com.github.thisisforever.keeper.swingx;

import javax.swing.*;

/**
 * A layout that will display components added from top to bottom
 */
public class ExLayeredLayout extends BoxLayout {

    /**
     * Creates a new {@link ExLayeredLayout} object
     * @param owner A reference to the {@link JFrame} which this layout will manage
     */
    public ExLayeredLayout(JFrame owner) {
        // Lay layers vertically, top to bottom
        super(owner.getContentPane(), BoxLayout.Y_AXIS);
    }

}
