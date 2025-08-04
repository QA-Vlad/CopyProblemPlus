package io.github.qavlad.copyproblemplus

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import io.github.qavlad.copyproblemplus.settings.CopyProblemPlusSettings

class CopyProblemPlusActionCustomizer : ProjectActivity {
    
    companion object {
        private val LOG = logger<CopyProblemPlusActionCustomizer>()
        private const val STANDARD_COPY_ACTION_ID = "ProblemDescriptionAction"
        private const val ALT_COPY_ACTION_ID = "CopyProblemDescription"
        private const val PROBLEMS_POPUP_GROUP = "ProblemsView.ToolWindow.TreePopup"
        private const val STANDARD_ACTION_TEXT = "Copy Problem Description"
    }
    
    private val removedActions = mutableMapOf<String, AnAction>()
    
    override suspend fun execute(project: Project) {
        LOG.info("CopyProblemPlusActionCustomizer started")
        
        // Listen for settings changes
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(CopyProblemPlusSettings.SETTINGS_CHANGED_TOPIC, Runnable {
                updateStandardActionVisibility()
            })
        
        // Apply initial state
        ApplicationManager.getApplication().invokeLater {
            updateStandardActionVisibility()
        }
    }
    
    private fun updateStandardActionVisibility() {
        // This method tries two approaches to find and hide the standard copy action:
        // 1. First, try to find by known action IDs (ProblemDescriptionAction, CopyProblemDescription)
        // 2. If not found by ID, use alternative approach to find by text "Copy Problem Description"
        // This dual approach ensures compatibility across different IDE versions
        
        val settings = CopyProblemPlusSettings.getInstance()
        val actionManager = ActionManager.getInstance()
        
        try {
            // Find the Problems popup group
            val group = actionManager.getAction(PROBLEMS_POPUP_GROUP) as? DefaultActionGroup
            if (group == null) {
                LOG.warn("Problems popup group not found")
                return
            }
            
            // Find the standard copy action - try multiple possible IDs
            var standardAction = actionManager.getAction(STANDARD_COPY_ACTION_ID)
            if (standardAction == null) {
                standardAction = actionManager.getAction(ALT_COPY_ACTION_ID)
            }
            
            if (standardAction == null) {
                // Log all available actions in the group to find the correct ID
                LOG.info("Standard copy action not found by ID. Will try alternative approach by text. Available actions:")
                val children = group.getChildren(null)
                children.forEach { action ->
                    val id = actionManager.getId(action) ?: "unknown"
                    val text = action.templatePresentation.text ?: "no text"
                    LOG.info("Action: id='$id', text='$text', class=${action.javaClass.simpleName}")
                }
                
                // Since we can't find the standard action by ID, continue with alternative approach
                LOG.debug("Continuing with alternative approach to find action by text...")
            }
            
            if (standardAction != null && settings.hideStandardCopyAction) {
                // Remove standard action from group
                val children = group.getChildren(null)
                if (children.contains(standardAction)) {
                    group.remove(standardAction)
                    LOG.info("Removed standard copy action from Problems popup")
                }
            } else if (standardAction != null) {
                // Add it back if it was removed
                val children = group.getChildren(null)
                if (!children.contains(standardAction)) {
                    // Find our action to add standard action before it
                    val ourAction = actionManager.getAction("CopyProblemPlus.CopyProblemWithContext")
                    if (ourAction != null) {
                        // Add after our action
                        group.addAction(standardAction, Constraints(Anchor.AFTER, "CopyProblemPlus.CopyProblemWithContext"))
                    } else {
                        group.add(standardAction)
                    }
                    LOG.info("Added standard copy action back to Problems popup")
                }
            }
            
            // Alternative approach: find and hide action by text
            if (settings.hideStandardCopyAction) {
                val children = group.getChildren(null)
                children.forEach { action ->
                    val id = actionManager.getId(action) ?: "unknown"
                    val text = action.templatePresentation.text ?: ""
                    
                    // Look for exact text match
                    if (text == STANDARD_ACTION_TEXT) {
                        LOG.info("Alternative approach succeeded! Found standard action by text: id='$id', removing it")
                        group.remove(action)
                        // Store the removed action so we can restore it later
                        removedActions[id] = action
                    }
                }
            } else {
                // Restore any previously removed actions
                removedActions.forEach { (id, action) ->
                    val children = group.getChildren(null)
                    if (!children.contains(action)) {
                        // Find our action to add standard action after it
                        val ourAction = actionManager.getAction("CopyProblemPlus.CopyProblemWithContext")
                        if (ourAction != null && children.contains(ourAction)) {
                            // Get the index of our action
                            val ourIndex = children.indexOf(ourAction)
                            if (ourIndex >= 0 && ourIndex < children.size - 1) {
                                // Add after our action
                                val nextAction = children[ourIndex + 1]
                                val nextId = actionManager.getId(nextAction)
                                if (nextId != null) {
                                    group.add(action, Constraints(Anchor.BEFORE, nextId))
                                } else {
                                    group.add(action)
                                }
                            } else {
                                group.add(action)
                            }
                        } else {
                            group.add(action)
                        }
                        LOG.info("Restored action: $id")
                    }
                }
                removedActions.clear()
            }
            
        } catch (e: Exception) {
            LOG.error("Error updating standard action visibility", e)
        }
    }
}