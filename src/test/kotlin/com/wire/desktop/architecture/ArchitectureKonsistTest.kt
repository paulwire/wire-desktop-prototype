package com.wire.desktop.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

private const val MAX_FILE_LINES = 300
private const val KALIUM_PACKAGE_PREFIX = "com.wire.kalium"
private const val DATA_PACKAGE_PREFIX = "com.wire.desktop.data"

class ArchitectureKonsistTest {

    @Test
    fun `layers depend only in the allowed direction`() {
        Konsist.scopeFromProject().assertArchitecture {
            val ui = Layer("UI", "com.wire.desktop.ui..")
            val presentation = Layer("Presentation", "com.wire.desktop.presentation..")
            val data = Layer("Data", "com.wire.desktop.data..")

            ui.dependsOn(presentation)
            presentation.dependsOn(data)
            data.dependsOnNothing()
        }
    }

    @Test
    fun `no file exceeds 300 lines`() {
        Konsist.scopeFromProject()
            .files
            .assertTrue { it.text.lines().size <= MAX_FILE_LINES }
    }

    @Test
    fun `only the data layer imports kalium`() {
        Konsist.scopeFromProject()
            .files
            .filterNot { it.packagee?.name?.startsWith(DATA_PACKAGE_PREFIX) == true }
            .assertFalse { file -> file.hasImport { it.name.startsWith(KALIUM_PACKAGE_PREFIX) } }
    }
}
