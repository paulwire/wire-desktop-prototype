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

import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationResult.Failure.InvalidCredentials as AuthInvalidCredentials
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientResult.Failure.InvalidCredentials as ClientInvalidCredentials
import com.wire.kalium.logic.feature.server.GetServerConfigResult

// 2FA failures map to their own message rather than an interactive retry flow - out of scope for
// this slice (see issue #12) - but must stay distinct from "wrong password": InvalidCredentials
// covers both Missing2FA/Invalid2FA (credentials are correct) and InvalidPasswordIdentityCombination
// (they aren't), so folding them into one message would misreport valid credentials as wrong.
private const val SECOND_FACTOR_REQUIRED_MESSAGE = "This account requires a two-factor verification code, which isn't supported yet."

internal fun GetServerConfigResult.Failure.toLoginError(): LoginResult.Error =
    LoginResult.Error("Could not reach the server. Please try again.")

internal fun AutoVersionAuthScopeUseCase.Result.Failure.toLoginError(): LoginResult.Error = when (this) {
    // TooNewVersion fires when the server's max supported API version exceeds the app's - i.e. the
    // app is the outdated side here, not the server (confirmed against ApiVersionDTO's handling in
    // kalium's ServerConfigRepositoryExtension).
    AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion ->
        LoginResult.Error("This server requires a newer version of the app. Please check for updates.")
    AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion ->
        LoginResult.Error("Could not determine the server version.")
    is AutoVersionAuthScopeUseCase.Result.Failure.Generic ->
        LoginResult.Error("Something went wrong. Please try again.")
}

internal fun AuthenticationResult.Failure.toLoginError(): LoginResult.Error = when (this) {
    AuthInvalidCredentials.Missing2FA, AuthInvalidCredentials.Invalid2FA ->
        LoginResult.Error(SECOND_FACTOR_REQUIRED_MESSAGE)
    AuthInvalidCredentials.InvalidPasswordIdentityCombination, AuthenticationResult.Failure.InvalidUserIdentifier ->
        LoginResult.Error("Incorrect email or password.")
    AuthenticationResult.Failure.AccountSuspended ->
        LoginResult.Error("This account has been suspended.")
    AuthenticationResult.Failure.AccountPendingActivation ->
        LoginResult.Error("This account hasn't been activated yet.")
    AuthenticationResult.Failure.SocketError ->
        LoginResult.Error("Could not reach the server. Please try again.")
    is AuthenticationResult.Failure.Generic ->
        LoginResult.Error("Something went wrong. Please try again.")
}

internal fun AddAuthenticatedUserUseCase.Result.Failure.toLoginError(): LoginResult.Error = when (this) {
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists ->
        LoginResult.Error("This account is already logged in.")
    is AddAuthenticatedUserUseCase.Result.Failure.Generic ->
        LoginResult.Error("Could not save this session. Please try again.")
}

internal fun RegisterClientResult.Failure.toLoginError(): LoginResult.Error = when (this) {
    ClientInvalidCredentials.Missing2FA, ClientInvalidCredentials.Invalid2FA ->
        LoginResult.Error(SECOND_FACTOR_REQUIRED_MESSAGE)
    ClientInvalidCredentials.InvalidPassword ->
        LoginResult.Error("Incorrect email or password.")
    RegisterClientResult.Failure.TooManyClients ->
        LoginResult.Error("This account already has the maximum number of devices registered.")
    RegisterClientResult.Failure.PasswordAuthRequired ->
        LoginResult.Error("Please enter your password to register this device.")
    is RegisterClientResult.Failure.Generic ->
        LoginResult.Error("Could not register this device. Please try again.")
}
