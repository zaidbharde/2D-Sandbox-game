package ide;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class OutputPanel extends JPanel {
    private JTextPane outputArea;
    private DefaultStyledDocument doc;
    private JScrollPane scrollPane;
    private JPanel inputPanel;
    private JLabel inputLabel;
    private JTextField inputField;
    private boolean autoScroll = true;
    private boolean acceptingInput = false;
    private java.util.function.Consumer<String> inputHandler;
    
    private static final Color BG_COLOR = new Color(15, 15, 15);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    
    public OutputPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 30, 30)));
        
        setupComponents();
    }
    
    private void setupComponents() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 20, 20));
        header.setPreferredSize(new Dimension(0, 30));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 30, 30)));
        
        JLabel title = new JLabel("  OUTPUT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(TEXT_COLOR);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        buttonPanel.setBackground(new Color(20, 20, 20));
        
        JButton clearBtn = createHeaderButton("X");
        clearBtn.setToolTipText("Clear Output");
        clearBtn.addActionListener(e -> clearOutput());
        
        JButton autoScrollBtn = createHeaderButton("Auto");
        autoScrollBtn.setToolTipText("Auto-scroll");
        autoScrollBtn.setBackground(new Color(50, 50, 50));
        autoScrollBtn.addActionListener(e -> {
            autoScroll = !autoScroll;
            autoScrollBtn.setBackground(autoScroll ? new Color(50, 50, 50) : new Color(35, 35, 35));
        });
        
        buttonPanel.add(clearBtn);
        buttonPanel.add(autoScrollBtn);
        
        header.add(title, BorderLayout.WEST);
        header.add(buttonPanel, BorderLayout.EAST);
        
        doc = new DefaultStyledDocument();
        outputArea = new JTextPane(doc);
        outputArea.setBackground(BG_COLOR);
        outputArea.setForeground(TEXT_COLOR);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        outputArea.setEditable(false);
        
        scrollPane = new JScrollPane(outputArea);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        scrollPane.getVerticalScrollBar().setBackground(new Color(25, 25, 25));
        scrollPane.getHorizontalScrollBar().setBackground(new Color(25, 25, 25));

        inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(BG_COLOR);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 30, 30)),
            BorderFactory.createEmptyBorder(6, 10, 8, 10)
        ));

        inputLabel = new JLabel("INPUT");
        inputLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        inputLabel.setForeground(new Color(255, 213, 79));

        inputField = new JTextField();
        inputField.setBackground(new Color(20, 20, 20));
        inputField.setForeground(TEXT_COLOR);
        inputField.setCaretColor(Color.WHITE);
        inputField.setFont(new Font("Consolas", Font.PLAIN, 12));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 50)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        inputField.setEnabled(false);
        inputField.addActionListener(e -> submitInput());

        inputPanel.add(inputLabel, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }
    
    private JButton createHeaderButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 10));
        button.setForeground(TEXT_COLOR);
        button.setBackground(new Color(35, 35, 35));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(40, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    public void appendOutput(String text, Color color) {
        try {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, color);
            doc.insertString(doc.getLength(), text, attrs);
            if (autoScroll) {
                outputArea.setCaretPosition(doc.getLength());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    public void clearOutput() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    public void attachProcessInput(java.util.function.Consumer<String> handler) {
        inputHandler = handler;
        acceptingInput = handler != null;
        inputField.setEnabled(acceptingInput);
        inputField.setEditable(acceptingInput);
        inputField.setText("");
        inputField.setToolTipText(acceptingInput ? "Type input for the running program" : "No running program");
        if (acceptingInput) {
            SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
        }
    }
    
    public void detachProcessInput() {
        attachProcessInput(null);
    }
    
    private void submitInput() {
        if (!acceptingInput || inputHandler == null) {
            return;
        }
        String value = inputField.getText();
        inputField.setText("");
        appendOutput("> " + value + "\n", new Color(255, 213, 79));
        inputHandler.accept(value);
    }
}
