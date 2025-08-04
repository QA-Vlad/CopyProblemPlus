package io.github.qavlad.copyproblemplus.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "CopyProblemPlusSettings",
    storages = [Storage("CopyProblemPlusSettings.xml")]
)
class CopyProblemPlusSettings : PersistentStateComponent<CopyProblemPlusSettings> {
    
    var useRelativePath: Boolean = false
    var formatPattern: String = "In file {file} - {description} (Line № {line})"
    var formatPatternMultiple: String = "Problems in file {file}:\n{problems}"
    var formatPatternProblemItem: String = "{index}. {description} (Line № {line})"
    var hideStandardCopyAction: Boolean = false
    
    companion object {
        const val DEFAULT_FORMAT_PATTERN = "In file {file} - {description} (Line № {line})"
        const val DEFAULT_FORMAT_PATTERN_MULTIPLE = "Problems in file {file}:\n{problems}"
        const val DEFAULT_FORMAT_PATTERN_PROBLEM_ITEM = "{index}. {description} (Line № {line})"
        
        val SETTINGS_CHANGED_TOPIC = Topic.create("CopyProblemPlusSettingsChanged", Runnable::class.java)
        
        fun getInstance(): CopyProblemPlusSettings {
            return ApplicationManager.getApplication().getService(CopyProblemPlusSettings::class.java)
        }
    }
    
    override fun getState(): CopyProblemPlusSettings {
        return this
    }
    
    override fun loadState(state: CopyProblemPlusSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}