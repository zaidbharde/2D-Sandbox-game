# OpenCode IDE

A lightweight, VS Code-like IDE built entirely with Java Swing.

## Features

- **File Explorer** - Browse and manage project files with folder tree view
- **Tabbed Code Editor** - Open multiple files with tab navigation
- **Syntax Highlighting** - Support for Java, Python, JavaScript, C/C++, and more
- **Integrated Terminal** - Full command execution with history and process management
- **Output Console** - View program output with color-coded messages
- **Multi-language Support** - Run Java, Python, Node.js, C/C++ programs
- **VS Code-like UI** - Familiar dark theme with split panes
- **Keyboard Shortcuts** - Quick actions for common tasks

## Requirements

- Java 17 or higher
- javac (Java compiler) in PATH
- Optional: Python, Node.js, GCC/G++ for running other languages

## Building

### Windows
```batch
build.bat
```

### Unix/Mac
```bash
chmod +x build.sh
./build.sh
```

## Running

### Windows
```batch
run.bat
```

### Unix/Mac
```bash
chmod +x run.sh
./run.sh
```

## Usage

### Opening a Project
1. Click **File > Open Folder...** or press `Ctrl+O`
2. Select your project directory
3. Files will appear in the sidebar

### Running Code
1. Open the file you want to run
2. Press `F5` or click the **Run** button
3. Output appears in the Output panel

### Supported Languages
- **Java** - Full compilation and execution
- **Python** - Requires Python installed
- **JavaScript** - Requires Node.js installed
- **C/C++** - Requires GCC/G++ installed
- **HTML** - Opens in default browser

### Terminal Commands
- `cd <directory>` - Change directory
- `ls` / `dir` - List files
- `pwd` - Print working directory
- `clear` - Clear terminal
- `history` - Show command history
- Any system command

### Keyboard Shortcuts
| Shortcut | Action |
|----------|--------|
| `Ctrl+O` | Open Folder |
| `Ctrl+N` | New File |
| `Ctrl+S` | Save |
| `Ctrl+W` | Close Tab |
| `F5` | Run |
| `Ctrl+B` | Toggle Sidebar |
| `Ctrl+J` | Toggle Terminal |
| `Ctrl++` | Zoom In |
| `Ctrl+-` | Zoom Out |
| `Up/Down` | Terminal History |

## Project Structure

```
OpenCode IDE/
├── src/
│   └── ide/
│       ├── OpenCodeIDE.java      # Main application window
│       ├── FileExplorerPanel.java # File tree sidebar
│       ├── EditorPanel.java      # Tabbed code editor
│       ├── TerminalPanel.java    # Integrated terminal
│       ├── OutputPanel.java      # Program output
│       └── StatusBar.java        # Status bar
├── build.xml                     # Ant build file
├── build.bat                     # Windows build script
├── build.sh                      # Unix build script
└── README.md                      # This file
```

## Customization

The IDE uses a dark theme with these colors:
- Background: #1E1E1E
- Sidebar: #282828
- Editor: #1E1E1E
- Terminal: #141414
- Accent: Cyan (#8BE9FD)

## License

MIT License
