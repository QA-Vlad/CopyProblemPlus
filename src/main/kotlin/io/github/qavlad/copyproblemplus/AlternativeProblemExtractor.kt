package io.github.qavlad.copyproblemplus

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class AlternativeProblemExtractor {
    
    companion object {
        private val LOG = logger<AlternativeProblemExtractor>()
    }
    
    private val highlightTextExtractor = HighlightInfoTextExtractor()
    
    fun extractProblemAtCaret(
        editor: Editor,
        psiFile: PsiFile
    ): ProblemInfoExtractor.ProblemInfo? {
        val caretOffset = editor.caretModel.offset
        val document = editor.document
        val lineNumber = document.getLineNumber(caretOffset) + 1
        
        LOG.info("Trying alternative extraction at offset $caretOffset, line $lineNumber")
        
        // Попробуем получить информацию через выделенный текст
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            val selectedText = selectionModel.selectedText ?: ""
            LOG.info("Selection found: $selectedText")
            
            return ProblemInfoExtractor.ProblemInfo(
                description = "Selected text: $selectedText",
                fileName = psiFile.name,
                lineNumber = lineNumber,
                severity = "Info"
            )
        }
        
        // Получим текст текущей строки
        val lineStartOffset = document.getLineStartOffset(lineNumber - 1)
        val lineEndOffset = document.getLineEndOffset(lineNumber - 1)
        val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, lineEndOffset)).trim()
        
        LOG.info("Current line text: $lineText")
        
        // Если строка содержит типичные паттерны варнингов
        if (lineText.contains("unused", ignoreCase = true) || 
            lineText.contains("never used", ignoreCase = true) ||
            lineText.contains("deprecated", ignoreCase = true)) {
            
            return ProblemInfoExtractor.ProblemInfo(
                description = "Potential issue in line: $lineText",
                fileName = psiFile.name,
                lineNumber = lineNumber,
                severity = "Warning"
            )
        }
        
        // Проверим, есть ли вообще какие-то маркеры в редакторе
        val markupModel = editor.markupModel
        val allHighlighters = markupModel.allHighlighters
        LOG.info("Total highlighters in document: ${allHighlighters.size}")
        
        // Найдём ближайший highlighter к позиции курсора
        val nearbyHighlighters = allHighlighters.filter { highlighter ->
            val distance = when {
                highlighter.startOffset <= caretOffset && caretOffset <= highlighter.endOffset -> 0
                highlighter.endOffset < caretOffset -> caretOffset - highlighter.endOffset
                else -> highlighter.startOffset - caretOffset
            }
            distance < 100 // В пределах 100 символов
        }.sortedBy { highlighter ->
            val distanceToStart = kotlin.math.abs(highlighter.startOffset - caretOffset)
            val distanceToEnd = kotlin.math.abs(highlighter.endOffset - caretOffset)
            kotlin.math.min(distanceToStart, distanceToEnd)
        }
        
        LOG.info("Found ${nearbyHighlighters.size} nearby highlighters")
        
        for (highlighter in nearbyHighlighters) {
            val tooltip = highlighter.errorStripeTooltip
            if (tooltip != null) {
                val highlighterLine = document.getLineNumber(highlighter.startOffset) + 1
                val text = when (tooltip) {
                    is HighlightInfo -> highlightTextExtractor.extractText(tooltip)
                    is String -> tooltip
                    else -> tooltip.toString()
                }
                
                LOG.info("Found highlighter with tooltip: $text")
                
                return ProblemInfoExtractor.ProblemInfo(
                    description = text,
                    fileName = psiFile.name,
                    lineNumber = highlighterLine,
                    severity = "Warning"
                )
            }
        }
        
        LOG.warn("No problems found using alternative method")
        return null
    }
}