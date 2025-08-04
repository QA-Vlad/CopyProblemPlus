package io.github.qavlad.copyproblemplus

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

class CopyProblemAction : AnAction() {
    
    companion object {
        private val LOG = logger<CopyProblemAction>()
    }
    
    private val problemExtractor = ProblemInfoExtractor()
    private val alternativeExtractor = AlternativeProblemExtractor()
    private val textFormatter = ProblemTextFormatter()
    
    override fun actionPerformed(event: AnActionEvent) {
        LOG.info("CopyProblemAction started")
        
        val editor = event.getData(CommonDataKeys.EDITOR) ?: run {
            LOG.warn("No editor found")
            return
        }
        val project = event.getData(CommonDataKeys.PROJECT) ?: run {
            LOG.warn("No project found")
            return
        }
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: run {
            LOG.warn("No PSI file found")
            return
        }
        
        LOG.info("Extracting problem at caret position: ${editor.caretModel.offset}")
        var problemInfo = problemExtractor.extractProblemAtCaret(editor, psiFile)
        
        if (problemInfo == null) {
            LOG.info("Primary extractor failed, trying alternative method")
            problemInfo = alternativeExtractor.extractProblemAtCaret(editor, psiFile)
        }
        
        if (problemInfo != null) {
            LOG.info("Problem found: $problemInfo")
            val formattedText = textFormatter.format(problemInfo)
            copyToClipboard(formattedText)
            LOG.info("Copied to clipboard: $formattedText")
        } else {
            LOG.warn("No problem found at caret position")
            showNotification(project)
        }
    }
    
    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val project = event.getData(CommonDataKeys.PROJECT)
        
        event.presentation.isEnabled = editor != null && project != null
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    private fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        CopyPasteManager.getInstance().setContents(stringSelection)
    }
    
    private fun showNotification(project: com.intellij.openapi.project.Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CopyProblemPlus")
            .createNotification(
                "No problem found",
                "Place cursor on a warning or error and try again",
                NotificationType.WARNING
            )
            .notify(project)
    }
}