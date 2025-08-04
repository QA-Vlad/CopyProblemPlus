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

class SimpleCopyLineAction : AnAction() {
    
    companion object {
        private val LOG = logger<SimpleCopyLineAction>()
    }
    
    override fun actionPerformed(event: AnActionEvent) {
        try {
            LOG.info("SimpleCopyLineAction started")
            
            val editor = event.getData(CommonDataKeys.EDITOR)
            val project = event.getData(CommonDataKeys.PROJECT)
            val psiFile = event.getData(CommonDataKeys.PSI_FILE)
            
            if (editor == null || project == null || psiFile == null) {
                LOG.warn("Missing required data: editor=$editor, project=$project, psiFile=$psiFile")
                showNotification(
                    project ?: return,
                    "Error",
                    "Cannot get editor context",
                    NotificationType.ERROR
                )
                return
            }
            
            val document = editor.document
            val caretModel = editor.caretModel
            val caretOffset = caretModel.offset
            val lineNumber = document.getLineNumber(caretOffset) + 1
            
            // Получаем текст текущей строки
            val lineStartOffset = document.getLineStartOffset(lineNumber - 1)
            val lineEndOffset = document.getLineEndOffset(lineNumber - 1)
            val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, lineEndOffset)).trim()
            
            // Форматируем текст
            val formattedText = "In file ${psiFile.name} - $lineText (Line № $lineNumber)"
            
            // Копируем в буфер обмена
            val stringSelection = StringSelection(formattedText)
            CopyPasteManager.getInstance().setContents(stringSelection)
            
            LOG.info("Copied to clipboard: $formattedText")
            
            showNotification(
                project,
                "Line copied",
                formattedText,
                NotificationType.INFORMATION
            )
            
        } catch (e: Exception) {
            LOG.error("Error in SimpleCopyLineAction", e)
            event.project?.let { project ->
                showNotification(
                    project,
                    "Error",
                    "Failed to copy: ${e.message}",
                    NotificationType.ERROR
                )
            }
        }
    }
    
    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val project = event.getData(CommonDataKeys.PROJECT)
        
        val isEnabled = editor != null && project != null
        event.presentation.isEnabled = isEnabled
        event.presentation.isVisible = true
        
        LOG.info("SimpleCopyLineAction.update called, isEnabled=$isEnabled, isVisible=true")
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    private fun showNotification(
        project: com.intellij.openapi.project.Project,
        title: String,
        content: String,
        type: NotificationType
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CopyProblemPlus")
            .createNotification(title, content, type)
            .notify(project)
    }
}