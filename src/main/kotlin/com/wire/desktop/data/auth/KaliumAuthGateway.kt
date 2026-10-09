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

import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult

// One method per step of the call sequence confirmed in issue #11's spike (ADR 0003). This seam
// exists so DefaultAuthRepository's sequencing/branching logic is unit-testable against a
// hand-written fake, without needing a real CoreLogic or a mocking library.
interface KaliumAuthGateway {
    suspend fun fetchServerConfig(url: String): GetServerConfigResult
    suspend fun login(serverLinks: ServerConfig.Links, email: String, password: String): LoginAttemptResult
    suspend fun persistAccount(loginSuccess: AuthenticationResult.Success): AddAuthenticatedUserUseCase.Result
    suspend fun registerClient(userId: UserId, password: String): RegisterClientResult
}
