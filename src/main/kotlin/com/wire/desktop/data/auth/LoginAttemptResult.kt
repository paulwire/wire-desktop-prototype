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

import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase

// Kalium's AuthenticationScope has an internal constructor (opaque outside kalium's own module), so
// it can never cross this gateway's interface boundary - a fake gateway couldn't construct one to
// test against. Folding "create the auth scope" + "log in with it" into one gateway call keeps that
// type entirely inside DefaultKaliumAuthGateway, while still letting the two failure modes map to
// distinct, testable results.
sealed interface LoginAttemptResult {
    data class Success(val authenticationResult: AuthenticationResult.Success) : LoginAttemptResult
    data class AuthScopeFailure(val failure: AutoVersionAuthScopeUseCase.Result.Failure) : LoginAttemptResult
    data class LoginFailure(val failure: AuthenticationResult.Failure) : LoginAttemptResult
}
