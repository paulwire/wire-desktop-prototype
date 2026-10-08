/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

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
            ui.doesNotDependOn(data)
            presentation.dependsOn(data)
            presentation.doesNotDependOn(ui)
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
