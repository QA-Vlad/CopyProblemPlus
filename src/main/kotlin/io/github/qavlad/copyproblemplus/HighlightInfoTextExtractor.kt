package io.github.qavlad.copyproblemplus

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.diagnostic.logger
import org.jsoup.Jsoup

class HighlightInfoTextExtractor {
    
    companion object {
        private val LOG = logger<HighlightInfoTextExtractor>()
    }
    
    fun extractText(highlightInfo: HighlightInfo): String {
        LOG.info("=== Extracting text from HighlightInfo ===")
        LOG.info("HighlightInfo class: ${highlightInfo.javaClass.name}")
        LOG.info("Severity: ${highlightInfo.severity}")
        
        // Проверяем все доступные поля через рефлексию для диагностики
        try {
            val fields = highlightInfo.javaClass.declaredFields
            LOG.info("Available fields in HighlightInfo:")
            for (field in fields) {
                try {
                    field.isAccessible = true
                    val value = field.get(highlightInfo)
                    if (value != null && value.toString().isNotEmpty()) {
                        LOG.info("  ${field.name}: ${value.toString().take(200)}")
                    }
                } catch (_: Exception) {
                    // Игнорируем недоступные поля
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to inspect fields: ${e.message}")
        }
        
        // Приоритет 1: description (обычно чистый текст)
        highlightInfo.description?.let { desc ->
            LOG.info("Found description: $desc")
            return cleanText(desc)
        }
        LOG.info("Description is null")
        
        // Приоритет 2: toolTip (может быть HTML)
        highlightInfo.toolTip?.let { tooltip ->
            LOG.info("Found tooltip (first 500 chars): ${tooltip.take(500)}")
            val extracted = extractFromHtml(tooltip)
            LOG.info("Extracted from tooltip: $extracted")
            return extracted
        }
        LOG.info("ToolTip is null")
        
        // Приоритет 3: text (сырой текст проблемы)
        val textField = try {
            val field = HighlightInfo::class.java.getDeclaredField("text")
            field.isAccessible = true
            field.get(highlightInfo) as? String
        } catch (e: Exception) {
            LOG.warn("Failed to access text field: ${e.message}")
            null
        }
        
        textField?.let { text ->
            LOG.info("Found text field: $text")
            return cleanText(text)
        }
        LOG.info("Text field is null")
        
        // Если ничего не нашли
        LOG.warn("No text found in HighlightInfo - returning 'Problem'")
        return "Problem"
    }
    
    private fun extractFromHtml(html: String): String {
        LOG.info("Extracting from HTML, length: ${html.length}")
        LOG.info("HTML content (first 500 chars): ${html.take(500)}")
        
        return try {
            // Парсим HTML и извлекаем текст
            val doc = Jsoup.parse(html)
            
            // Логируем структуру документа
            LOG.info("Document title: ${doc.title()}")
            
            // Пробуем разные способы извлечения текста
            val bodyText = doc.body().text() ?: ""
            val wholeText = doc.text()
            val allText = doc.wholeText()
            
            LOG.info("Body text: $bodyText")
            LOG.info("Whole text: $wholeText")
            LOG.info("All text: $allText")
            
            // Ищем элементы с классом problem или error
            val problemElements = doc.select(".problem, .error, .warning")
            if (problemElements.isNotEmpty()) {
                LOG.info("Found problem elements: ${problemElements.size}")
                problemElements.forEach { element ->
                    LOG.info("  Element: ${element.text()}")
                }
            }
            
            // Выбираем самый информативный текст
            val text = when {
                bodyText.isNotEmpty() && bodyText != "Problem" -> bodyText
                wholeText.isNotEmpty() && wholeText != "Problem" -> wholeText
                allText.isNotEmpty() && allText != "Problem" -> allText
                else -> ""
            }
            
            // Убираем лишнее (например, "Problem" в начале)
            val cleanedText = text
                .replace("^Problem\\s*".toRegex(), "")
                .trim()
            
            LOG.info("Final extracted from HTML: $cleanedText")

            cleanedText.ifEmpty {
                LOG.info("Cleaned text is empty, falling back to regex")
                extractTextWithRegex(html)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse HTML: ${e.message}")
            // Если парсинг не удался, пытаемся извлечь текст регулярками
            extractTextWithRegex(html)
        }
    }
    
    private fun extractTextWithRegex(html: String): String {
        LOG.info("Fallback to regex extraction")
        LOG.info("Original HTML for regex (first 300 chars): ${html.take(300)}")
        
        // Удаляем HTML теги
        val textWithoutTags = html.replace("<[^>]*>".toRegex(), " ")
        LOG.info("After removing tags: $textWithoutTags")
        
        // Декодируем HTML entities
        val decoded = textWithoutTags
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
        
        LOG.info("After decoding entities: $decoded")
        
        // Убираем множественные пробелы
        val cleaned = decoded.replace("\\s+".toRegex(), " ").trim()
        
        LOG.info("After cleaning spaces: $cleaned")
        
        // Убираем "Problem" в начале если есть
        val result = cleaned.replace("^Problem\\s*".toRegex(), "").trim()
        
        LOG.info("Final result after removing 'Problem': $result")
        
        return result.ifEmpty { "Problem" }
    }
    
    private fun cleanText(text: String): String {
        return text.trim()
    }
}