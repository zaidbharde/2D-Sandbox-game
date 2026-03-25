package ide;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TerminalPanel extends JPanel {
    private OpenCodeIDE parent;
    private JTextArea terminalArea;
    private JScrollPane scrollPane;

    private String currentDirectory;
    private List<String> commandHistory;
    private int historyIndex;
    private int inputStart;

    private Process currentProcess;
    private ProcessBuilder currentPb;
    private boolean externalProgramSession;

    private static final Color BG_COLOR = new Color(10, 10, 10);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);
    private static final Color HEADER_BG = new Color(20, 20, 20);

    public TerminalPanel(OpenCodeIDE parent) {
        this.parent = parent;
        this.commandHistory = new ArrayList<>();
        this.historyIndex = -1;
        this.currentDirectory = System.getProperty("user.home");

        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 30, 30)));

        setupComponents();
    }

    private void setupComponents() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, 30));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 30, 30)));

        JLabel title = new JLabel("  TERMINAL");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(TEXT_COLOR);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        buttonPanel.setBackground(HEADER_BG);

        JButton clearBtn = createHeaderButton("X");
        clearBtn.setToolTipText("Clear Terminal");
        clearBtn.addActionListener(e -> clearTerminal());

        JButton killBtn = createHeaderButton("\u25A0");
        killBtn.setToolTipText("Kill Process");
        killBtn.addActionListener(e -> killCurrentProcess());

        buttonPanel.add(clearBtn);
        buttonPanel.add(killBtn);

        header.add(title, BorderLayout.WEST);
        header.add(buttonPanel, BorderLayout.EAST);

        terminalArea = new JTextArea();
        terminalArea.setBackground(BG_COLOR);
        terminalArea.setForeground(TEXT_COLOR);
        terminalArea.setCaretColor(Color.WHITE);
        terminalArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        terminalArea.setLineWrap(false);
        terminalArea.setWrapStyleWord(false);
        terminalArea.setTabSize(4);
        terminalArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        terminalArea.setCaret(new DefaultCaret());
        terminalArea.getCaret().setBlinkRate(500);

        terminalArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleTerminalKeyPressed(e);
            }
        });

        scrollPane = new JScrollPane(terminalArea);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setBackground(new Color(25, 25, 25));
        scrollPane.getHorizontalScrollBar().setBackground(new Color(25, 25, 25));

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        appendWelcome();
        appendPrompt();
    }

    private JButton createHeaderButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 10));
        button.setForeground(TEXT_COLOR);
        button.setBackground(new Color(30, 30, 30));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(24, 24));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void handleTerminalKeyPressed(KeyEvent e) {
        int caret = terminalArea.getCaretPosition();

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            e.consume();
            submitCurrentInput();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_UP) {
            e.consume();
            navigateHistory(-1);
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            e.consume();
            navigateHistory(1);
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
            e.consume();
            if (currentProcess != null && currentProcess.isAlive()) {
                killCurrentProcess();
            } else {
                appendText("^C\n");
                appendPrompt();
            }
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_L && e.isControlDown()) {
            e.consume();
            clearTerminal();
            return;
        }

        if (caret < inputStart) {
            terminalArea.setCaretPosition(terminalArea.getDocument().getLength());
        }

        if ((e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_LEFT) && caret <= inputStart) {
            e.consume();
        }
    }

    private void appendWelcome() {
        appendText("OpenCode IDE Terminal v1.0\n");
        appendText("Type 'help' for available commands, 'exit' to close\n\n");
    }

    private void appendPrompt() {
        appendText(buildPromptText());
        inputStart = terminalArea.getDocument().getLength();
        moveCaretToEnd();
    }

    private String buildPromptText() {
        return System.getProperty("user.name") + "@IDE " + getShortPath() + " $ ";
    }

    private String getShortPath() {
        String home = System.getProperty("user.home");
        if (currentDirectory.startsWith(home)) {
            return "~" + currentDirectory.substring(home.length()).replace("\\", "/");
        }
        return currentDirectory.replace("\\", "/");
    }

    private void appendText(String text) {
        terminalArea.append(text);
        moveCaretToEnd();
    }

    private void moveCaretToEnd() {
        SwingUtilities.invokeLater(() -> terminalArea.setCaretPosition(terminalArea.getDocument().getLength()));
    }

    public void clearTerminal() {
        terminalArea.setText("");
        appendWelcome();
        appendPrompt();
        externalProgramSession = false;
    }

    public void newTerminal() {
        if (currentProcess != null && currentProcess.isAlive()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Kill current process?", "New Terminal", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                terminalArea.requestFocusInWindow();
                return;
            }
            killCurrentProcess();
        }
        appendText("\n[new session]\n");
        appendPrompt();
        terminalArea.requestFocusInWindow();
    }

    private void submitCurrentInput() {
        String command = getCurrentInput();
        appendText("\n");

        if (currentProcess != null && currentProcess.isAlive()) {
            sendToRunningProcess(command);
            return;
        }

        executeCommand(command);
    }

    private String getCurrentInput() {
        Document document = terminalArea.getDocument();
        try {
            return document.getText(inputStart, document.getLength() - inputStart);
        } catch (BadLocationException e) {
            return "";
        }
    }

    private void replaceCurrentInput(String value) {
        Document document = terminalArea.getDocument();
        try {
            document.remove(inputStart, document.getLength() - inputStart);
            document.insertString(document.getLength(), value, null);
            moveCaretToEnd();
        } catch (BadLocationException ignored) {
        }
    }

    public void executeCommand(String command) {
        if (command.trim().isEmpty()) {
            appendPrompt();
            return;
        }

        commandHistory.add(command);
        historyIndex = commandHistory.size();

        String trimmed = command.trim();

        if (trimmed.equalsIgnoreCase("clear") || trimmed.equalsIgnoreCase("cls")) {
            clearTerminal();
            return;
        }

        if (trimmed.equalsIgnoreCase("help")) {
            showHelp();
            appendPrompt();
            return;
        }

        if (trimmed.equalsIgnoreCase("exit")) {
            appendText("Use File > Exit to close the IDE\n");
            appendPrompt();
            return;
        }

        if (trimmed.equalsIgnoreCase("history")) {
            showHistory();
            appendPrompt();
            return;
        }

        if (trimmed.equalsIgnoreCase("pwd")) {
            appendText(currentDirectory + "\n");
            appendPrompt();
            return;
        }

        if (trimmed.toLowerCase().startsWith("cd ") || trimmed.equalsIgnoreCase("cd") || trimmed.equalsIgnoreCase("cd~")) {
            changeDirectory(trimmed);
            appendPrompt();
            return;
        }

        if (trimmed.equalsIgnoreCase("ls") || trimmed.equalsIgnoreCase("dir")) {
            listDirectory();
            appendPrompt();
            return;
        }

        runProcess(trimmed);
    }

    public void clearForProgramRun(String title) {
        terminalArea.setText("");
        externalProgramSession = true;
        appendText("=== Running " + title + " ===\n");
        inputStart = terminalArea.getDocument().getLength();
    }

    public void clearForCompilation() {
        terminalArea.setText("");
        externalProgramSession = true;
        appendText("=== Compiling ===\n");
        inputStart = terminalArea.getDocument().getLength();
    }

    public void appendOutput(String text) {
        appendText(text);
        inputStart = terminalArea.getDocument().getLength();
    }

    public void appendOutput(String text, Color color) {
        appendOutput(text);
    }

    public void attachExternalProcess(Process process) {
        currentProcess = process;
        externalProgramSession = true;
        inputStart = terminalArea.getDocument().getLength();
        terminalArea.requestFocusInWindow();
    }

    public void detachExternalProcess(int exitCode) {
        currentProcess = null;
        appendText("\n=== Exit: " + exitCode + " ===\n");
        externalProgramSession = false;
        appendPrompt();
    }

    public void detachExternalProcess() {
        currentProcess = null;
        externalProgramSession = false;
        appendPrompt();
    }

    private void changeDirectory(String command) {
        String path;

        if (command.equalsIgnoreCase("cd") || command.equalsIgnoreCase("cd~")) {
            path = System.getProperty("user.home");
        } else {
            path = command.substring(2).trim();
        }

        if (path.startsWith("~")) {
            path = System.getProperty("user.home") + path.substring(1);
        } else if (!path.matches("^[a-zA-Z]:.*") && !path.startsWith("/")) {
            path = currentDirectory + File.separator + path;
        }

        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            try {
                currentDirectory = dir.getCanonicalPath();
            } catch (IOException e) {
                currentDirectory = dir.getAbsolutePath();
            }
        } else {
            appendText("cd: no such directory: " + path + "\n");
        }
    }

    private void listDirectory() {
        File dir = new File(currentDirectory);
        File[] files = dir.listFiles();

        if (files == null) {
            appendText("Unable to list directory\n");
            return;
        }

        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File file : files) {
            if (file.isHidden()) continue;
            appendText(file.getName() + (file.isDirectory() ? "/  " : "  "));
        }
        appendText("\n");
    }

    private void showHistory() {
        for (int i = 0; i < commandHistory.size(); i++) {
            appendText(String.format("%4d  %s\n", i + 1, commandHistory.get(i)));
        }
    }

    private void showHelp() {
        appendText("\nAvailable Commands:\n");
        appendText("  cd <dir>       Change directory\n");
        appendText("  ls / dir       List files\n");
        appendText("  pwd            Print working directory\n");
        appendText("  clear / cls    Clear terminal\n");
        appendText("  history        Show command history\n");
        appendText("  help           Show this help\n");
        appendText("  [any command]  Execute system command\n\n");
    }

    private void runProcess(String command) {
        if (currentProcess != null && currentProcess.isAlive()) {
            appendText("A process is running. Ctrl+C to stop it.\n");
            return;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("windows")) {
                currentPb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                currentPb = new ProcessBuilder("bash", "-c", command);
            }

            currentPb.directory(new File(currentDirectory));
            currentPb.redirectErrorStream(true);

            currentProcess = currentPb.start();
            parent.getStatusBar().setRunning(true);

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    int ch;
                    while ((ch = reader.read()) != -1) {
                        char outputChar = (char) ch;
                        SwingUtilities.invokeLater(() -> appendText(String.valueOf(outputChar)));
                    }
                } catch (IOException ignored) {
                }
            });
            outputThread.setDaemon(true);
            outputThread.start();

            Thread waitThread = new Thread(() -> {
                try {
                    int exitCode = currentProcess.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        appendText("\n" + (exitCode == 0 ? "[Done]" : "[Process exited with code " + exitCode + "]") + "\n");
                        parent.getStatusBar().setRunning(false);
                        currentProcess = null;
                        externalProgramSession = false;
                        appendPrompt();
                    });
                } catch (InterruptedException ignored) {
                }
            });
            waitThread.setDaemon(true);
            waitThread.start();
        } catch (IOException e) {
            appendText("Error: " + e.getMessage() + "\n");
            appendPrompt();
        }
    }

    private void sendToRunningProcess(String input) {
        try {
            OutputStream stream = currentProcess.getOutputStream();
            stream.write((input + System.lineSeparator()).getBytes());
            stream.flush();
        } catch (IOException e) {
            appendText("Input error: " + e.getMessage() + "\n");
            appendPrompt();
        }
    }

    public void killCurrentProcess() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
            appendText("^C\n[Process killed]\n");
            currentProcess = null;
            parent.getStatusBar().setRunning(false);
            externalProgramSession = false;
            appendPrompt();
        }
    }

    private void navigateHistory(int direction) {
        if (currentProcess != null && currentProcess.isAlive()) {
            return;
        }
        if (commandHistory.isEmpty()) return;

        historyIndex += direction;

        if (historyIndex < 0) {
            historyIndex = 0;
        } else if (historyIndex >= commandHistory.size()) {
            historyIndex = commandHistory.size();
            replaceCurrentInput("");
            return;
        }

        replaceCurrentInput(commandHistory.get(historyIndex));
    }
}
