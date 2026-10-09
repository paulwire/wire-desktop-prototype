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

package com.wire.desktop.ui.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.wire.desktop.presentation.login.LoginUiState
import com.wire.desktop.presentation.login.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        if (state is LoginUiState.Success) {
            Text("Logged in", modifier = Modifier.semantics { contentDescription = "Logged in" })
        } else {
            LoginForm(state, onSubmit = viewModel::login)
        }
    }
}

@Composable
private fun LoginForm(state: LoginUiState, onSubmit: (email: String, password: String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = state is LoginUiState.Loading

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        enabled = !isLoading,
        modifier = Modifier.semantics { contentDescription = "Email input" },
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        enabled = !isLoading,
        modifier = Modifier.semantics { contentDescription = "Password input" },
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = { onSubmit(email, password) },
        enabled = !isLoading,
        modifier = Modifier.semantics { contentDescription = "Log in" },
    ) {
        Text(if (isLoading) "Logging in…" else "Log in")
    }
    if (state is LoginUiState.Error) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(state.message, color = MaterialTheme.colors.error)
    }
}
