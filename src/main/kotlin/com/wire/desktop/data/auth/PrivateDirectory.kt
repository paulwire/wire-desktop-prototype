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
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

// Kalium's JVM session storage (access/refresh tokens, local DB) isn't encrypted at rest here - it
// relies on filesystem permissions instead. Restricting the root directory to owner-only is enough
// to block other local users: without read/execute on this directory, they can't resolve a path to
// any file inside it, regardless of that file's own permission bits. Plain java.nio logic, no
// Kalium dependency, kept in its own file so it's unit-testable and not swept into
// AuthRepositoryFactory's coverage exclusion (see PrivateDirectoryTest).
internal fun securePrivateDirectory(path: String) {
    val directory = Path.of(path)
    Files.createDirectories(directory)
    if (directory.fileSystem.supportedFileAttributeViews().contains("posix")) {
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"))
    }
}
