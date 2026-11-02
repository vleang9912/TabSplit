import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * Represents an abstract parent class for tabs in the TabSplit application.
 * Provides a common structure for creating tabs with a title, a back button,
 * and a standardized layout. Subclasses put their components in {@link #content}.
 */
public abstract class ParentTab extends JPanel {

    static final Color BACKGROUND = new Color(242, 242, 247);
    static final Color CARD = Color.WHITE;
    static final Color CARD_BORDER = new Color(220, 220, 225);
    static final Color ACCENT = new Color(0, 122, 255);
    static final Color ERROR = new Color(200, 30, 30);
    static final Color MUTED = new Color(110, 110, 115);

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
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        // Header: back button on the left, title in the middle
        backButton = new JButton("‹ Menu");
        backButton.addActionListener(e -> onBackButtonClick.onBackButtonClick());
        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));
        header.add(backButton, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        // Same width as the back button on the right, so the title stays centered
        header.add(Box.createHorizontalStrut(backButton.getPreferredSize().width), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Area for the tab's own components
        content = new JPanel();
        content.setOpaque(false);
        add(content, BorderLayout.CENTER);
    }

    /**
     * Called every time the tab is shown, so it can refresh anything that may have changed on
     * another tab (e.g. new friends). Does nothing unless a subclass overrides it.
     */
    public void onShow() {
    }

    /**
     * Provides access to the back button for external use.
     *
     * @return The back button instance.
     */
    public JButton getBackButton() {
        return backButton;
    }

    /**
     * Wraps a component in a white card with a small heading above it.
     *
     * @param heading   The heading text.
     * @param component The component to put in the card.
     * @return The card.
     */
    static JPanel card(String heading, JComponent component) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(new LineBorder(CARD_BORDER, 1, true), new EmptyBorder(10, 12, 12, 12)));
        if (heading != null) {
            JLabel label = new JLabel(heading.toUpperCase());
            label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
            label.setForeground(MUTED);
            card.add(label, BorderLayout.NORTH);
        }
        card.add(component, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates a list without its own border or background, for use inside a card.
     *
     * @param model     The items to show.
     * @param emptyText A grey hint shown while the list is empty, or {@code null} for none.
     * @return The list, inside a borderless scroll pane.
     */
    static JScrollPane plainList(ListModel<String> model, String emptyText) {
        JList<String> list = new JList<>(model) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Show a hint in grey when there's nothing in the list yet
                if (getModel().getSize() == 0 && emptyText != null) {
                    g.setColor(MUTED);
                    g.setFont(getFont());
                    g.drawString(emptyText, 2, g.getFontMetrics().getAscent() + 2);
                }
            }
        };
        list.setFocusable(false);
        list.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                // Read-only list: ignore selection
            }
        });
        list.setFixedCellHeight(list.getFontMetrics(list.getFont()).getHeight() + 6);
        list.setBackground(CARD);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD);
        return scroll;
    }

    /**
     * Creates a red label for error messages. It starts empty but keeps its height, so the layout
     * doesn't jump when a message appears.
     *
     * @return The label.
     */
    static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(ERROR);
        return label;
    }
}
