package swingextended;

import javax.swing.*;

public class ExLayeredLayout extends BoxLayout {

    public ExLayeredLayout(JFrame owner) {
        // Lay layers vertically, top to bottom
        super(owner.getContentPane(), BoxLayout.Y_AXIS);
    }

}
