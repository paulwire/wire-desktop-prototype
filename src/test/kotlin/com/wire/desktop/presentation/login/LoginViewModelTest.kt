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

package com.wire.desktop.presentation.login

import com.wire.desktop.data.auth.AuthRepository
import com.wire.desktop.data.auth.LoginResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeAuthRepository(private val result: LoginResult) : AuthRepository {
    override suspend fun login(email: String, password: String): LoginResult = result
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Test
    fun givenNoLoginAttempted_whenReadingState_thenStateIsIdle() {
        val viewModel = LoginViewModel(FakeAuthRepository(LoginResult.Success), UnconfinedTestDispatcher())

        assertEquals(LoginUiState.Idle, viewModel.state.value)
    }

    @Test
    fun givenRepositoryReturnsSuccess_whenLoggingIn_thenStateBecomesSuccess() {
        val viewModel = LoginViewModel(FakeAuthRepository(LoginResult.Success), UnconfinedTestDispatcher())

        viewModel.login("user@example.com", "password")

        assertEquals(LoginUiState.Success, viewModel.state.value)
    }

    @Test
    fun givenRepositoryReturnsError_whenLoggingIn_thenStateBecomesErrorWithMessage() {
        val viewModel = LoginViewModel(
            FakeAuthRepository(LoginResult.Error("Incorrect email or password.")),
            UnconfinedTestDispatcher(),
        )

        viewModel.login("user@example.com", "wrong-password")

        assertEquals(LoginUiState.Error("Incorrect email or password."), viewModel.state.value)
    }
}
