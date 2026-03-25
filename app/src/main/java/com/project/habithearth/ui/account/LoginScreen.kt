package com.project.habithearth.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.ui.theme.HabitHearthTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    userProgressRepository: UserProgressRepository,
    defaultUsername: String,
    onSignedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showNewAccountConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(defaultUsername) {
        if (username.isEmpty() && defaultUsername.isNotEmpty()) {
            username = defaultUsername
        }
    }

    if (showNewAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showNewAccountConfirm = false },
            title = { Text("Start a new account?") },
            text = {
                Text(
                    "This removes all Habit Hearth data on this device — habits, village progress, " +
                        "and settings — so you can create a new profile.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewAccountConfirm = false
                        onCreateAccount()
                    },
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAccountConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sign in",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your username and password to continue.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                error = null
            },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                scope.launch {
                    if (userProgressRepository.tryUnlockSession(username, password)) {
                        onSignedIn()
                    } else {
                        error = "That username or password doesn't match."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign in")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "First time here, or want a fresh profile?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showNewAccountConfirm = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create an account")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//private fun LoginScreenPreview() {
//    val context = LocalContext.current
//    val repo = remember { UserProgressRepository(context.applicationContext) }
//    HabitHearthTheme {
//        LoginScreen(
//            userProgressRepository = repo,
//            defaultUsername = "",
//            onSignedIn = {},
//            onCreateAccount = {},
//        )
//    }
//}
