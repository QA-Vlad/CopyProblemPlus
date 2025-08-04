package io.github.qavlad.copyproblemplus

import io.github.qavlad.copyproblemplus.settings.CopyProblemPlusSettings

class ProblemTextFormatter {
    
    private val settings = CopyProblemPlusSettings.getInstance()
    
    fun format(problemInfo: ProblemInfoExtractor.ProblemInfo): String {
        return settings.formatPattern
            .replace("{file}", problemInfo.fileName)
            .replace("{description}", problemInfo.description)
            .replace("{line}", problemInfo.lineNumber.toString())
    }
}