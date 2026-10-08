package com.wire.desktop.ui.main

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.desktop.presentation.main.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var greeting by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        greeting = viewModel.loadGreeting()
    }

    Text(greeting)
}
