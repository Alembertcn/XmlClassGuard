package com.xml.guard.tasks

import com.xml.guard.entensions.GuardExtension
import com.xml.guard.model.MappingParser
import com.xml.guard.utils.isAndroidProject
import com.xml.guard.utils.proguardFile
import com.xml.guard.utils.replaceWords
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * @author Hai
 * @date   2025/1/6 12:28
 * @desc 修改替换 proguardFile
 */
open class ReplaceGuardFile @Inject constructor(private val guardExtension:GuardExtension):DefaultTask() {
    init {
        group = "guard"
    }
    private val mappingFile = guardExtension.mappingFile ?: project.file("xml-class-mapping.txt")
    private val mapping = MappingParser.parse(mappingFile)

    @TaskAction
    fun execute(){
        val classMapping = mapping.classMapping
        val dirMapping = mapping.dirMapping
        val proguardFile = project.proguardFile()
        if (classMapping.isEmpty() || !project.isAndroidProject() || !proguardFile.exists()) return
        val moveDir = guardExtension.moveDir
        var readText = proguardFile.readText()
        // 替换classMapping
        classMapping.entries.forEach {
            readText=readText.replaceWords(it.key,it.value)
        }
        // 替换divMapping
        dirMapping.entries.forEach {
            readText=readText.replaceWords("${it.key}.**","${it.value}.**")
        }
        // 替换moveDir
        moveDir.entries.forEach {
            readText=readText.replaceWords(it.key, it.value)
        }
        proguardFile.writeText(readText)
    }
}