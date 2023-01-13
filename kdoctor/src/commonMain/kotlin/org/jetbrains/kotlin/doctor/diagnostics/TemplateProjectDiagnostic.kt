package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.System

class TemplateProjectDiagnostic(
    private val system: System,
    private val tag: String = "template"
) : Diagnostic() {
    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder("Synthetic generated project")

        val dir = system.execute("mktemp", "-d").output?.trim().orEmpty()
        if (dir.isEmpty()) {
            result.addWarning(
                "Error: impossible to create temporary directory",
                "Check your file system write permissions"
            )
            return result.build()
        }

        val zip = "$dir/archive.zip"

        val download = system.execute(
            "curl",
            "-L", //for redirects
            "-o", zip,
            "https://github.com/Kotlin/kdoctor/archive/refs/tags/$tag.zip"
        )
        if (download.code != 0) {
            result.addWarning(
                "Error: impossible to download a template project",
                "Check your internet connection"
            )
            return result.build()
        }

        val unzip = system.execute(
            "unzip",
            zip,
            "-d", dir
        )
        if (unzip.code != 0) {
            result.addWarning(
                "Error: impossible to unzip a template project"
            )
            return result.build()
        }

        val project = "$dir/kdoctor-$tag/template"
        val gradlew = "$project/gradlew"

        val gradleExecution = system.execute(
            gradlew,
            "-p", project,
            "clean", "linkReleaseFrameworkIosArm64", "jvmJar"
        )
        if (gradleExecution.code != 0) {
            result.addFailure(
                "Template project build has problems:",
                gradleExecution.rawOutput.orEmpty()
            )
            return result.build()
        }

        result.addSuccess(
            "Template project build is successful"
        )
        return result.build()
    }
}