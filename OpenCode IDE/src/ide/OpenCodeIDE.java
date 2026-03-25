package ide;

import javax.swing.*;
import javax.tools.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenCodeIDE extends JFrame {
    private JSplitPane leftSplitPane;
    private JSplitPane rightSplitPane;
    
    private FileExplorerPanel fileExplorer;
    private EditorPanel editorPanel;
    private TerminalPanel terminalPanel;
    private StatusBar statusBar;
    
    private static final String APP_NAME = "OpenCode IDE";
    private static final Color BG_COLOR = new Color(20, 20, 20);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([\\w\\.]+)\\s*;");
    
    private Map<String, ProjectInfo> openProjects = new HashMap<>();
    private volatile Process currentRunningProcess;
    
    public OpenCodeIDE() {
        setTitle(APP_NAME);
        setSize(1400, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        setupLayout();
        setupMenuBar();
        setupToolBar();
        setupKeyboardShortcuts();
        
        setVisible(true);
    }
    
    private void initComponents() {
        getContentPane().setBackground(BG_COLOR);
        
        fileExplorer = new FileExplorerPanel(this);
        editorPanel = new EditorPanel(this);
        terminalPanel = new TerminalPanel(this);
        statusBar = new StatusBar();
    }
    
    private void setupLayout() {
        leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftSplitPane.setLeftComponent(fileExplorer);
        leftSplitPane.setRightComponent(editorPanel);
        leftSplitPane.setDividerLocation(250);
        leftSplitPane.setDividerSize(4);
        leftSplitPane.setBackground(BG_COLOR);
        leftSplitPane.setBorder(null);
        
        rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplitPane.setLeftComponent(leftSplitPane);
        rightSplitPane.setRightComponent(terminalPanel);
        rightSplitPane.setDividerLocation(1000);
        rightSplitPane.setDividerSize(4);
        rightSplitPane.setBackground(BG_COLOR);
        rightSplitPane.setBorder(null);
        
        setLayout(new BorderLayout());
        add(rightSplitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(25, 25, 25));
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 40)));
        
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createRunMenu());
        menuBar.add(createTerminalMenu());
        menuBar.add(createHelpMenu());
        
        setJMenuBar(menuBar);
    }
    
    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        menu.setForeground(new Color(200, 200, 200));
        menu.setBackground(new Color(25, 25, 25));
        
        addMenuItem(menu, "Open Folder...", e -> openFolder());
        addMenuItem(menu, "New File", e -> editorPanel.createNewFile());
        addMenuItem(menu, "Save", e -> editorPanel.saveCurrentFile());
        addMenuItem(menu, "Save As...", e -> editorPanel.saveAsCurrentFile());
        menu.addSeparator();
        addMenuItem(menu, "Exit", e -> System.exit(0));
        
        return menu;
    }
    
    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic('E');
        menu.setForeground(new Color(200, 200, 200));
        menu.setBackground(new Color(25, 25, 25));
        
        addMenuItem(menu, "Undo", e -> editorPanel.undo());
        addMenuItem(menu, "Redo", e -> editorPanel.redo());
        menu.addSeparator();
        addMenuItem(menu, "Cut", e -> editorPanel.cut());
        addMenuItem(menu, "Copy", e -> editorPanel.copy());
        addMenuItem(menu, "Paste", e -> editorPanel.paste());
        menu.addSeparator();
        addMenuItem(menu, "Select All", e -> editorPanel.selectAll());
        
        return menu;
    }
    
    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic('V');
        menu.setForeground(new Color(200, 200, 200));
        menu.setBackground(new Color(25, 25, 25));
        
        addMenuItem(menu, "Toggle Sidebar", e -> toggleSidebar());
        addMenuItem(menu, "Toggle Terminal", e -> toggleTerminal());
        menu.addSeparator();
        addMenuItem(menu, "Zoom In", e -> editorPanel.zoomIn());
        addMenuItem(menu, "Zoom Out", e -> editorPanel.zoomOut());
        addMenuItem(menu, "Reset Zoom", e -> editorPanel.resetZoom());
        
        return menu;
    }
    
    private JMenu createRunMenu() {
        JMenu menu = new JMenu("Run");
        menu.setMnemonic('R');
        menu.setForeground(new Color(200, 200, 200));
        menu.setBackground(new Color(25, 25, 25));
        
        addMenuItem(menu, "Run (F5)", e -> runCurrentFile());
        addMenuItem(menu, "Stop", e -> stopExecution());
        menu.addSeparator();
        addMenuItem(menu, "Compile", e -> compileCurrentFile());
        
        return menu;
    }
    
    private JMenu createTerminalMenu() {
        JMenu menu = new JMenu("Terminal");
        menu.setMnemonic('T');
        menu.setForeground(new Color(200, 200, 200));
        menu.setBackground(new Color(25, 25, 25));
        
        addMenuItem(menu, "New Terminal", e -> terminalPanel.newTerminal());
        addMenuItem(menu, "Clear Terminal", e -> terminalPanel.clearTerminal());
        
        return menu;
    }
    
    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic('H');
        menu.setForeground(new Color(200, 200, 200));
        menu.setBackground(new Color(25, 25, 25));
        
        addMenuItem(menu, "About", e -> showAbout());
        
        return menu;
    }
    
    private void addMenuItem(JMenu menu, String text, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setForeground(new Color(200, 200, 200));
        item.setBackground(new Color(40, 40, 40));
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.addActionListener(action);
        menu.add(item);
    }
    
    private void setupToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setBackground(new Color(25, 25, 25));
        toolBar.setFloatable(false);
        toolBar.setBorderPainted(false);
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 40)));
        
        addToolBarButton(toolBar, "New", e -> editorPanel.createNewFile());
        addToolBarButton(toolBar, "Open", e -> openFolder());
        addToolBarButton(toolBar, "Save", e -> editorPanel.saveCurrentFile());
        toolBar.addSeparator();
        addToolBarButton(toolBar, "Run", e -> runCurrentFile());
        addToolBarButton(toolBar, "Stop", e -> stopExecution());
        toolBar.addSeparator();
        addToolBarButton(toolBar, "Terminal", e -> terminalPanel.newTerminal());
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private void addToolBarButton(JToolBar toolBar, String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setForeground(new Color(200, 200, 200));
        button.setBackground(new Color(35, 35, 35));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.addActionListener(action);
        button.setMargin(new Insets(4, 10, 4, 10));
        toolBar.add(button);
    }
    
    private void setupKeyboardShortcuts() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "run");
        getRootPane().getActionMap().put("run", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { runCurrentFile(); }
        });
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl O"), "openFolder");
        getRootPane().getActionMap().put("openFolder", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { openFolder(); }
        });
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl S"), "save");
        getRootPane().getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { editorPanel.saveCurrentFile(); }
        });
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl N"), "newFile");
        getRootPane().getActionMap().put("newFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { editorPanel.createNewFile(); }
        });
    }
    
    public void openFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Open Folder");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            openProject(folder);
        }
    }
    
    public void openProject(File projectFolder) {
        ProjectInfo project = new ProjectInfo(projectFolder);
        openProjects.put(projectFolder.getAbsolutePath(), project);
        
        fileExplorer.setRootFolder(projectFolder);
        statusBar.setStatus("Opened: " + projectFolder.getName());
        statusBar.setProject(projectFolder.getName());
        
        setTitle(APP_NAME + " - " + projectFolder.getName());
    }
    
    public void openFile(File file) {
        editorPanel.openFile(file);
    }
    
    public void runCurrentFile() {
        File currentFile = editorPanel.getCurrentFile();
        if (currentFile == null) {
            terminalPanel.appendOutput("No file open!\n", Color.RED);
            return;
        }

        editorPanel.saveCurrentFile();

        String fileName = currentFile.getName();
        String extension = getExtension(fileName);
        terminalPanel.clearForProgramRun(fileName);
        
        try {
            ProcessBuilder pb;
            
            switch (extension.toLowerCase()) {
                case "java" -> {
                    JavaCompileResult compileResult = compileJavaFile(currentFile, true);
                    if (!compileResult.success()) {
                        return;
                    }

                    pb = new ProcessBuilder("java", "-cp",
                        compileResult.outputDirectory().getAbsolutePath(),
                        compileResult.mainClassName());
                    pb.directory(compileResult.projectRoot());
                }
                case "py", "python" -> {
                    pb = new ProcessBuilder("python", currentFile.getAbsolutePath());
                    pb.directory(currentFile.getParentFile());
                }
                case "js", "javascript" -> {
                    pb = new ProcessBuilder("node", currentFile.getAbsolutePath());
                    pb.directory(currentFile.getParentFile());
                }
                case "html", "htm" -> {
                    try {
                        Desktop.getDesktop().browse(currentFile.toURI());
                        terminalPanel.appendOutput("Opened in browser...\n", Color.GREEN);
                    } catch (Exception e) {
                        terminalPanel.appendOutput("Error: " + e.getMessage() + "\n", Color.RED);
                    }
                    return;
                }
                default -> {
                    terminalPanel.appendOutput("Unsupported: " + extension + "\n", Color.RED);
                    return;
                }
            }
            
            final Process runningProcess = pb.start();
            currentRunningProcess = runningProcess;
            terminalPanel.attachExternalProcess(runningProcess);
            statusBar.setRunning(true);
            
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(runningProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    int ch;
                    while ((ch = reader.read()) != -1) {
                        final String chunk = String.valueOf((char) ch);
                        SwingUtilities.invokeLater(() -> terminalPanel.appendOutput(chunk, Color.WHITE));
                    }
                } catch (IOException e) {}
            });
            
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(runningProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                    int ch;
                    while ((ch = reader.read()) != -1) {
                        final String chunk = String.valueOf((char) ch);
                        SwingUtilities.invokeLater(() -> terminalPanel.appendOutput(chunk, Color.RED));
                    }
                } catch (IOException e) {}
            });
            
            outputThread.start();
            errorThread.start();
            
            Thread waitThread = new Thread(() -> {
                try {
                    int exitCode = runningProcess.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        currentRunningProcess = null;
                        statusBar.setRunning(false);
                        terminalPanel.detachExternalProcess(exitCode);
                    });
                } catch (InterruptedException e) {}
            });
            waitThread.start();
            
        } catch (Exception e) {
            terminalPanel.detachExternalProcess();
            terminalPanel.appendOutput("Error: " + e.getMessage() + "\n", Color.RED);
        }
    }
    
    private void compileCurrentFile() {
        File currentFile = editorPanel.getCurrentFile();
        if (currentFile == null) {
            terminalPanel.appendOutput("No file open!\n", Color.RED);
            return;
        }

        editorPanel.saveCurrentFile();

        String extension = getExtension(currentFile.getName());
        if (extension.equalsIgnoreCase("java")) {
            terminalPanel.clearForCompilation();
            compileJavaFile(currentFile, true);
        }
    }
    
    private void stopExecution() {
        Process process = currentRunningProcess;
        if (process != null) {
            process.destroy();
            currentRunningProcess = null;
        }
        terminalPanel.appendOutput("\n[Stopped]\n", Color.YELLOW);
        terminalPanel.detachExternalProcess();
        statusBar.setRunning(false);
    }
    
    private void toggleSidebar() {
        int location = leftSplitPane.getDividerLocation();
        if (location < 30) {
            leftSplitPane.setDividerLocation(250);
        } else {
            leftSplitPane.setDividerLocation(0);
        }
    }
    
    private void toggleTerminal() {
        int location = rightSplitPane.getDividerLocation();
        int width = rightSplitPane.getWidth();
        if (width <= 0) {
            return;
        }
        if (width - location < 40) {
            rightSplitPane.setDividerLocation(Math.max(700, width - 420));
        } else {
            rightSplitPane.setDividerLocation(width);
        }
    }
    
    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "OpenCode IDE v1.0\n\nA lightweight IDE built with Java Swing\n\nPure Java, Full Dark Theme",
            "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) return fileName.substring(lastDot + 1);
        return "";
    }
    
    public File getSuggestedDirectoryForNewFile() {
        File selected = fileExplorer.getSelectedDirectoryOrRoot();
        if (selected != null) {
            return selected;
        }
        File current = editorPanel.getCurrentFile();
        return current != null ? current.getParentFile() : null;
    }
    
    public void refreshProjectTree() {
        fileExplorer.refreshTree();
    }
    
    private JavaCompileResult compileJavaFile(File currentFile, boolean writeOutput) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            if (writeOutput) {
                terminalPanel.appendOutput("Java compiler not found. Run the IDE with a JDK, not a JRE.\n", Color.RED);
            }
            return JavaCompileResult.failed();
        }

        File projectRoot = resolveProjectRoot(currentFile);
        File outputDir = new File(projectRoot, "build/classes");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            if (writeOutput) {
                terminalPanel.appendOutput("Could not create output folder: " + outputDir.getAbsolutePath() + "\n", Color.RED);
            }
            return JavaCompileResult.failed();
        }

        List<File> sourceFiles = collectJavaSources(projectRoot);
        if (sourceFiles.isEmpty()) {
            if (writeOutput) {
                terminalPanel.appendOutput("No Java source files found in project.\n", Color.RED);
            }
            return JavaCompileResult.failed();
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

        try {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(outputDir.getAbsolutePath());
            options.add("-classpath");
            options.add(outputDir.getAbsolutePath());
            options.add("-encoding");
            options.add("UTF-8");

            boolean success = Boolean.TRUE.equals(
                compiler.getTask(null, fileManager, diagnostics, options, null, units).call()
            );

            if (writeOutput) {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    String location = diagnostic.getSource() != null
                        ? new File(diagnostic.getSource().toUri()).getName()
                        : "compiler";
                    Color color = diagnostic.getKind() == Diagnostic.Kind.ERROR ? Color.RED : Color.YELLOW;
                    terminalPanel.appendOutput(
                        location + ":" + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null) + "\n",
                        color
                    );
                }
            }

            if (!success) {
                if (writeOutput && diagnostics.getDiagnostics().isEmpty()) {
                    terminalPanel.appendOutput("Compilation failed.\n", Color.RED);
                }
                return JavaCompileResult.failed();
            }

            String mainClassName = resolveMainClassName(currentFile, projectRoot);
            if (writeOutput) {
                terminalPanel.appendOutput("Compilation successful!\n", Color.GREEN);
                terminalPanel.appendOutput("Output: " + outputDir.getAbsolutePath() + "\n", Color.GREEN);
            }
            return new JavaCompileResult(true, outputDir, mainClassName, projectRoot);
        } catch (IOException e) {
            if (writeOutput) {
                terminalPanel.appendOutput("Compiler error: " + e.getMessage() + "\n", Color.RED);
            }
            return JavaCompileResult.failed();
        } finally {
            try {
                fileManager.close();
            } catch (IOException ignored) {
            }
        }
    }
    
    private File resolveProjectRoot(File currentFile) {
        File explorerRoot = fileExplorer.getRootFile();
        if (explorerRoot != null && isWithin(explorerRoot, currentFile)) {
            return explorerRoot;
        }
        return currentFile.getParentFile();
    }
    
    private boolean isWithin(File root, File file) {
        try {
            Path rootPath = root.getCanonicalFile().toPath();
            Path filePath = file.getCanonicalFile().toPath();
            return filePath.startsWith(rootPath);
        } catch (IOException e) {
            return false;
        }
    }
    
    private List<File> collectJavaSources(File projectRoot) {
        List<File> sources = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(projectRoot.toPath())) {
            stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !isGeneratedPath(projectRoot.toPath(), path))
                .forEach(path -> sources.add(path.toFile()));
        } catch (IOException ignored) {
        }
        return sources;
    }
    
    private boolean isGeneratedPath(Path root, Path candidate) {
        Path relative = root.relativize(candidate);
        for (Path segment : relative) {
            String name = segment.toString();
            if (name.equals("build") || name.equals("out") || name.equals("target") || name.equals("bin") || name.equals(".git")) {
                return true;
            }
        }
        return false;
    }
    
    private String resolveMainClassName(File currentFile, File projectRoot) throws IOException {
        String content = Files.readString(currentFile.toPath(), StandardCharsets.UTF_8);
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        String simpleName = currentFile.getName().replaceFirst("\\.java$", "");
        if (matcher.find()) {
            return matcher.group(1) + "." + simpleName;
        }
        return simpleName;
    }
    
    public EditorPanel getEditorPanel() { return editorPanel; }
    public StatusBar getStatusBar() { return statusBar; }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(OpenCodeIDE::new);
    }
}

class ProjectInfo {
    File rootFolder;
    String name;
    
    ProjectInfo(File rootFolder) {
        this.rootFolder = rootFolder;
        this.name = rootFolder.getName();
    }
}

record JavaCompileResult(boolean success, File outputDirectory, String mainClassName, File projectRoot) {
    static JavaCompileResult failed() {
        return new JavaCompileResult(false, null, null, null);
    }
}
