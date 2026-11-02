import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * Represents an abstract parent class for tabs in the TabSplit application.
 * Provides a common structure for creating tabs with a title, a back button,
 * and a standardized layout. Subclasses put their components in {@link #content}.
 */
public abstract class ParentTab extends JPanel {

    private static final int MENU_WIDTH = 525;
    private static final int MENU_HEIGHT = 725;
    private static final int CANVAS_WIDTH = 600;
    private static final int CANVAS_HEIGHT = 800;
    private static final int BORDER_WIDTH = (CANVAS_WIDTH - MENU_WIDTH) / 2;
    private static final int BORDER_HEIGHT = (CANVAS_HEIGHT - MENU_HEIGHT) / 2;

    protected JButton backButton;
    protected JPanel content;

    /**
     * Constructs a ParentTab object with a title and a back button.
     *
     * @param titleText         The title text to display at the top of the tab.
     * @param onBackButtonClick A functional interface to handle back button clicks.
     */
    public ParentTab(String titleText, BackButtonHandler onBackButtonClick) {

        // Background for the tab
        setLayout(new BorderLayout());
        setBackground(Color.DARK_GRAY);
        setBorder(new EmptyBorder(BORDER_HEIGHT, BORDER_WIDTH, BORDER_HEIGHT, BORDER_WIDTH));

        // Content box within the tab
        JPanel contentBox = new JPanel(new BorderLayout(0, 20));
        contentBox.setBackground(Color.WHITE);
        contentBox.setBorder(new CompoundBorder(new LineBorder(Color.BLACK, 3), new EmptyBorder(20, 20, 20, 20)));
        add(contentBox, BorderLayout.CENTER);

        // Title text
        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        contentBox.add(title, BorderLayout.NORTH);

        // Area for the tab's own components
        content = new JPanel();
        content.setOpaque(false);
        contentBox.add(content, BorderLayout.CENTER);

        // Back button
        backButton = new JButton("back");
        backButton.addActionListener(e -> onBackButtonClick.onBackButtonClick());
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.add(backButton);
        contentBox.add(footer, BorderLayout.SOUTH);
    }

    /**
     * Provides access to the back button for external use.
     *
     * @return The back button instance.
     */
    public JButton getBackButton() {
        return backButton;
    }
}
