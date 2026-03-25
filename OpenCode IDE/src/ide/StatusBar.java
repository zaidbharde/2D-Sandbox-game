package ide;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {
    private JLabel statusLabel;
    private JLabel projectLabel;
    private JLabel fileLabel;
    private JLabel encodingLabel;
    private JLabel languageLabel;
    private JPanel runningIndicator;
    private boolean isRunning;
    
    private static final Color BG_COLOR = new Color(15, 15, 15);
    private static final Color TEXT_COLOR = new Color(180, 180, 180);
    private static final Color RUNNING_COLOR = new Color(255, 165, 0);
    
    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(0, 24));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 30, 30)));
        
        setupLeftPanel();
        setupRightPanel();
    }
    
    private void setupLeftPanel() {
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(BG_COLOR);
        
        runningIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isRunning) {
                    g2.setColor(RUNNING_COLOR);
                    g2.fillOval(4, 4, 12, 12);
                } else {
                    g2.setColor(BG_COLOR);
                    g2.fillOval(4, 4, 12, 12);
                }
            }
        };
        runningIndicator.setPreferredSize(new Dimension(20, 20));
        runningIndicator.setBackground(BG_COLOR);
        
        projectLabel = createLabel("No project");
        statusLabel = createLabel("Ready");
        
        leftPanel.add(runningIndicator);
        leftPanel.add(projectLabel);
        leftPanel.add(createSeparator());
        leftPanel.add(statusLabel);
        
        add(leftPanel, BorderLayout.WEST);
    }
    
    private void setupRightPanel() {
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(BG_COLOR);
        
        fileLabel = createLabel("No file");
        languageLabel = createLabel("Plain Text");
        encodingLabel = createLabel("UTF-8");
        
        rightPanel.add(fileLabel);
        rightPanel.add(createSeparator());
        rightPanel.add(languageLabel);
        rightPanel.add(createSeparator());
        rightPanel.add(encodingLabel);
        
        add(rightPanel, BorderLayout.EAST);
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(TEXT_COLOR);
        label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        return label;
    }
    
    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 14));
        sep.setBackground(new Color(50, 50, 50));
        sep.setForeground(new Color(50, 50, 50));
        return sep;
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    public void setProject(String projectName) {
        projectLabel.setText(projectName);
    }
    
    public void setFile(String fileName) {
        fileLabel.setText(fileName);
        setLanguage(fileName);
    }
    
    public void setLanguage(String fileName) {
        String ext = getExtension(fileName);
        String lang;
        
        switch (ext.toLowerCase()) {
            case "java": lang = "Java"; break;
            case "py": lang = "Python"; break;
            case "js": lang = "JavaScript"; break;
            case "ts": lang = "TypeScript"; break;
            case "html": case "htm": lang = "HTML"; break;
            case "css": lang = "CSS"; break;
            case "json": lang = "JSON"; break;
            case "xml": lang = "XML"; break;
            case "md": lang = "Markdown"; break;
            case "c": lang = "C"; break;
            case "cpp": case "cxx": case "cc": lang = "C++"; break;
            case "cs": lang = "C#"; break;
            case "go": lang = "Go"; break;
            case "rs": lang = "Rust"; break;
            case "rb": lang = "Ruby"; break;
            case "php": lang = "PHP"; break;
            case "swift": lang = "Swift"; break;
            case "kt": lang = "Kotlin"; break;
            case "sql": lang = "SQL"; break;
            case "sh": case "bash": lang = "Shell"; break;
            case "yaml": case "yml": lang = "YAML"; break;
            default: lang = ext.isEmpty() ? "Plain Text" : ext.toUpperCase();
        }
        
        languageLabel.setText(lang);
    }
    
    public void setRunning(boolean running) {
        this.isRunning = running;
        runningIndicator.repaint();
        
        if (running) {
            statusLabel.setText("Running...");
            statusLabel.setForeground(RUNNING_COLOR);
        } else {
            statusLabel.setText("Ready");
            statusLabel.setForeground(TEXT_COLOR);
        }
    }
    
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) return fileName.substring(lastDot + 1);
        return "";
    }
}
