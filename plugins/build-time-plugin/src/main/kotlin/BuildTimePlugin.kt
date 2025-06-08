import com.android.build.api.variant.*
import com.android.build.gradle.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.internal.buildoption.BooleanBuildOption
import org.gradle.internal.cc.base.*
import org.gradle.kotlin.dsl.*
import java.io.*
import java.time.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Adds the given build time (epoc seconds of the build day) as a resource string
 */
class BuildTimePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.foreachAndroidVariant {
            val res = it.sources.res
            if (res == null) {
                logger.error("Skipping ${it.name} as it cannot have any Res folder(s)")
                return@foreachAndroidVariant
            }
            createTaskFor(project = project, sourceDirs = res, variantName = it.name)
        }
    }

    private fun Project.foreachAndroidVariant(onVariant: (ApplicationVariant) -> Unit) {
        project.plugins.withType(AppPlugin::class.java) {
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            androidComponents.onVariants(callback = onVariant)
        }
    }

    private fun createTaskFor(
        project: Project,
        sourceDirs: SourceDirectories.Layered,
        variantName: String
    ) {
        val assetCreationTask =
            project.tasks.register<BuildTimeTask>("create${variantName.titleCase()}BuildTimeResource")
        sourceDirs.addGeneratedSourceDirectory(
            assetCreationTask,
            BuildTimeTask::outputDirectory
        )
    }

    private fun String.titleCase(): String {
        return replaceFirstChar { it.uppercase() }
    }
}

abstract class BuildTimeTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val resourceName: Property<String>

    @get:Input
    abstract val fileName: Property<String>


    init {
        resourceName.convention("build_time_epoc_seconds")
        fileName.convention("build_time_epoc_seconds")
        group = "build"
        outputs.upToDateWhen {
            isUpToDate()
        }
    }

    override fun getDescription(): String? {
        return "Writes the build day (in epoc seconds) into a resource string by the property \"${BuildTimeTask::resourceName.name}\"." +
                "\nIts currently set to ${this.resourceName.get()}"
    }

    @TaskAction
    fun taskAction() {
        val resourceFile: File = getBuildTimeFileAfterSetup()
        writeBuildTimeInto(resourceFile)
    }

    private fun writeBuildTimeInto(file: File) {
        file.writeText(
            """
                            <resources>
                                <string name="build_time_epoc_seconds">${getEpocSecondsOfToday()}</string>
                            </resources>
            """.trimIndent()
        )
    }

    private fun getEpocSecondsOfToday(): Long {
        return LocalDate.now().toEpochSecond(LocalTime.MIN, ZoneOffset.UTC)
    }

    private fun getBuildTimeFileAfterSetup(): File {
        val valuesFolder = createValuesFolder()
        return createStringResourceFile(valuesFolder)
    }

    private fun createStringResourceFile(valuesFolder: File): File {
        return File(valuesFolder, "build-time.xml")
    }

    private fun createValuesFolder(): File {
        val valuesDir = File(outputDirectory.get().asFile, "values")
        try {
            valuesDir.mkdirs()
            return valuesDir
        } catch (e: Throwable) {
            logger.error("", e)
            throw e
        }
    }


    private fun epocSecondsInADay(): Long {
        return 60 * 60 * 60 * 24
    }

    private fun isUpToDate(): Boolean {
        val file = getBuildTimeFileAfterSetup()
        if (!file.exists()) {
            return false
        }
        return isFileUpdatedToday(file.lastModified())
    }

    private fun isFileUpdatedToday(lastModified: Long): Boolean {
        val epocSecondsYesterday = getEpocSecondsOfToday() - epocSecondsInADay()
        val epocSecondsTomorrow = getEpocSecondsOfToday() + epocSecondsInADay()

        val lastModifiedEpocSeconds = lastModified.milliseconds.inWholeSeconds

        return lastModifiedEpocSeconds >= epocSecondsYesterday &&
                lastModifiedEpocSeconds <= epocSecondsTomorrow
    }
}
