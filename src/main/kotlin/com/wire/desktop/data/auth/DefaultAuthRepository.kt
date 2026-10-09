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
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult

// Backend config is always resolved through Kalium's fetchServerConfigFromDeepLink, never
// hand-derived, per docs/roadmap.md.
private const val STAGING_DEEPLINK = "https://staging-nginz-https.zinfra.io/deeplink.json"

// Call sequence confirmed end to end against staging in issue #11's spike (ADR 0003): resolve
// server config, create an auth scope, log in, persist the account, then register a client - which
// also completes MLS key-package upload internally, no separate step needed. Split into one
// single-expression function per step (rather than early returns in one function) to stay under
// detekt's ReturnCount limit while keeping each step's failure handling next to its own call.
class DefaultAuthRepository(private val coreLogic: CoreLogic) : AuthRepository {

    override suspend fun login(email: String, password: String): LoginResult =
        when (val result = coreLogic.globalScope { fetchServerConfigFromDeepLink(STAGING_DEEPLINK) }) {
            is GetServerConfigResult.Failure -> result.toLoginError()
            is GetServerConfigResult.Success -> continueWithServerLinks(result.serverConfigLinks, email, password)
        }

    private suspend fun continueWithServerLinks(serverLinks: ServerConfig.Links, email: String, password: String): LoginResult =
        when (val result = coreLogic.versionedAuthenticationScope(serverLinks).invoke(null)) {
            is AutoVersionAuthScopeUseCase.Result.Failure -> result.toLoginError()
            is AutoVersionAuthScopeUseCase.Result.Success -> continueWithAuthScope(result.authenticationScope, email, password)
        }

    private suspend fun continueWithAuthScope(authenticationScope: AuthenticationScope, email: String, password: String): LoginResult =
        when (val result = authenticationScope.login(email, password, shouldPersistClient = true)) {
            is AuthenticationResult.Failure -> result.toLoginError()
            is AuthenticationResult.Success -> continueWithLoginSuccess(result, password)
        }

    private suspend fun continueWithLoginSuccess(loginSuccess: AuthenticationResult.Success, password: String): LoginResult =
        when (
            val result = coreLogic.globalScope {
                addAuthenticatedAccount(loginSuccess.serverConfigId, loginSuccess.ssoID, loginSuccess.authData, null, true)
            }
        ) {
            is AddAuthenticatedUserUseCase.Result.Failure -> result.toLoginError()
            is AddAuthenticatedUserUseCase.Result.Success -> registerClient(result.userId, password)
        }

    private suspend fun registerClient(userId: UserId, password: String): LoginResult =
        when (
            val result = coreLogic.sessionScope(userId) {
                client.getOrRegister(RegisterClientParam(password, emptyList()))
            }
        ) {
            is RegisterClientResult.Success, is RegisterClientResult.E2EICertificateRequired -> LoginResult.Success
            is RegisterClientResult.Failure -> result.toLoginError()
        }
}
