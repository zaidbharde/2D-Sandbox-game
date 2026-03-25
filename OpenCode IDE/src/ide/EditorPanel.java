package ide;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorPanel extends JPanel {
    private OpenCodeIDE parent;
    private JTabbedPane tabbedPane;
    private JPanel emptyStatePanel;
    private CardLayout cardLayout;
    private Map<File, CodeEditor> editors;
    private java.util.List<File> openFiles;
    private float fontSize;

    private static final Color BG_COLOR = new Color(20, 20, 20);
    private static final Color TAB_BG = new Color(25, 25, 25);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);

    public EditorPanel(OpenCodeIDE parent) {
        this.parent = parent;
        this.editors = new HashMap<>();
        this.openFiles = new ArrayList<>();
        this.fontSize = 14f;

        cardLayout = new CardLayout();
        setLayout(cardLayout);
        setBackground(BG_COLOR);

        setupTabbedPane();
        setupEmptyState();
        showEmptyState();
    }

    private void setupTabbedPane() {
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(4, 8, 0, 8);
                contentBorderInsets = new Insets(1, 0, 0, 0);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                g.setColor(new Color(45, 45, 45));
                int y = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                g.drawLine(0, y, getWidth(), y);
            }
        });
        tabbedPane.setBackground(TAB_BG);
        tabbedPane.setForeground(TEXT_COLOR);
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabbedPane.setBorder(null);
        tabbedPane.setFocusable(false);
        tabbedPane.setOpaque(true);

        tabbedPane.addChangeListener(e -> {
            updateStatusBar();
            CodeEditor editor = getCurrentEditor();
            if (editor != null) {
                editor.focusTextArea();
            }
        });

        add(tabbedPane, "editor");
    }

    private void setupEmptyState() {
        emptyStatePanel = new JPanel(new GridBagLayout());
        emptyStatePanel.setBackground(BG_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel welcomeLabel = new JLabel("OpenCode IDE");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        welcomeLabel.setForeground(new Color(80, 80, 80));
        emptyStatePanel.add(welcomeLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(10, 0, 0, 0);
        JLabel hintLabel = new JLabel("Open a folder to get started");
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        hintLabel.setForeground(new Color(60, 60, 60));
        emptyStatePanel.add(hintLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(40, 0, 0, 0);

        JPanel shortcutsPanel = new JPanel(new GridLayout(0, 2, 30, 12));
        shortcutsPanel.setBackground(BG_COLOR);

        String[][] shortcuts = {
            {"Ctrl + O", "Open Folder"},
            {"Ctrl + N", "New File"},
            {"Ctrl + S", "Save"},
            {"F5", "Run"},
            {"Ctrl + B", "Toggle Sidebar"},
            {"Ctrl + J", "Toggle Terminal"},
        };

        for (String[] shortcut : shortcuts) {
            JLabel keyLabel = new JLabel(shortcut[0]);
            keyLabel.setFont(new Font("Consolas", Font.BOLD, 12));
            keyLabel.setForeground(new Color(100, 100, 100));

            JLabel actionLabel = new JLabel(shortcut[1]);
            actionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            actionLabel.setForeground(new Color(80, 80, 80));

            shortcutsPanel.add(keyLabel);
            shortcutsPanel.add(actionLabel);
        }

        emptyStatePanel.add(shortcutsPanel, gbc);
        add(emptyStatePanel, "empty");
    }

    public void openFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }

        if (editors.containsKey(file)) {
            for (int i = 0; i < openFiles.size(); i++) {
                if (Objects.equals(openFiles.get(i), file)) {
                    tabbedPane.setSelectedIndex(i);
                    return;
                }
            }
        }

        try {
            openEditor(createEditor(readFile(file), file), file.getName(), file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage());
        }
    }

    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public void createNewFile() {
        String name = JOptionPane.showInputDialog(this, "Enter file name:", "New File", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        File baseDir = parent.getSuggestedDirectoryForNewFile();
        if (baseDir == null) {
            openEditor(createEditor("", null), name.trim(), "Unsaved file");
            return;
        }

        File file = new File(baseDir, name.trim());
        try {
            ensureParentDirectoryExists(file);
            if (file.exists() || file.createNewFile()) {
                openFile(file);
                parent.refreshProjectTree();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    public void saveCurrentFile() {
        CodeEditor editor = getCurrentEditor();
        if (editor == null) {
            return;
        }

        File file = editor.getFile();
        if (file == null) {
            saveAsCurrentFile();
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(editor.getText());
            editor.setModified(false);
            refreshCurrentTabTitle();
            parent.getStatusBar().setStatus("Saved: " + file.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    public void saveAsCurrentFile() {
        CodeEditor editor = getCurrentEditor();
        if (editor == null) {
            return;
        }

        File oldFile = editor.getFile();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save As");

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        editor.setFile(file);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            ensureParentDirectoryExists(file);
            writer.write(editor.getText());

            int index = tabbedPane.getSelectedIndex();
            if (index >= 0 && index < openFiles.size()) {
                openFiles.set(index, file);
            }
            if (oldFile != null) {
                editors.remove(oldFile);
            }
            editors.put(file, editor);

            editor.setModified(false);
            refreshCurrentTabTitle();
            parent.getStatusBar().setStatus("Saved: " + file.getName());
            parent.refreshProjectTree();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    public void closeTab(int index) {
        if (index < 0 || index >= openFiles.size()) {
            return;
        }

        Component component = tabbedPane.getComponentAt(index);
        if (!(component instanceof CodeEditor editor)) {
            return;
        }

        File file = openFiles.get(index);
        String fileName = file != null ? file.getName() : editor.getDisplayName();

        if (editor.isModified()) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Save changes to " + fileName + "?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION
            );
            if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (result == JOptionPane.YES_OPTION) {
                tabbedPane.setSelectedIndex(index);
                saveCurrentFile();
            }
        }

        editors.remove(file);
        openFiles.remove(index);
        tabbedPane.remove(index);

        if (tabbedPane.getTabCount() == 0) {
            showEmptyState();
        }
    }

    private void removeEmptyState() {
        cardLayout.show(this, "editor");
    }

    private void showEmptyState() {
        cardLayout.show(this, "empty");
    }

    private CodeEditor getCurrentEditor() {
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0 && index < tabbedPane.getTabCount()) {
            Component comp = tabbedPane.getComponentAt(index);
            if (comp instanceof CodeEditor editor) {
                return editor;
            }
        }
        return null;
    }

    public File getCurrentFile() {
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0 && index < openFiles.size()) {
            return openFiles.get(index);
        }
        return null;
    }

    public boolean hasOpenFile() {
        return tabbedPane.getTabCount() > 0;
    }

    public void refreshCurrentTabTitle() {
        int index = tabbedPane.getSelectedIndex();
        CodeEditor editor = getCurrentEditor();
        if (index >= 0 && editor != null) {
            String title = editor.getDisplayName();
            File file = editor.getFile();
            tabbedPane.setTitleAt(index, title);
            tabbedPane.setToolTipTextAt(index, file != null ? file.getAbsolutePath() : title);
            tabbedPane.setTabComponentAt(index, new TabHeader(title, editor));
        }
    }

    private void refreshCurrentTabTitleIfSelected(CodeEditor editor) {
        if (editor == getCurrentEditor()) {
            refreshCurrentTabTitle();
        }
    }

    private void ensureParentDirectoryExists(File file) throws IOException {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Could not create folder: " + parentDir.getAbsolutePath());
        }
    }

    private void openEditor(CodeEditor editor, String title, String tooltip) {
        editor.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshCurrentTabTitleIfSelected(editor);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshCurrentTabTitleIfSelected(editor);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshCurrentTabTitleIfSelected(editor);
            }
        });

        File file = editor.getFile();
        if (file != null) {
            editors.put(file, editor);
        }
        openFiles.add(file);

        int index = tabbedPane.getTabCount();
        tabbedPane.addTab(title, editor);
        tabbedPane.setToolTipTextAt(index, tooltip);
        tabbedPane.setTabComponentAt(index, new TabHeader(title, editor));
        tabbedPane.setSelectedIndex(index);

        removeEmptyState();
        SwingUtilities.invokeLater(editor::focusTextArea);
    }

    private CodeEditor createEditor(String content, File file) {
        return new CodeEditor(content, file);
    }

    public void undo() {
        CodeEditor editor = getCurrentEditor();
        if (editor != null) editor.undo();
    }

    public void redo() {
        CodeEditor editor = getCurrentEditor();
        if (editor != null) editor.redo();
    }

    public void cut() {
        CodeEditor editor = getCurrentEditor();
        if (editor != null) editor.cut();
    }

    public void copy() {
        CodeEditor editor = getCurrentEditor();
        if (editor != null) editor.copy();
    }

    public void paste() {
        CodeEditor editor = getCurrentEditor();
        if (editor != null) editor.paste();
    }

    public void selectAll() {
        CodeEditor editor = getCurrentEditor();
        if (editor != null) editor.selectAll();
    }

    public void zoomIn() {
        fontSize += 2;
        applyFontSize();
    }

    public void zoomOut() {
        if (fontSize > 8) {
            fontSize -= 2;
            applyFontSize();
        }
    }

    public void resetZoom() {
        fontSize = 14;
        applyFontSize();
    }

    private void applyFontSize() {
        for (CodeEditor editor : editors.values()) {
            editor.setFontSize(fontSize);
        }
        CodeEditor editor = getCurrentEditor();
        if (editor != null) {
            editor.focusTextArea();
        }
    }

    private void updateStatusBar() {
        CodeEditor editor = getCurrentEditor();
        if (editor != null && editor.getFile() != null) {
            parent.getStatusBar().setFile(editor.getFile().getName());
        }
    }

    private class TabHeader extends JPanel {
        TabHeader(String title, CodeEditor editor) {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 2));

            JLabel label = new JLabel(title);
            label.setForeground(TEXT_COLOR);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            JButton close = new JButton("\u00D7");
            close.setBorderPainted(false);
            close.setFocusPainted(false);
            close.setContentAreaFilled(false);
            close.setForeground(new Color(160, 160, 160));
            close.setCursor(new Cursor(Cursor.HAND_CURSOR));
            close.setMargin(new Insets(0, 2, 0, 2));
            close.addActionListener(e -> closeTab(tabbedPane.indexOfComponent(editor)));

            add(label);
            add(close);
        }
    }
}

