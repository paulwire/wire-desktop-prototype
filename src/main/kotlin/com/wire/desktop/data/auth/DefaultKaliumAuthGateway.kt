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

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult

// Thin delegation to a real CoreLogic - no branching logic of its own beyond what's needed to keep
// AuthenticationScope (internal-constructor, opaque outside kalium's module) from crossing the
// gateway interface. Can't be meaningfully unit tested without a live Kalium session; all the
// sequencing/branching logic lives in DefaultAuthRepository instead, against this interface.
class DefaultKaliumAuthGateway(private val coreLogic: CoreLogic) : KaliumAuthGateway {

    override suspend fun fetchServerConfig(url: String): GetServerConfigResult =
        coreLogic.globalScope { fetchServerConfigFromDeepLink(url) }

    override suspend fun login(serverLinks: ServerConfig.Links, email: String, password: String): LoginAttemptResult =
        when (val scopeResult = coreLogic.versionedAuthenticationScope(serverLinks).invoke(null)) {
            is AutoVersionAuthScopeUseCase.Result.Failure -> LoginAttemptResult.AuthScopeFailure(scopeResult)
            is AutoVersionAuthScopeUseCase.Result.Success ->
                when (val authResult = scopeResult.authenticationScope.login(email, password, shouldPersistClient = true)) {
                    is AuthenticationResult.Failure -> LoginAttemptResult.LoginFailure(authResult)
                    is AuthenticationResult.Success -> LoginAttemptResult.Success(authResult)
                }
        }

    override suspend fun persistAccount(loginSuccess: AuthenticationResult.Success): AddAuthenticatedUserUseCase.Result =
        coreLogic.globalScope {
            addAuthenticatedAccount(loginSuccess.serverConfigId, loginSuccess.ssoID, loginSuccess.authData, null, true)
        }

    override suspend fun registerClient(userId: UserId, password: String): RegisterClientResult =
        coreLogic.sessionScope(userId) {
            client.getOrRegister(RegisterClientParam(password, emptyList()))
        }
}
