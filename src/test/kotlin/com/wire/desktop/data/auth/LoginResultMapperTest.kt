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

import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginResultMapperTest {

    @Test
    fun givenServerConfigFailure_whenMappingToLoginError_thenReturnsServerUnreachableMessage() {
        val failure = GetServerConfigResult.Failure.Generic(CoreFailure.MissingClientRegistration)

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Could not reach the server. Please try again."), result)
    }

    @Test
    fun givenTooNewVersionFailure_whenMappingToLoginError_thenReturnsTooNewVersionMessage() {
        val failure = AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("This app version is too new for the server. Please check for updates."), result)
    }

    @Test
    fun givenUnknownServerVersionFailure_whenMappingToLoginError_thenReturnsUnknownVersionMessage() {
        val failure = AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Could not determine the server version."), result)
    }

    @Test
    fun givenGenericAuthScopeFailure_whenMappingToLoginError_thenReturnsGenericMessage() {
        val failure = AutoVersionAuthScopeUseCase.Result.Failure.Generic(CoreFailure.MissingClientRegistration)

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Something went wrong. Please try again."), result)
    }

    @Test
    fun givenInvalidCredentials_whenMappingAuthenticationFailureToLoginError_thenReturnsIncorrectCredentialsMessage() {
        val failure = AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Incorrect email or password."), result)
    }

    @Test
    fun givenInvalidUserIdentifier_whenMappingAuthenticationFailureToLoginError_thenReturnsIncorrectCredentialsMessage() {
        val failure = AuthenticationResult.Failure.InvalidUserIdentifier

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Incorrect email or password."), result)
    }

    @Test
    fun givenAccountSuspended_whenMappingAuthenticationFailureToLoginError_thenReturnsSuspendedMessage() {
        val failure = AuthenticationResult.Failure.AccountSuspended

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("This account has been suspended."), result)
    }

    @Test
    fun givenAccountPendingActivation_whenMappingAuthenticationFailureToLoginError_thenReturnsPendingActivationMessage() {
        val failure = AuthenticationResult.Failure.AccountPendingActivation

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("This account hasn't been activated yet."), result)
    }

    @Test
    fun givenSocketError_whenMappingAuthenticationFailureToLoginError_thenReturnsServerUnreachableMessage() {
        val failure = AuthenticationResult.Failure.SocketError

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Could not reach the server. Please try again."), result)
    }

    @Test
    fun givenGenericAuthenticationFailure_whenMappingToLoginError_thenReturnsGenericMessage() {
        val failure = AuthenticationResult.Failure.Generic(CoreFailure.MissingClientRegistration)

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Something went wrong. Please try again."), result)
    }

    @Test
    fun givenUserAlreadyExists_whenMappingAddAuthenticatedUserFailureToLoginError_thenReturnsAlreadyLoggedInMessage() {
        val failure = AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("This account is already logged in."), result)
    }

    @Test
    fun givenGenericAddAuthenticatedUserFailure_whenMappingToLoginError_thenReturnsCouldNotSaveSessionMessage() {
        val failure = AddAuthenticatedUserUseCase.Result.Failure.Generic(CoreFailure.MissingClientRegistration)

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Could not save this session. Please try again."), result)
    }

    @Test
    fun givenInvalidCredentials_whenMappingRegisterClientFailureToLoginError_thenReturnsIncorrectCredentialsMessage() {
        val failure = RegisterClientResult.Failure.InvalidCredentials.InvalidPassword

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Incorrect email or password."), result)
    }

    @Test
    fun givenTooManyClients_whenMappingRegisterClientFailureToLoginError_thenReturnsTooManyClientsMessage() {
        val failure = RegisterClientResult.Failure.TooManyClients

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("This account already has the maximum number of devices registered."), result)
    }

    @Test
    fun givenPasswordAuthRequired_whenMappingRegisterClientFailureToLoginError_thenReturnsPasswordRequiredMessage() {
        val failure = RegisterClientResult.Failure.PasswordAuthRequired

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Please enter your password to register this device."), result)
    }

    @Test
    fun givenGenericRegisterClientFailure_whenMappingToLoginError_thenReturnsCouldNotRegisterMessage() {
        val failure = RegisterClientResult.Failure.Generic(CoreFailure.MissingClientRegistration)

        val result = failure.toLoginError()

        assertEquals(LoginResult.Error("Could not register this device. Please try again."), result)
    }
}
