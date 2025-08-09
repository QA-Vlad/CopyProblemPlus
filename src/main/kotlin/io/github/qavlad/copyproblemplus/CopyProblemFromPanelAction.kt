package io.github.qavlad.copyproblemplus

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import io.github.qavlad.copyproblemplus.settings.CopyProblemPlusSettings
import java.awt.datatransfer.StringSelection
import javax.swing.JTree

class CopyProblemFromPanelAction : AnAction() {
    
    companion object {
        private val LOG = logger<CopyProblemFromPanelAction>()
    }
    
    override fun actionPerformed(event: AnActionEvent) {
        LOG.info("CopyProblemFromPanelAction started")
        
        val project = event.project
        if (project == null) {
            LOG.warn("No project found")
            return
        }
        
        try {
            // Получаем данные из контекста
            val dataContext = event.dataContext
            
            // Пытаемся получить различные данные из контекста
            val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
            val navigatable = event.getData(CommonDataKeys.NAVIGATABLE)
            val psiElement = event.getData(CommonDataKeys.PSI_ELEMENT)
            
            LOG.info("Available data - VirtualFile: $virtualFile, Navigatable: $navigatable, PsiElement: $psiElement")
            
            // Пробуем получить описание проблемы из разных источников
            var problemDescription: String? = null
            var fileName: String? = virtualFile?.name
            var lineNumber: Int? = null
            var isMultipleProblems = false
            
            // Получаем настройки
            val settings = CopyProblemPlusSettings.getInstance()
            
            // Если включен относительный путь, получаем его
            if (settings.useRelativePath && virtualFile != null) {
                LOG.info("Use relative path enabled. VirtualFile path: ${virtualFile.path}, Project base path: ${project.basePath}")
                fileName = project.basePath?.let { basePath ->
                    val relativePath = virtualFile.path.removePrefix(basePath).removePrefix("/").removePrefix("\\")
                    LOG.info("Calculated relative path: $relativePath")
                    relativePath
                } ?: virtualFile.name
            }
            
            // Пытаемся получить все доступные данные из контекста
            LOG.info("Trying to get all available data from context...")
            
            // Пытаемся получить компонент дерева или выбранный узел
            val component = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT)
            val selectedValues = dataContext.getData(PlatformDataKeys.SELECTED_ITEMS)
            
            LOG.info("Component: $component")
            LOG.info("Selected values: ${selectedValues?.contentToString()}")
            
            // Если компонент это JTree, пытаемся получить выбранный узел
            if (component is JTree) {
                val selectedPath = component.selectionPath
                val selectedNode = selectedPath?.lastPathComponent
                LOG.info("Selected tree node: $selectedNode")
                
                // Пытаемся получить текст из узла
                if (selectedNode != null) {
                    val nodeText = selectedNode.toString()
                    LOG.info("Node text: $nodeText")
                    
                    // Проверяем, является ли выбранный узел файлом с проблемами
                    // Файл - это узел с расширением файла в конце и без дополнительного текста
                    val isFileNode = nodeText.matches("""^[^'"\s]+\.\w{1,5}\s*$""".toRegex())
                    
                    LOG.info("Is file node: $isFileNode")
                    
                    if (isFileNode) {
                        // Это файл с проблемами, нужно собрать все проблемы
                        LOG.info("Selected node is a file with problems, collecting all problems...")
                        
                        // Получаем все дочерние узлы (проблемы)
                        val problemsList = mutableListOf<String>()
                        val model = component.model
                        val childCount = model.getChildCount(selectedNode)
                        
                        for (i in 0 until childCount) {
                            val child = model.getChild(selectedNode, i)
                            val childText = child.toString()
                            LOG.info("Child problem: $childText")
                            LOG.info("Child type: ${child.javaClass.name}")
                            LOG.info("Model type: ${model.javaClass.name}")
                            
                            // Извлекаем номер строки и текст проблемы
                            var lineNum = 0
                            var cleanProblemText = childText
                            
                            // Пробуем получить дополнительную информацию из узла
                            try {
                                // Используем рефлексию для доступа к полям и методам
                                val childClass = child.javaClass
                                LOG.info("Analyzing child class: ${childClass.name}")
                                
                                // Если это ProblemNode, получаем номер строки напрямую из поля
                                if (childClass.name.contains("ProblemNode")) {
                                    try {
                                        val lineField = childClass.getDeclaredField("line")
                                        lineField.isAccessible = true
                                        val lineValue = lineField.get(child)
                                        if (lineValue is Int) {
                                            lineNum = lineValue + 1 // Преобразуем из 0-based в 1-based
                                            LOG.info("Got line from field: $lineNum")
                                        }
                                    } catch (e: Exception) {
                                        LOG.info("Failed to get line field: ${e.message}")
                                    }
                                }
                                
                                // Если номер строки не получен через поле, пробуем метод getLine
                                if (lineNum == 0) {
                                    val getLineMethod = childClass.methods.find { it.name == "getLine" && it.parameterCount == 0 }
                                    if (getLineMethod != null) {
                                        try {
                                            val line = getLineMethod.invoke(child)
                                            if (line is Int) {
                                                lineNum = line + 1 // Преобразуем из 0-based в 1-based
                                                LOG.info("Got line from getLine(): $lineNum")
                                            }
                                        } catch (e: Exception) {
                                            LOG.info("Failed to invoke getLine: ${e.message}")
                                        }
                                    }
                                }
                                
                            } catch (e: Exception) {
                                LOG.info("Error during reflection: ${e.message}")
                                e.printStackTrace()
                            }
                            
                            // Если номер строки все еще не найден, пробуем искать в тексте
                            if (lineNum == 0) {
                                // Ищем паттерн с номером строки в конце (например, "Unused import directive :14")
                                val linePattern = """^(.+?)\s*:\s*(\d+)\s*$""".toRegex()
                                val lineMatch = linePattern.find(childText)
                                
                                if (lineMatch != null) {
                                    cleanProblemText = lineMatch.groupValues[1].trim()
                                    lineNum = lineMatch.groupValues[2].toIntOrNull() ?: 0
                                    LOG.info("Extracted from pattern - problem: '$cleanProblemText', line: $lineNum")
                                }
                            }
                            
                            LOG.info("Final result - problem: '$cleanProblemText', line: $lineNum from original: '$childText'")
                            
                            problemsList.add("${problemsList.size + 1}. $cleanProblemText (Line № $lineNum)")
                        }
                        
                        if (problemsList.isNotEmpty()) {
                            // Используем имя файла из nodeText, если fileName не установлено
                            val fileNameForHeader = fileName ?: nodeText.trim()
                            
                            // Форматируем список проблем с использованием паттернов из настроек
                            val formattedProblems = problemsList.mapIndexed { index, problem ->
                                // Извлекаем описание и номер строки из элемента списка
                                val match = """^\d+\. (.+) \(Line № (\d+)\)$""".toRegex().find(problem)
                                if (match != null) {
                                    val desc = match.groupValues[1]
                                    val line = match.groupValues[2]
                                    settings.formatPatternProblemItem
                                        .replace("{index}", (index + 1).toString())
                                        .replace("{description}", desc)
                                        .replace("{line}", line)
                                } else {
                                    problem
                                }
                            }.joinToString("\n")
                            
                            problemDescription = settings.formatPatternMultiple
                                .replace("{file}", fileNameForHeader)
                                .replace("{problems}", formattedProblems)
                            
                            // Помечаем, что это список проблем
                            isMultipleProblems = true
                            
                            // Для списка проблем не нужен номер строки
                            lineNumber = null
                        }
                    } else if (nodeText.isNotBlank() && !nodeText.contains("file://")) {
                        // Это конкретная проблема
                        problemDescription = nodeText
                        LOG.info("Set problem description from node text: $problemDescription")
                    }
                }
            }
            
            // Если есть navigatable и это OpenFileDescriptor, получаем точную информацию о строке
            if (navigatable is OpenFileDescriptor) {
                lineNumber = navigatable.line + 1 // line is 0-based, we need 1-based
                LOG.info("Got line number from OpenFileDescriptor: $lineNumber")
            }
            
            // Если всё ещё нет описания, пробуем через CopyProvider
            if (problemDescription == null) {
                val copyProvider = dataContext.getData(PlatformDataKeys.COPY_PROVIDER)
                if (copyProvider != null) {
                    LOG.info("Found CopyProvider: $copyProvider")
                    val prevClipboard = CopyPasteManager.getInstance().contents
                    copyProvider.performCopy(dataContext)
                    Thread.sleep(100)
                    
                    val clipboard = CopyPasteManager.getInstance().contents
                    if (clipboard != null && clipboard != prevClipboard && clipboard.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                        problemDescription = clipboard.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
                        LOG.info("Got problem description from CopyProvider: $problemDescription")
                    }
                }
            }
            
            // Если у нас есть хоть какая-то информация, форматируем её
            if (problemDescription != null || fileName != null) {
                val formattedText = if (isMultipleProblems) {
                    // Это список проблем, уже отформатирован
                    problemDescription
                } else {
                    // Это одна проблема, форматируем с использованием паттерна
                    val description = problemDescription ?: "Problem"
                    val file = fileName ?: "Unknown file"
                    val line = lineNumber ?: 1
                    settings.formatPattern
                        .replace("{file}", file)
                        .replace("{description}", description)
                        .replace("{line}", line.toString())
                }
                
                // Копируем в буфер обмена
                val stringSelection = StringSelection(formattedText)
                CopyPasteManager.getInstance().setContents(stringSelection)
                
                LOG.info("Copied to clipboard: $formattedText")
            } else {
                LOG.warn("Could not extract problem information")
                showNotification(
                    project,
                    "No Selection",
                    "Could not extract problem information. Try using the standard 'Copy Problem Description' first.",
                    NotificationType.WARNING
                )
            }
            
        } catch (e: Exception) {
            LOG.error("Error in CopyProblemFromPanelAction", e)
            showNotification(
                project,
                "Error",
                "Failed to copy: ${e.message}",
                NotificationType.ERROR
            )
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        event.presentation.isEnabled = project != null
        event.presentation.isVisible = true
        
        LOG.debug("Update called, project: $project")
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    private fun showNotification(
        project: Project,
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