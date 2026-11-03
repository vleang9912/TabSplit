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
    static final Color ERROR = new Color(200, 30, 30); // also used for "owes"
    static final Color OWED = new Color(30, 135, 60);
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

    /** A button that looks like a blue text link, like Venmo's "Clear" */
    static JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setForeground(ACCENT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * One line of a list with links on the right, ie "Vin Leang     Rename  Remove", long text
     * wraps onto more lines
     * @return the row
     */
    static JPanel actionRow(String text, JComponent... links) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override
            public Dimension getMaximumSize() {
                // full width but only as tall as it needs, so rows stack at the top
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(3, 4, 3, 4));
        JTextArea area = wrappingText(text);
        // nudge the text down so its first line lines up with the links
        if (links.length > 0) {
            int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
            area.setBorder(new EmptyBorder(Math.max(0, (links[0].getPreferredSize().height - lineHeight) / 2), 0, 0, 0));
        }
        // how tall wrapped text is depends on its width, so lay the list out again once the width is known
        area.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (area.getPreferredSize().height != area.getHeight()) {
                    row.revalidate();
                }
            }
        });
        row.add(area, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        for (JComponent link : links) {
            right.add(link);
        }
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // read-only text that looks like a label but wraps at word breaks to fit its width
    private static JTextArea wrappingText(String text) {
        JTextArea area = new JTextArea();
        // its hidden cursor would scroll the list to wherever the text ends, so it never moves
        ((javax.swing.text.DefaultCaret) area.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        area.setText(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(UIManager.getFont("Label.font"));
        area.setForeground(UIManager.getColor("Label.foreground"));
        // start narrow so it takes the row's width instead of asking for one long line
        area.setColumns(1);
        // a first guess at the width, so it isn't one word per line before the real layout
        area.setSize(300, Short.MAX_VALUE);
        return area;
    }

    /**
     * A panel for actionRows that's always as wide as the card, so long rows wrap instead of
     * pushing their links out of view
     * @return the empty panel
     */
    static JPanel rowPanel() {
        return new RowPanel();
    }

    private static class RowPanel extends JPanel implements Scrollable {
        public boolean getScrollableTracksViewportWidth() { return true; } // never scroll sideways
        public boolean getScrollableTracksViewportHeight() { return false; }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 12; }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return r.height; }
    }

    /**
     * Turns rows into a scrolling top to bottom list for inside a card
     * @return the rows in a borderless scroll pane
     */
    static JScrollPane rowList(JPanel rows) {
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(CARD);
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    /** A grey line of text for an empty list, ie "No expenses yet." */
    static JLabel emptyHint(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(3, 4, 3, 4));
        return label;
    }
}
