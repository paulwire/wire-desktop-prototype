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

package com.wire.desktop.data.auth

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrivateDirectoryTest {

    private val tempRoot = Files.createTempDirectory("private-directory-test")

    @AfterTest
    fun tearDown() {
        tempRoot.toFile().deleteRecursively()
    }

    @Test
    fun givenDirectoryDoesNotExist_whenSecuringIt_thenItIsCreatedWithOwnerOnlyPermissions() {
        val path = tempRoot.resolve("new-dir")

        securePrivateDirectory(path.toString())

        assertTrue(path.exists())
        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(path)))
    }

    @Test
    fun givenDirectoryAlreadyExistsWithLoosePermissions_whenSecuringIt_thenPermissionsAreRestricted() {
        val path = tempRoot.resolve("existing-dir")
        Files.createDirectory(path)
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"))

        securePrivateDirectory(path.toString())

        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(path)))
    }
}
