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

// Backend config is always resolved through Kalium's fetchServerConfigFromDeepLink, never
// hand-derived, per docs/roadmap.md.
private const val STAGING_DEEPLINK = "https://staging-nginz-https.zinfra.io/deeplink.json"

// Call sequence confirmed end to end against staging in issue #11's spike (ADR 0003): resolve
// server config, log in, persist the account, then register a client - which also completes MLS
// key-package upload internally, no separate step needed. Split into one single-expression
// function per step (rather than early returns in one function) to stay under detekt's ReturnCount
// limit. All Kalium interaction goes through KaliumAuthGateway, so this sequencing/branching logic
// is unit-testable against a fake (see DefaultAuthRepositoryTest).
class DefaultAuthRepository(private val gateway: KaliumAuthGateway) : AuthRepository {

    override suspend fun login(email: String, password: String): LoginResult =
        when (val result = gateway.fetchServerConfig(STAGING_DEEPLINK)) {
            is GetServerConfigResult.Failure -> result.toLoginError()
            is GetServerConfigResult.Success -> continueWithServerLinks(result.serverConfigLinks, email, password)
        }

    private suspend fun continueWithServerLinks(serverLinks: ServerConfig.Links, email: String, password: String): LoginResult =
        when (val result = gateway.login(serverLinks, email, password)) {
            is LoginAttemptResult.AuthScopeFailure -> result.failure.toLoginError()
            is LoginAttemptResult.LoginFailure -> result.failure.toLoginError()
            is LoginAttemptResult.Success -> continueWithLoginSuccess(result.authenticationResult, password)
        }

    private suspend fun continueWithLoginSuccess(loginSuccess: AuthenticationResult.Success, password: String): LoginResult =
        when (val result = gateway.persistAccount(loginSuccess)) {
            is AddAuthenticatedUserUseCase.Result.Failure -> result.toLoginError()
            is AddAuthenticatedUserUseCase.Result.Success -> registerClient(result.userId, password)
        }

    private suspend fun registerClient(userId: UserId, password: String): LoginResult =
        when (val result = gateway.registerClient(userId, password)) {
            is RegisterClientResult.Success -> LoginResult.Success
            // The Proteus client registered, but MLS registration specifically is blocked pending
            // e2e identity certificate enrollment - not a plain success, since MLS conversations
            // won't work yet, and not supported by this slice (see issue #12's scope).
            is RegisterClientResult.E2EICertificateRequired -> LoginResult.Error(
                "This account requires end-to-end identity certificate enrollment, which isn't supported yet.",
            )
            is RegisterClientResult.Failure -> result.toLoginError()
        }
}
