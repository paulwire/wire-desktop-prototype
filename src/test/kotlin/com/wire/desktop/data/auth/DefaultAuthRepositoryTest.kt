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
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val testUserId = UserId("test-user", "test-domain")

private val authenticationSuccess = AuthenticationResult.Success(
    authData = AccountTokens(
        userId = testUserId,
        accessToken = "access-token",
        refreshToken = "refresh-token",
        tokenType = "Bearer",
        cookieLabel = null,
    ),
    ssoID = null,
    serverConfigId = "server-config-id",
    proxyCredentials = null,
)

private val registeredClient = Client(
    id = ClientId("test-client"),
    type = null,
    registrationTime = null,
    lastActive = null,
    isVerified = false,
    isValid = true,
    deviceType = null,
    label = null,
    model = null,
    mlsPublicKeys = null,
    isMLSCapable = false,
    isAsyncNotificationsCapable = false,
)

private class FakeKaliumAuthGateway(
    private val fetchServerConfigResult: GetServerConfigResult = GetServerConfigResult.Success(ServerConfig.STAGING),
    private val loginResult: LoginAttemptResult = LoginAttemptResult.Success(authenticationSuccess),
    private val persistAccountResult: AddAuthenticatedUserUseCase.Result = AddAuthenticatedUserUseCase.Result.Success(testUserId),
    private val registerClientResult: RegisterClientResult = RegisterClientResult.Success(registeredClient),
) : KaliumAuthGateway {

    var loginCalled = false
        private set
    var persistAccountCalled = false
        private set
    var registerClientCalled = false
        private set
    var capturedLoginEmail: String? = null
        private set
    var capturedLoginPassword: String? = null
        private set
    var capturedRegisterUserId: UserId? = null
        private set
    var capturedRegisterPassword: String? = null
        private set

    override suspend fun fetchServerConfig(url: String): GetServerConfigResult = fetchServerConfigResult

    override suspend fun login(serverLinks: ServerConfig.Links, email: String, password: String): LoginAttemptResult {
        loginCalled = true
        capturedLoginEmail = email
        capturedLoginPassword = password
        return loginResult
    }

    override suspend fun persistAccount(loginSuccess: AuthenticationResult.Success): AddAuthenticatedUserUseCase.Result {
        persistAccountCalled = true
        return persistAccountResult
    }

    override suspend fun registerClient(userId: UserId, password: String): RegisterClientResult {
        registerClientCalled = true
        capturedRegisterUserId = userId
        capturedRegisterPassword = password
        return registerClientResult
    }
}

class DefaultAuthRepositoryTest {

    @Test
    fun givenServerConfigFails_whenLoggingIn_thenReturnsErrorAndStopsBeforeLogin() = runTest {
        val gateway = FakeKaliumAuthGateway(
            fetchServerConfigResult = GetServerConfigResult.Failure.Generic(CoreFailure.MissingClientRegistration),
        )
        val repository = DefaultAuthRepository(gateway)

        val result = repository.login("user@example.com", "password")

        assertIs<LoginResult.Error>(result)
        assertEquals(false, gateway.loginCalled)
    }

    @Test
    fun givenAuthScopeFails_whenLoggingIn_thenReturnsErrorAndStopsBeforePersistingAccount() = runTest {
        val gateway = FakeKaliumAuthGateway(
            loginResult = LoginAttemptResult.AuthScopeFailure(AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion),
        )
        val repository = DefaultAuthRepository(gateway)

        val result = repository.login("user@example.com", "password")

        assertIs<LoginResult.Error>(result)
        assertEquals(false, gateway.persistAccountCalled)
    }

    @Test
    fun givenLoginFails_whenLoggingIn_thenReturnsErrorAndStopsBeforePersistingAccount() = runTest {
        val loginFailure = AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination
        val gateway = FakeKaliumAuthGateway(loginResult = LoginAttemptResult.LoginFailure(loginFailure))
        val repository = DefaultAuthRepository(gateway)

        val result = repository.login("user@example.com", "wrong-password")

        assertIs<LoginResult.Error>(result)
        assertEquals(false, gateway.persistAccountCalled)
    }

    @Test
    fun givenPersistAccountFails_whenLoggingIn_thenReturnsErrorAndStopsBeforeRegisteringClient() = runTest {
        val gateway = FakeKaliumAuthGateway(
            persistAccountResult = AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
        )
        val repository = DefaultAuthRepository(gateway)

        val result = repository.login("user@example.com", "password")

        assertIs<LoginResult.Error>(result)
        assertEquals(false, gateway.registerClientCalled)
    }

    @Test
    fun givenRegisterClientFails_whenLoggingIn_thenReturnsError() = runTest {
        val gateway = FakeKaliumAuthGateway(
            registerClientResult = RegisterClientResult.Failure.TooManyClients,
        )
        val repository = DefaultAuthRepository(gateway)

        val result = repository.login("user@example.com", "password")

        assertIs<LoginResult.Error>(result)
    }

    @Test
    fun givenE2EICertificateRequired_whenLoggingIn_thenReturnsErrorMentioningCertificateEnrollment() = runTest {
        val gateway = FakeKaliumAuthGateway(
            registerClientResult = RegisterClientResult.E2EICertificateRequired(registeredClient, testUserId),
        )
        val repository = DefaultAuthRepository(gateway)

        val result = repository.login("user@example.com", "password")

        assertIs<LoginResult.Error>(result)
        assertEquals(
            "This account requires end-to-end identity certificate enrollment, which isn't supported yet.",
            result.message,
        )
    }

    @Test
    fun givenAllStepsSucceed_whenLoggingIn_thenReturnsSuccessAndForwardsArgumentsToEachStep() = runTest {
        val gateway = FakeKaliumAuthGateway()
        val repository = DefaultAuthRepository(gateway)

        val result = repository.login("user@example.com", "password")

        assertEquals(LoginResult.Success, result)
        assertEquals("user@example.com", gateway.capturedLoginEmail)
        assertEquals("password", gateway.capturedLoginPassword)
        assertEquals(testUserId, gateway.capturedRegisterUserId)
        assertEquals("password", gateway.capturedRegisterPassword)
    }
}
