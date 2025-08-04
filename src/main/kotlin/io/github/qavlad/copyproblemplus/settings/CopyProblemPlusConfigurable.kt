package io.github.qavlad.copyproblemplus.settings

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.impl.ui.EditKeymapsDialog
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent
import javax.swing.KeyStroke

class CopyProblemPlusConfigurable : Configurable {
    
    private val settings = CopyProblemPlusSettings.getInstance()
    private lateinit var panel: DialogPanel
    private lateinit var shortcutLabel: JBLabel
    
    override fun getDisplayName(): String = "Copy Problem Plus"
    
    override fun createComponent(): JComponent {
        panel = panel {
            row {
                checkBox("Use relative paths from project root")
                    .bindSelected(settings::useRelativePath)
                    .comment("Show src/main/kotlin/MyClass.kt instead of just MyClass.kt")
            }
            
            row {
                checkBox("Hide standard 'Copy Problem Description' action")
                    .bindSelected(settings::hideStandardCopyAction)
                    .comment("Remove the default copy action from Problems context menu")
            }
            
            separator()
            
            group("Copy Format Templates") {
                collapsibleGroup("Single Problem Format") {
                    row {
                        textArea()
                            .bindText(settings::formatPattern)
                            .columns(COLUMNS_LARGE)
                            .rows(2)
                    }
                    row {
                        label("Available variables: {file}, {description}, {line}")
                            .comment("Default: In file {file} - {description} (Line № {line})")
                    }
                }
                
                collapsibleGroup("Multiple Problems Format") {
                    row("Header template:") {
                        textArea()
                            .bindText(settings::formatPatternMultiple)
                            .columns(COLUMNS_LARGE)
                            .rows(2)
                    }
                    row {
                        label("Variables: {file}, {problems}")
                    }
                    separator()
                    row("Item template:") {
                        textArea()
                            .bindText(settings::formatPatternProblemItem)
                            .columns(COLUMNS_LARGE)
                            .rows(2)
                    }
                    row {
                        label("Variables: {index}, {description}, {line}")
                    }
                }
                
                collapsibleGroup("Preset Examples", false) {
                    row {
                        label("Markdown:").bold()
                    }
                    row {
                        label("Single: - [ ] `{file}:{line}` - {description}")
                    }
                    row {
                        label("Header: ## Problems in {file}")
                    }
                    row {
                        label("Item: - [ ] Line {line}: {description}")
                    }
                    separator()
                    row {
                        label("Jira/Confluence:").bold()
                    }
                    row {
                        label("Single: {code}{file}:{line} - {description}{code}")
                    }
                    row {
                        label("Header: h3. Problems in {file}\\n{problems}")
                    }
                    row {
                        label("Item: * Line {line}: {description}")
                    }
                }
            }
            
            separator()
            
            group("Keyboard Shortcut") {
                row {
                    shortcutLabel = JBLabel(getCurrentShortcutText())
                    shortcutLabel.font = shortcutLabel.font.deriveFont(shortcutLabel.font.style or java.awt.Font.BOLD)
                    cell(shortcutLabel)
                    link("Change shortcut") {
                        // Open keymap settings with our action pre-selected
                        val dialog = EditKeymapsDialog(null, "CopyProblemPlus.CopyProblemWithContext")
                        dialog.show()
                        // Update shortcut label immediately
                        shortcutLabel.text = getCurrentShortcutText()
                    }
                }
            }
            
            separator()
            
            row {
                button("Reset All Settings") {
                    resetToDefaults()
                }.align(AlignX.RIGHT)
            }
        }
        
        return panel
    }
    
    private fun resetToDefaults() {
        val result = Messages.showYesNoDialog(
            "Are you sure you want to reset all settings to default values?\nThis will also reset the keyboard shortcut to Ctrl+Alt+P.",
            "Reset Settings",
            Messages.getQuestionIcon()
        )
        
        if (result == Messages.YES) {
            settings.useRelativePath = false
            settings.formatPattern = CopyProblemPlusSettings.DEFAULT_FORMAT_PATTERN
            settings.formatPatternMultiple = CopyProblemPlusSettings.DEFAULT_FORMAT_PATTERN_MULTIPLE
            settings.formatPatternProblemItem = CopyProblemPlusSettings.DEFAULT_FORMAT_PATTERN_PROBLEM_ITEM
            settings.hideStandardCopyAction = false
            
            // Reset keyboard shortcut to default
            resetKeyboardShortcut()
            
            // Update UI
            panel.reset()
            shortcutLabel.text = getCurrentShortcutText()
        }
    }
    
    private fun resetKeyboardShortcut() {
        val keymap = KeymapManager.getInstance().activeKeymap
        val actionId = "CopyProblemPlus.CopyProblemWithContext"
        
        // Remove all current shortcuts
        keymap.removeAllActionShortcuts(actionId)
        
        // Add default shortcut (Ctrl+Alt+P)
        val defaultShortcut = KeyboardShortcut(
            KeyStroke.getKeyStroke("ctrl alt P"),
            null
        )
        keymap.addShortcut(actionId, defaultShortcut)
    }
    
    override fun isModified(): Boolean {
        return panel.isModified()
    }
    
    override fun apply() {
        panel.apply()
        // Notify listeners about settings change
        ApplicationManager.getApplication().messageBus
            .syncPublisher(CopyProblemPlusSettings.SETTINGS_CHANGED_TOPIC)
            .run()
    }
    
    override fun reset() {
        panel.reset()
    }
    
    private fun getCurrentShortcutText(): String {
        val shortcuts = getCurrentShortcut()
        return if (shortcuts.contains(",")) {
            "Current shortcuts: $shortcuts"
        } else {
            "Current shortcut: $shortcuts"
        }
    }
    
    private fun getCurrentShortcut(): String {
        val keymap = KeymapManager.getInstance().activeKeymap
        val shortcuts = keymap.getShortcuts("CopyProblemPlus.CopyProblemWithContext")
        
        if (shortcuts.isEmpty()) {
            return "Not set"
        }
        
        // Get all keyboard shortcuts
        val keyboardShortcuts = shortcuts.filterIsInstance<KeyboardShortcut>()
        if (keyboardShortcuts.isEmpty()) {
            return "Not set"
        }
        
        // Format all shortcuts
        val shortcutTexts = keyboardShortcuts.map { shortcut ->
            val firstKeyStroke = getKeystrokeText(shortcut.firstKeyStroke)
            val secondKeyStroke = shortcut.secondKeyStroke?.let { " " + getKeystrokeText(it) } ?: ""
            firstKeyStroke + secondKeyStroke
        }
        
        return shortcutTexts.joinToString(", ")
    }
    
    private fun getKeystrokeText(keystroke: KeyStroke): String {
        val modifiers = mutableListOf<String>()
        val modifierMask = keystroke.modifiers
        
        if ((modifierMask and java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
            modifiers.add("Ctrl")
        }
        if ((modifierMask and java.awt.event.InputEvent.ALT_DOWN_MASK) != 0) {
            modifiers.add("Alt")
        }
        if ((modifierMask and java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) {
            modifiers.add("Shift")
        }
        if ((modifierMask and java.awt.event.InputEvent.META_DOWN_MASK) != 0) {
            modifiers.add(if (System.getProperty("os.name").contains("Mac")) "Cmd" else "Meta")
        }
        
        val keyText = java.awt.event.KeyEvent.getKeyText(keystroke.keyCode)
        modifiers.add(keyText)
        
        return modifiers.joinToString("+")
    }
}