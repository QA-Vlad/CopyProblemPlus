# Copy Problem Plus for JetBrains IDEs 🤡

[![Version](https://img.shields.io/jetbrains/plugin/v/28098-copy-problem-plus)](https://plugins.jetbrains.com/plugin/28098-copy-problem-plus)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28098-copy-problem-plus)](https://plugins.jetbrains.com/plugin/28098-copy-problem-plus)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/28098-copy-problem-plus)](https://plugins.jetbrains.com/plugin/28098-copy-problem-plus)
[![Reviews](https://img.shields.io/badge/Leave%20a-Review-green)](https://plugins.jetbrains.com/plugin/28098-copy-problem-plus/reviews)

> Hey there, my little vibe coder! Tired of awkwardly copying warnings from your IDE to your favorite AI chat? I've solved this problem for you!

![Copy Problem Plus Demo](https://raw.githubusercontent.com/QA-Vlad/CopyProblemPlus/master/src/main/resources/META-INF/demo.gif)

## 🎯 The Problem

Where previously warnings copied like this:
```
[UNRESOLVED_REFERENCE] Unresolved reference: ProblemInfo
```

## 💡 The Solution

Now they copy like this:
```
In file src/main/kotlin/.../CopyProblemAction.kt - [UNRESOLVED_REFERENCE] Unresolved reference: ProblemInfo (Line № 19)
```

No more explaining to your AI chat which file and line number the warning is on! 

## 🚀 Features

- **One-click copy** with full context (file name + line number)
- **Customizable format** - make it work the way YOU want
- **Smart path handling** - relative or absolute, your choice
- **Hide the boring copy button** - who needs it anyway?
- **Hotkey support** - `Ctrl+Alt+P` for true vibe coders

## 📥 Installation

### [🚀 Install from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/28098-copy-problem-plus)

### Method 1: From IDE
1. Open **Settings** → **Plugins**
2. Search for **"Copy Problem Plus"**
3. Click **Install**
4. Restart your IDE
5. Start vibing! 🎉

### Method 2: From GitHub Release
1. Download the latest `.zip` from [Releases](https://github.com/QA-Vlad/CopyProblemPlus/releases)
2. Open **Settings** → **Plugins** → **⚙️** → **Install Plugin from Disk...**
3. Select the downloaded `.zip` file
4. Restart your IDE

## 🎮 Usage

1. Open the **Problems** panel
2. Right-click on any warning/error
3. Select **"Copy Problem Plus"** or press `Ctrl+Alt+P`
4. Paste into your favorite AI chat
5. Watch the magic happen! ✨

### Bonus: Copy ALL Problems from a File

When you click on a file node with multiple problems:

```
Problems in file src/.../CopyProblemAction.kt:
1. [UNRESOLVED_REFERENCE] Unresolved reference: ProblemInfo (Line № 19)
2. Expecting an expression (Line № 19)
3. Class "CopyProblemAction" is never used (Line № 13)
4. Condition 'problemInfo == null' is always false (Line № 42)
5. Condition 'problemInfo != null' is always true (Line № 47)
```

## ⚙️ Configuration

Access settings via **File** → **Settings** → **Tools** → **Copy Problem Plus**

- **Format Pattern**: Customize your copy format
- **Use Relative Paths**: Because who needs full paths?
- **Hide Standard Copy**: Out with the old, in with the Plus!
- **Configure Shortcuts**: Make it YOUR way

## 🏗️ Building from Source

```bash
# Clone the repo
git clone https://github.com/QA-Vlad/CopyProblemPlus.git
cd CopyProblemPlus

# Build
./gradlew buildPlugin

# Run in sandbox
./gradlew runIde
```

## 🤝 Contributing

Found a bug? Have an idea? PRs are welcome!

1. Fork it
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- All the vibe coders who copy-paste their problems into AI chats
- JetBrains for the amazing IntelliJ Platform
- You, for being awesome! 🤡

---

Made with ❤️ and 🤡 by [Vlad QA](https://github.com/QA-Vlad)

_P.S. Now your AI assistant will definitely understand where your problems are! 😉_