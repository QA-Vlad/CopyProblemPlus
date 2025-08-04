plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "io.github.qavlad"
version = "1.0.0"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2023.3.8")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf())
    // Используем постоянную песочницу для сохранения настроек между запусками
    sandboxDir.set("${project.projectDir}/sandbox-persistent")
}


tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("")  // Пустая строка убирает ограничение
    }
    
    buildSearchableOptions {
        enabled = false
    }
    
    test {
        enabled = false
    }
}