class CodeEditor extends JPanel {
    private JTextPane textArea;
    private LineNumbers lineNumbers;
    private JScrollPane scrollPane;
    private File file;
    private boolean modified;
    private UndoManager undoManager;
    private StyledDocument document;
    private boolean applyingHighlight;

    private static final Color BG_COLOR = new Color(20, 20, 20);
    private static final Color TEXT_COLOR = new Color(212, 212, 212);
    private static final Color KEYWORD_COLOR = new Color(86, 156, 214);
    private static final Color STRING_COLOR = new Color(206, 145, 120);
    private static final Color COMMENT_COLOR = new Color(106, 153, 85);
    private static final Color NUMBER_COLOR = new Color(181, 206, 168);
    private static final Color TYPE_COLOR = new Color(78, 201, 176);
    private static final Set<String> KEYWORDS = new HashSet<>(List.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
        "volatile", "while", "true", "false", "null"
    ));

    public CodeEditor(String content, File file) {
        this.file = file;
        this.modified = false;

        setLayout(new BorderLayout());
        setBackground(BG_COLOR);

        textArea = new JTextPane();
        textArea.setBackground(BG_COLOR);
        textArea.setForeground(TEXT_COLOR);
        textArea.setCaretColor(Color.WHITE);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 8));
        textArea.setSelectionColor(new Color(43, 87, 151));
        textArea.setSelectedTextColor(Color.WHITE);
        textArea.setDragEnabled(false);
        textArea.getCaret().setBlinkRate(500);
        textArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        document = textArea.getStyledDocument();
        textArea.setText(content);

        undoManager = new UndoManager();
        document.addUndoableEditListener(e -> {
            undoManager.undoableEditHappened(e);
            modified = true;
            highlightSyntaxLater();
        });

        lineNumbers = new LineNumbers(textArea);

        scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumbers);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setBackground(new Color(30, 30, 30));
        scrollPane.getHorizontalScrollBar().setBackground(new Color(30, 30, 30));

        add(scrollPane, BorderLayout.CENTER);
        highlightSyntaxLater();
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String text) {
        textArea.setText(text);
        highlightSyntaxLater();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void addDocumentListener(DocumentListener listener) {
        document.addDocumentListener(listener);
    }

    public String getDisplayName() {
        String baseName = file != null ? file.getName() : "Untitled";
        return modified ? baseName + " *" : baseName;
    }

    public void undo() {
        try {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
        } catch (Exception ignored) {
        }
    }

    public void redo() {
        try {
            if (undoManager.canRedo()) {
                undoManager.redo();
            }
        } catch (Exception ignored) {
        }
    }

    public void cut() {
        textArea.cut();
    }

    public void copy() {
        textArea.copy();
    }

    public void paste() {
        textArea.paste();
    }

    public void selectAll() {
        textArea.selectAll();
    }

    public void setFontSize(float size) {
        Font font = new Font("Consolas", Font.PLAIN, (int) size);
        textArea.setFont(font);
        lineNumbers.setFont(font);
        lineNumbers.repaint();
    }

    public void focusTextArea() {
        textArea.requestFocusInWindow();
    }

    private void highlightSyntaxLater() {
        if (applyingHighlight) {
            return;
        }
        applyingHighlight = true;
        SwingUtilities.invokeLater(() -> {
            try {
                applySyntaxHighlighting();
            } finally {
                applyingHighlight = false;
            }
        });
    }

    private void applySyntaxHighlighting() {
        String text = textArea.getText();
        StyleContext context = StyleContext.getDefaultStyleContext();

        AttributeSet normal = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, TEXT_COLOR);
        AttributeSet keyword = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, KEYWORD_COLOR);
        AttributeSet string = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, STRING_COLOR);
        AttributeSet comment = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, COMMENT_COLOR);
        AttributeSet number = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, NUMBER_COLOR);
        AttributeSet type = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, TYPE_COLOR);

        document.setCharacterAttributes(0, text.length(), normal, true);
        applyPattern("//.*|/\\*(.|\\R)*?\\*/", text, comment);
        applyPattern("\"([^\"\\\\]|\\\\.)*\"", text, string);
        applyPattern("\\b\\d+(\\.\\d+)?\\b", text, number);
        applyPattern("\\b[A-Z][A-Za-z0-9_]*\\b", text, type);

        Matcher matcher = Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b").matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            if (KEYWORDS.contains(word)) {
                document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), keyword, true);
            }
        }
    }

    private void applyPattern(String regex, String text, AttributeSet attributes) {
        Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(text);
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), attributes, true);
        }
    }
}

