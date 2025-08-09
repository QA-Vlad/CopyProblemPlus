package io.github.qavlad.copyproblemplus

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class DaemonAnalyzerProblemExtractor {
    
    companion object {
        private val LOG = logger<DaemonAnalyzerProblemExtractor>()
    }
    
    private val highlightTextExtractor = HighlightInfoTextExtractor()
    
    fun extractProblemAtCaret(
        editor: Editor,
        psiFile: PsiFile,
        project: Project
    ): ProblemInfoExtractor.ProblemInfo? {
        val caretOffset = editor.caretModel.offset
        val document = editor.document
        val lineNumber = document.getLineNumber(caretOffset) + 1
        
        LOG.info("=== Using DaemonCodeAnalyzer to extract problem at offset $caretOffset, line $lineNumber ===")
        
        val analyzer = DaemonCodeAnalyzer.getInstance(project)
        if (analyzer !is DaemonCodeAnalyzerImpl) {
            LOG.warn("DaemonCodeAnalyzer is not DaemonCodeAnalyzerImpl")
            return null
        }
        
        try {
            // Получаем все highlights для файла
            val highlightInfos = analyzer.getFileLevelHighlights(project, psiFile)
            LOG.info("Found ${highlightInfos.size} file level highlights")
            
            // Ищем highlights в позиции курсора
            for (info in highlightInfos) {
                if (info.startOffset <= caretOffset && caretOffset <= info.endOffset) {
                    LOG.info("Found matching highlight at cursor position")
                    LOG.info("  Type: ${info.type}")
                    LOG.info("  Severity: ${info.severity}")
                    LOG.info("  Description: ${info.description}")
                    LOG.info("  ToolTip: ${info.toolTip?.take(200)}")
                    
                    val description = highlightTextExtractor.extractText(info)
                    val lineNum = document.getLineNumber(info.startOffset) + 1
                    
                    return ProblemInfoExtractor.ProblemInfo(
                        description = description,
                        fileName = psiFile.name,
                        lineNumber = lineNum,
                        severity = ProblemInfoExtractor().getSeverityString(info.severity)
                    )
                }
            }
            
            // Если не нашли точное совпадение, ищем ближайший
            val nearbyHighlights = highlightInfos.filter { info ->
                val distance = when {
                    info.startOffset <= caretOffset && caretOffset <= info.endOffset -> 0
                    info.endOffset < caretOffset -> caretOffset - info.endOffset
                    else -> info.startOffset - caretOffset
                }
                distance < 50 // В пределах 50 символов
            }.sortedBy { info ->
                val distanceToStart = kotlin.math.abs(info.startOffset - caretOffset)
                val distanceToEnd = kotlin.math.abs(info.endOffset - caretOffset)
                kotlin.math.min(distanceToStart, distanceToEnd)
            }
            
            LOG.info("Found ${nearbyHighlights.size} nearby highlights")
            
            if (nearbyHighlights.isNotEmpty()) {
                val info = nearbyHighlights.first()
                LOG.info("Using nearest highlight:")
                LOG.info("  Type: ${info.type}")
                LOG.info("  Severity: ${info.severity}")
                LOG.info("  Description: ${info.description}")
                LOG.info("  ToolTip: ${info.toolTip?.take(200)}")
                
                val description = highlightTextExtractor.extractText(info)
                val lineNum = document.getLineNumber(info.startOffset) + 1
                
                return ProblemInfoExtractor.ProblemInfo(
                    description = description,
                    fileName = psiFile.name,
                    lineNumber = lineNum,
                    severity = ProblemInfoExtractor().getSeverityString(info.severity)
                )
            }
            
        } catch (e: Exception) {
            LOG.error("Failed to get highlights from DaemonCodeAnalyzer", e)
        }
        
        LOG.info("No problem found using DaemonCodeAnalyzer")
        return null
    }
}