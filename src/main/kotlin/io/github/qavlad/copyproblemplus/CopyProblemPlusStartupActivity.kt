package io.github.qavlad.copyproblemplus

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CopyProblemPlusStartupActivity : ProjectActivity {
    
    companion object {
        private val LOG = logger<CopyProblemPlusStartupActivity>()
    }
    
    override suspend fun execute(project: Project) {
        LOG.info("CopyProblemPlus startup activity started for project: ${project.name}")
        
        try {
            val actionManager = ActionManager.getInstance()
            
            // Пытаемся найти группу ProblemsView
            val problemsViewGroup = actionManager.getAction("ProblemsView.ToolWindow.TreePopup") as? DefaultActionGroup
            if (problemsViewGroup != null) {
                LOG.info("Found ProblemsView.ToolWindow.TreePopup group")
                
                val copyProblemAction = actionManager.getAction("CopyProblemPlus.CopyProblemWithContext")
                if (copyProblemAction != null) {
                    if (!problemsViewGroup.containsAction(copyProblemAction)) {
                        problemsViewGroup.add(copyProblemAction)
                        LOG.info("Added CopyProblemWithContext to ProblemsView popup menu")
                    } else {
                        LOG.info("CopyProblemWithContext already in ProblemsView popup menu")
                    }
                } else {
                    LOG.warn("CopyProblemWithContext action not found")
                }
            } else {
                LOG.warn("ProblemsView.ToolWindow.TreePopup group not found")
            }
            
            // Логируем все зарегистрированные действия
            val allActionIds = actionManager.getActionIdList("CopyProblemPlus")
            LOG.info("All registered CopyProblemPlus actions: ${allActionIds.joinToString(", ")}")
            
            // Логируем доступные группы для отладки
            val allGroups = actionManager.getActionIdList("")
            val problemGroups = allGroups.filter { it.contains("Problem", ignoreCase = true) }
            LOG.info("Available problem-related groups: ${problemGroups.take(10).joinToString(", ")}")
            
            LOG.info("CopyProblemPlus startup activity completed")
        } catch (e: Exception) {
            LOG.error("Error in CopyProblemPlus startup activity", e)
        }
    }
}