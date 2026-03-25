package ide;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class FileExplorerPanel extends JPanel {
    private OpenCodeIDE parent;
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private File rootFile;
    private JPanel headerPanel;
    private JLabel titleLabel;
    private JPopupMenu contextMenu;
    
    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color HEADER_BG = new Color(25, 25, 25);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);
    
    public FileExplorerPanel(OpenCodeIDE parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        
        setupHeader();
        setupTree();
        setupContextMenu();
        
        add(headerPanel, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(fileTree);
        scrollPane.getVerticalScrollBar().setBackground(new Color(35, 35, 35));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void setupHeader() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(HEADER_BG);
        headerPanel.setPreferredSize(new Dimension(0, 35));
        
        titleLabel = new JLabel("EXPLORER");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        buttonPanel.setBackground(HEADER_BG);
        
        JButton newFileBtn = createIconButton("+");
        newFileBtn.setToolTipText("New File");
        newFileBtn.addActionListener(e -> createNewFile());
        
        JButton refreshBtn = createIconButton("\u21BB");
        refreshBtn.setToolTipText("Refresh");
        refreshBtn.addActionListener(e -> refreshTree());
        
        JButton collapseBtn = createIconButton("-");
        collapseBtn.setToolTipText("Collapse All");
        collapseBtn.addActionListener(e -> collapseAll());
        
        buttonPanel.add(newFileBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(collapseBtn);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
    }
    
    private JButton createIconButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(TEXT_COLOR);
        button.setBackground(HEADER_BG);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(26, 26));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private void setupTree() {
        rootNode = new DefaultMutableTreeNode("No folder open");
        treeModel = new DefaultTreeModel(rootNode);
        
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setBackground(BG_COLOR);
        fileTree.setForeground(TEXT_COLOR);
        fileTree.setRowHeight(24);
        fileTree.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileTree.setShowsRootHandles(true);
        fileTree.setExpandsSelectedPaths(true);
        fileTree.setOpaque(true);
        
        FileSystemView fsv = FileSystemView.getFileSystemView();
        
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, 
                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
                
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                setOpaque(true);
                setBackgroundNonSelectionColor(BG_COLOR);
                setBackgroundSelectionColor(new Color(45, 45, 45));
                setBorderSelectionColor(new Color(70, 70, 70));
                
                if (sel) {
                    setForeground(Color.WHITE);
                } else {
                    setForeground(TEXT_COLOR);
                }
                
                if (value instanceof DefaultMutableTreeNode node) {
                    Object obj = node.getUserObject();
                    if (obj instanceof File file) {
                        String label = file.getName().isEmpty() ? file.getAbsolutePath() : file.getName();
                        setText(label);
                        setIcon(fsv.getSystemIcon(file));
                    }
                }
                
                return this;
            }
        };
        fileTree.setCellRenderer(renderer);
        
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick();
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
        });
        
        fileTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleDoubleClick();
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedFile();
                }
            }
        });
    }
    
    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            int row = fileTree.getRowForLocation(e.getX(), e.getY());
            if (row != -1) {
                fileTree.setSelectionRow(row);
                contextMenu.show(fileTree, e.getX(), e.getY());
            }
        }
    }
    
    private void handleDoubleClick() {
        TreePath path = fileTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object obj = node.getUserObject();
            if (obj instanceof File file && file.isFile()) {
                parent.openFile(file);
            }
        }
    }
    
    private void setupContextMenu() {
        contextMenu = new JPopupMenu();
        contextMenu.setBackground(new Color(40, 40, 40));
        
        addMenuItem("New File", this::createNewFile);
        addMenuItem("New Folder", this::createNewFolder);
        contextMenu.addSeparator();
        addMenuItem("Rename", this::renameFile);
        addMenuItem("Delete", this::deleteSelectedFile);
        contextMenu.addSeparator();
        addMenuItem("Copy Path", this::copyPath);
        addMenuItem("Open in Explorer", this::openInExplorer);
    }
    
    private void addMenuItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setForeground(TEXT_COLOR);
        item.setBackground(new Color(40, 40, 40));
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.addActionListener(e -> action.run());
        contextMenu.add(item);
    }
    
    public void setRootFolder(File folder) {
        this.rootFile = folder;
        titleLabel.setText(folder.getName().toUpperCase());
        
        rootNode = new DefaultMutableTreeNode(folder);
        buildTree(folder, rootNode);
        treeModel.setRoot(rootNode);
        
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < fileTree.getRowCount(); i++) {
                fileTree.expandRow(i);
            }
        });
    }
    
    private void buildTree(File folder, DefaultMutableTreeNode node) {
        File[] files = folder.listFiles();
        if (files == null) return;
        
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        
        for (File file : files) {
            if (file.isHidden()) continue;
            
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);
            node.add(childNode);
            
            if (file.isDirectory() && !isIgnoredFolder(file.getName())) {
                File[] subFiles = file.listFiles();
                if (subFiles != null && subFiles.length > 0) {
                    buildTree(file, childNode);
                }
            }
        }
    }
    
    private boolean isIgnoredFolder(String name) {
        String[] ignored = {".git", ".vscode", "node_modules", "target", "bin", "out", "build", ".idea"};
        for (String ign : ignored) {
            if (name.equals(ign)) return true;
        }
        return false;
    }
    
    public void refreshTree() {
        if (rootFile != null && rootFile.exists()) {
            setRootFolder(rootFile);
        }
    }
    
    private void collapseAll() {
        for (int i = fileTree.getRowCount() - 1; i >= 0; i--) {
            fileTree.collapseRow(i);
        }
    }
    
    private void createNewFile() {
        if (rootFile == null) {
            JOptionPane.showMessageDialog(this, "Open a folder first.");
            return;
        }

        File parentDir = rootFile;
        
        TreePath path = fileTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object obj = node.getUserObject();
            if (obj instanceof File selected) {
                if (selected.isDirectory()) {
                    parentDir = selected;
                } else {
                    parentDir = selected.getParentFile();
                }
            }
        }
        
        String name = JOptionPane.showInputDialog(this, "Enter file name:", "New File", JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            try {
                File newFile = new File(parentDir, name.trim());
                File container = newFile.getParentFile();
                if (container != null && !container.exists() && !container.mkdirs()) {
                    throw new IOException("Could not create folder: " + container.getAbsolutePath());
                }
                if (newFile.exists() || newFile.createNewFile()) {
                    refreshTree();
                    parent.openFile(newFile);
                    expandPathToFile(newFile);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
    
    private void createNewFolder() {
        if (rootFile == null) {
            JOptionPane.showMessageDialog(this, "Open a folder first.");
            return;
        }

        File parentDir = rootFile;
        
        TreePath path = fileTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object obj = node.getUserObject();
            if (obj instanceof File selected) {
                if (selected.isDirectory()) {
                    parentDir = selected;
                } else {
                    parentDir = selected.getParentFile();
                }
            }
        }
        
        String name = JOptionPane.showInputDialog(this, "Enter folder name:", "New Folder", JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            File newFolder = new File(parentDir, name.trim());
            if (newFolder.mkdir()) {
                refreshTree();
                expandPathToFile(newFolder);
            }
        }
    }
    
    private void expandPathToFile(File targetFile) {
        TreePath path = findPathToFile(rootNode, targetFile);
        if (path != null) {
            fileTree.expandPath(path);
            fileTree.setSelectionPath(path);
        }
    }
    
    private TreePath findPathToFile(DefaultMutableTreeNode node, File target) {
        Object obj = node.getUserObject();
        if (obj instanceof File file) {
            if (file.equals(target)) {
                return new TreePath(node.getPath());
            }
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath path = findPathToFile(child, target);
            if (path != null) return path;
        }
        
        return null;
    }
    
    private void renameFile() {
        TreePath path = fileTree.getSelectionPath();
        if (path == null) return;
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object obj = node.getUserObject();
        if (obj instanceof File file) {
            String newName = JOptionPane.showInputDialog(this, "Rename to:", file.getName());
            if (newName != null && !newName.trim().isEmpty() && !newName.equals(file.getName())) {
                File newFile = new File(file.getParent(), newName.trim());
                if (file.renameTo(newFile)) {
                    refreshTree();
                    expandPathToFile(newFile);
                }
            }
        }
    }
    
    private void deleteSelectedFile() {
        TreePath path = fileTree.getSelectionPath();
        if (path == null) return;
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object obj = node.getUserObject();
        if (obj instanceof File file) {
            String type = file.isDirectory() ? "folder" : "file";
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Delete " + type + " '" + file.getName() + "'?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                deleteRecursively(file);
                refreshTree();
            }
        }
    }
    
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
    
    private void copyPath() {
        TreePath path = fileTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object obj = node.getUserObject();
            if (obj instanceof File file) {
                StringSelection selection = new StringSelection(file.getAbsolutePath());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            }
        }
    }
    
    private void openInExplorer() {
        TreePath path = fileTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object obj = node.getUserObject();
            if (obj instanceof File file) {
                try {
                    Desktop.getDesktop().open(file.getParentFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public File getRootFile() {
        return rootFile;
    }
    
    public File getSelectedDirectoryOrRoot() {
        if (rootFile == null) {
            return null;
        }
        
        TreePath path = fileTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object obj = node.getUserObject();
            if (obj instanceof File selected) {
                return selected.isDirectory() ? selected : selected.getParentFile();
            }
        }
        
        return rootFile;
    }
}
