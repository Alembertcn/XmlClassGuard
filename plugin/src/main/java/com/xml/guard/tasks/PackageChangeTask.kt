package com.xml.guard.tasks

import com.xml.guard.entensions.GuardExtension
import com.xml.guard.utils.allDependencyAndroidProjects
import com.xml.guard.utils.findPackage
import com.xml.guard.utils.findXmlDirs
import com.xml.guard.utils.insertImportXxxIfAbsent
import com.xml.guard.utils.javaDirs
import com.xml.guard.utils.manifestFile
import com.xml.guard.utils.replaceWords
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * User: ljx
 * Date: 2022/2/25
 * Time: 19:06
 */
open class PackageChangeTask @Inject constructor(
    private val guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    @TaskAction
    fun execute() {
        val packageExtension = guardExtension.packageChange
        if (packageExtension.isEmpty()) return
        val androidProjects = allDependencyAndroidProjects()
        androidProjects.forEach {
            if(it.name.startsWith("lib_") || it.name.startsWith("module_") || it.name.startsWith("app")){
                it.changePackage(packageExtension)
            }
        }
    }

    private fun Project.changePackage(map: Map<String, String>) {
        val oldPackage: String = findPackage()
        val newPackage = map[oldPackage]
        val javaDirs = javaDirs(variantName)
        if(newPackage!=null){
            val dirs = findXmlDirs(variantName, "layout")
            dirs.add(manifestFile())
            dirs.add(buildFile)
            //1、修改layout文件、AndroidManifest文件、build.gradle文件
            files(dirs).asFileTree.forEach { file ->
                when (file.name) {
                    //修改AndroidManifest.xml文件
                    "AndroidManifest.xml" -> file.modifyManifestFile(oldPackage, newPackage)
                    //修改 build.gradle namespace
                    buildFile.name -> file.modifyBuildGradleFile(oldPackage, newPackage)
                    //修改layout文件
                    else -> file.modifyLayoutXml(oldPackage)
                }
            }
        }

        //2.修改 kt/java文件
        files(javaDirs).asFileTree.forEach { javaFile ->
            var readText = javaFile.readText()
            map.entries.forEach { e->
                val old = e.key
                val newStr = e.value
                if(old==null || newStr==null)return

                readText = readText.replaceWords("$old.R", "$newStr.R")
                    .replaceWords("$old.R2", "$newStr.R2")
                    .replaceWords("$old.BR", "$newStr.BR")
                    .replaceWords("$old.BuildConfig", "$newStr.BuildConfig")
                    .replaceWords("$old.databinding", "$newStr.databinding")
            }
            readText.let { javaFile.writeText(it) }
        }

        //3.替换完再进行对旧包名下的直接子类，检测R类、BuildConfig类是否有用到，有的话，插入import语句
        if(newPackage!=null){
            val oldPackagePath = oldPackage.replace(".", File.separator)
            javaDirs.forEach {
                File(it, oldPackagePath).listFiles { f -> f.isFile }?.forEach { file ->
                    file.insertImportXxxIfAbsent(newPackage)
                }
            }
        }

    }

    //修复build.gradle文件的 namespace 语句
    private fun File.modifyBuildGradleFile(oldPackage: String, newPackage: String) {
        readText()
            .replace("namespace\\s+['\"]${oldPackage}['\"]".toRegex(), "namespace '$newPackage'")
            .replace("namespace\\s*=\\s*['\"]${oldPackage}['\"]".toRegex(), """namespace = "$newPackage"""")
            .let { writeText(it) }
    }

    //修改AndroidManifest.xml文件，并返回新旧包名
    private fun File.modifyManifestFile(oldPackage: String, newPackage: String) {
        readText()
            .replaceWords("""package="$oldPackage"""", """package="$newPackage"""")
            .replaceWords("""android:name=".""", """android:name="$oldPackage.""")
            .let { writeText(it) }
    }

    private fun File.modifyLayoutXml(oldPackage: String) {
        readText()
            .replaceWords("""tools:context=".""", """tools:context="$oldPackage.""")
            .replaceWords("""app:layoutManager=".""", """app:layoutManager="$oldPackage.""")
            .let { writeText(it) }
    }
}