package io.github.qavlad.copyproblemplus


import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class ProblemInfoExtractor {
    
    companion object {
        private val LOG = logger<ProblemInfoExtractor>()
    }
    
    private val highlightTextExtractor = HighlightInfoTextExtractor()
    
    data class ProblemInfo(
        val description: String,
        val fileName: String,
        val lineNumber: Int,
        val severity: String
    )
    
    fun extractProblemAtCaret(
        editor: Editor,
        psiFile: PsiFile
    ): ProblemInfo? {
        val caretOffset = editor.caretModel.offset
        val document = editor.document
        
        // Попытка 1: Проверить через текст под курсором, есть ли проблема
        document.getLineNumber(caretOffset)

        // Попытка 2: Получить информацию через MarkupModel
        val markupModel = editor.markupModel
        val rangeHighlighters = markupModel.allHighlighters
        LOG.info("Found ${rangeHighlighters.size} highlighters in the document")
        
        for (highlighter in rangeHighlighters) {
            if (highlighter.startOffset <= caretOffset && caretOffset <= highlighter.endOffset) {
                val errorStripeTooltip = highlighter.errorStripeTooltip
                LOG.info("Found highlighter at range [${highlighter.startOffset}, ${highlighter.endOffset}], tooltip type: ${errorStripeTooltip?.javaClass?.simpleName}")
                
                when (errorStripeTooltip) {
                    is HighlightInfo -> {
                        LOG.info("Processing HighlightInfo tooltip")
                        return createProblemInfo(errorStripeTooltip, psiFile, document)
                    }
                    is String -> {
                        LOG.info("Processing String tooltip: $errorStripeTooltip")
                        val lineNumber = document.getLineNumber(highlighter.startOffset) + 1
                        return ProblemInfo(
                            description = errorStripeTooltip,
                            fileName = psiFile.name,
                            lineNumber = lineNumber,
                            severity = "Warning"
                        )
                    }
                    else -> {
                        LOG.info("Unknown tooltip type: ${errorStripeTooltip?.javaClass?.name}")
                        LOG.info("Tooltip content: $errorStripeTooltip")
                    }
                }
            }
        }
        
        return null
    }
    
    private fun createProblemInfo(
        highlightInfo: HighlightInfo,
        psiFile: PsiFile,
        document: com.intellij.openapi.editor.Document
    ): ProblemInfo {
        val lineNumber = document.getLineNumber(highlightInfo.startOffset) + 1
        val severity = getSeverityString(highlightInfo.severity)
        val description = highlightTextExtractor.extractText(highlightInfo)
        
        return ProblemInfo(
            description = description,
            fileName = psiFile.name,
            lineNumber = lineNumber,
            severity = severity
        )
    }
    
    fun getSeverityString(severity: HighlightSeverity): String {
        return when (severity) {
            HighlightSeverity.ERROR -> "Error"
            HighlightSeverity.WARNING -> "Warning"
            HighlightSeverity.WEAK_WARNING -> "Weak Warning"
            HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING -> "Server Problem"
            else -> "Info"
        }
    }
}