class LineNumbers extends JPanel {
    private JTextComponent textArea;

    public LineNumbers(JTextComponent textArea) {
        this.textArea = textArea;
        setPreferredSize(new Dimension(56, Integer.MAX_VALUE));
        setBackground(new Color(24, 24, 24));

        textArea.addCaretListener(e -> repaint());
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { repaint(); }
            @Override
            public void removeUpdate(DocumentEvent e) { repaint(); }
            @Override
            public void changedUpdate(DocumentEvent e) { repaint(); }
        });
        textArea.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                revalidate();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        FontMetrics fm = g.getFontMetrics();
        Element root = textArea.getDocument().getDefaultRootElement();
        int caretLine = root.getElementIndex(textArea.getCaretPosition());
        Rectangle clip = g.getClipBounds();

        for (int line = 0; line < root.getElementCount(); line++) {
            Element element = root.getElement(line);
            try {
                Rectangle bounds = textArea.modelToView2D(element.getStartOffset()).getBounds();
                if (bounds.y + bounds.height < clip.y || bounds.y > clip.y + clip.height) {
                    continue;
                }

                String number = String.valueOf(line + 1);
                g.setColor(line == caretLine ? new Color(220, 220, 220) : new Color(110, 110, 110));
                int x = getWidth() - fm.stringWidth(number) - 10;
                int y = bounds.y + fm.getAscent();
                g.drawString(number, x, y);
            } catch (BadLocationException ignored) {
            }
        }
    }
}
