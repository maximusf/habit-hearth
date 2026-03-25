package com.project.habithearth.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.project.habithearth.data.AccountSettings
import com.project.habithearth.data.UserProgressRepository
import androidx.compose.ui.platform.LocalContext
import com.project.habithearth.ui.components.VerticalScrollIndicator
import com.project.habithearth.ui.state.GameUiState
import com.project.habithearth.ui.theme.HabitHearthTheme
import kotlinx.coroutines.launch

private data class ProfilePicturePlaceholder(
    val id: Int,
    val label: String,
    val backgroundColor: Color,
)

private val profilePicturePlaceholders = listOf(
    ProfilePicturePlaceholder(0, "A", Color(0xFF5C7C6A)),
    ProfilePicturePlaceholder(1, "B", Color(0xFF6B5B7C)),
    ProfilePicturePlaceholder(2, "C", Color(0xFF7C6B5B)),
    ProfilePicturePlaceholder(3, "D", Color(0xFF5B6B7C)),
    ProfilePicturePlaceholder(4, "E", Color(0xFF7C7C5B)),
    ProfilePicturePlaceholder(5, "F", Color(0xFF5B7C7C)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    gameUiState: GameUiState,
    userProgressRepository: UserProgressRepository,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val account by userProgressRepository.accountSettings.collectAsState(initial = AccountSettings.DEFAULT)

    var displayNameDraft by remember { mutableStateOf(account.displayName) }
    LaunchedEffect(account.displayName) {
        displayNameDraft = account.displayName
    }

    var usernameDraft by remember { mutableStateOf(account.username) }
    LaunchedEffect(account.username) {
        usernameDraft = account.username
    }

    var addLoginUsername by remember { mutableStateOf("") }
    var addLoginPassword by remember { mutableStateOf("") }
    var addLoginConfirm by remember { mutableStateOf("") }

    var showChangePassword by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val themeOptions = remember { listOf("System default", "Light", "Dark") }
    var themeExpanded by remember { mutableStateOf(false) }

    val languageOptions = remember { listOf("English", "Español", "Français") }
    var languageExpanded by remember { mutableStateOf(false) }

    val textSizeOptions = remember { listOf("Small", "Default", "Large", "Extra large") }
    var textSizeExpanded by remember { mutableStateOf(false) }

    val selectedProfile = profilePicturePlaceholders[
        account.profileAvatarId.coerceIn(0, profilePicturePlaceholders.lastIndex),
    ]
    var showProfileGallery by remember { mutableStateOf(false) }

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onSubmit = { current, newPassword ->
                scope.launch {
                    val ok = userProgressRepository.changePassword(current, newPassword)
                    showChangePassword = false
                    notice = if (ok) {
                        "Password updated."
                    } else {
                        "Could not change password. Check your current password and new password length."
                    }
                }
            },
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Sign out?") },
            text = {
                Text("You'll need your username and password to open the app again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        scope.launch {
                            if (userProgressRepository.logout()) {
                                onLogoutSuccess()
                            } else {
                                notice = "Add a username and password in Profile first to sign out."
                            }
                        }
                    },
                ) {
                    Text("Sign out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showProfileGallery) {
        ProfilePictureGalleryDialog(
            onDismiss = { showProfileGallery = false },
            onPick = { option ->
                scope.launch {
                    userProgressRepository.setProfileAvatarId(option.id)
                }
                showProfileGallery = false
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 14.dp)
                .verticalScroll(scrollState)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
            )





            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ){
                    ProfileAvatar(
                        selected = selectedProfile,
                        onClick = { showProfileGallery = true },
                        modifier = Modifier.size(88.dp),
                    )
                    Text(
                        text = account.displayName.ifBlank { "Traveler" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    ResourceLine(
                        label = "Strength gem",
                        value = gameUiState.strengthGems.toString(),
                        leading = {
                            GemPlaceholder(
                                backgroundColor = Color(0xFFB85C5C),
                                content = {
                                    Icon(
                                        imageVector = Icons.Filled.Diamond,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White.copy(alpha = 0.95f),
                                    )
                                },
                            )
                        },
                    )
                    ResourceLine(
                        label = "Wisdom gem",
                        value = gameUiState.wisdomGems.toString(),
                        leading = {
                            GemPlaceholder(
                                backgroundColor = Color(0xFF5C6BB8),
                                content = {
                                    Icon(
                                        imageVector = Icons.Filled.Diamond,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White.copy(alpha = 0.95f),
                                    )
                                },
                            )
                        },
                    )
                    ResourceLine(
                        label = "Vitality gem",
                        value = gameUiState.vitalityGems.toString(),
                        leading = {
                            GemPlaceholder(
                                backgroundColor = Color(0xFF5CB86B),
                                content = {
                                    Icon(
                                        imageVector = Icons.Filled.Diamond,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White.copy(alpha = 0.95f),
                                    )
                                },
                            )
                        },
                    )
                    ResourceLine(
                        label = "Spirit gem",
                        value = gameUiState.spiritGems.toString(),
                        leading = {
                            GemPlaceholder(
                                backgroundColor = Color(0xFF8B5CB8),
                                content = {
                                    Icon(
                                        imageVector = Icons.Filled.Diamond,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White.copy(alpha = 0.95f),
                                    )
                                },
                            )
                        },
                    )
                    ResourceLine(
                        label = "Coins",
                        value = gameUiState.coins.toString(),
                        leading = {
                            GemPlaceholder(
                                backgroundColor = Color(0xFFC9A227),
                                content = {
                                    Icon(
                                        imageVector = Icons.Filled.Paid,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White.copy(alpha = 0.95f),
                                    )
                                },
                            )
                        },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsToggleRow(
                label = "Push notifications",
                checked = account.pushNotifications,
                onCheckedChange = { enabled ->
                    scope.launch { userProgressRepository.setPushNotifications(enabled) }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsToggleRow(
                label = "Vacation mode",
                checked = account.vacationMode,
                onCheckedChange = { enabled ->
                    scope.launch { userProgressRepository.setVacationMode(enabled) }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsDropdownRow(
                label = "Theme mode",
                options = themeOptions,
                selected = account.themeMode,
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = it },
                onSelect = {
                    scope.launch { userProgressRepository.setThemeMode(it) }
                    themeExpanded = false
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsDropdownRow(
                label = "Language",
                options = languageOptions,
                selected = account.language,
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = it },
                onSelect = {
                    scope.launch { userProgressRepository.setLanguage(it) }
                    languageExpanded = false
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsDropdownRow(
                label = "Text size",
                options = textSizeOptions,
                selected = account.textSize,
                expanded = textSizeExpanded,
                onExpandedChange = { textSizeExpanded = it },
                onSelect = {
                    scope.launch { userProgressRepository.setTextSize(it) }
                    textSizeExpanded = false
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            OutlinedTextField(
                value = displayNameDraft,
                onValueChange = { displayNameDraft = it },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    scope.launch {
                        userProgressRepository.setDisplayName(displayNameDraft)
                    }
                },
            ) {
                Text("Save display name")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Username and password for signing in. Stored only on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Account & settings are saved on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (account.hasLoginCredentials) {
                OutlinedTextField(
                    value = usernameDraft,
                    onValueChange = { usernameDraft = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            userProgressRepository.setUsername(usernameDraft)
                            notice = "Username saved."
                        }
                    },
                    enabled = usernameDraft.isNotBlank(),
                ) {
                    Text("Save username")
                }
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.titleSmall,
                )
                TextButton(onClick = { showChangePassword = true }) {
                    Text("Change password")
                }
            } else {
                Text(
                    text = "This profile was created before sign-in was added. Choose a username and password to protect your session when you sign out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = addLoginUsername,
                    onValueChange = { addLoginUsername = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addLoginPassword,
                    onValueChange = { addLoginPassword = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addLoginConfirm,
                    onValueChange = { addLoginConfirm = it },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        when {
                            addLoginUsername.isBlank() -> notice = "Enter a username."
                            addLoginPassword.length < UserProgressRepository.MIN_PASSWORD_LENGTH ->
                                notice = "Password must be at least ${UserProgressRepository.MIN_PASSWORD_LENGTH} characters."
                            addLoginPassword != addLoginConfirm -> notice = "Passwords don't match."
                            else -> {
                                scope.launch {
                                    val ok = userProgressRepository.setLoginCredentials(
                                        addLoginUsername,
                                        addLoginPassword,
                                    )
                                    notice = if (ok) {
                                        addLoginPassword = ""
                                        addLoginConfirm = ""
                                        "Sign-in saved. You can use Sign out above."
                                    } else {
                                        "Could not save sign-in."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save sign-in")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Session",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Sign out locks the app until you sign in again with your username and password.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (account.hasLoginCredentials) {
                Button(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("Sign out")
                }
            } else {
                Text(
                    text = "Add sign-in credentials above to enable sign out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            notice?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        VerticalScrollIndicator(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun ProfileAvatar(
    selected: ProfilePicturePlaceholder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .background(selected.backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = selected.label,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White.copy(alpha = 0.92f),
        )
    }
}

@Composable
private fun ProfilePictureGalleryDialog(
    onDismiss: () -> Unit,
    onPick: (ProfilePicturePlaceholder) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose profile picture",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Placeholders until art is added. Tap one to use it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                profilePicturePlaceholders.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEachIndexed { index, option ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .background(option.backgroundColor)
                                    .clickable { onPick(option) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White.copy(alpha = 0.92f),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun GemPlaceholder(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ResourceLine(
    label: String,
    value: String,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            leading()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownRow(
    label: String,
    options: List<String>,
    selected: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (currentPassword: String, newPassword: String) -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        localError = null
                    },
                    label = { Text("Current password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        localError = null
                    },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                localError?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        newPassword.length < UserProgressRepository.MIN_PASSWORD_LENGTH -> {
                            localError =
                                "New password must be at least ${UserProgressRepository.MIN_PASSWORD_LENGTH} characters."
                        }
                        newPassword != confirmPassword -> {
                            localError = "New passwords don't match."
                        }
                        else -> onSubmit(currentPassword, newPassword)
                    }
                },
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//private fun ProfileScreenPreview() {
//    val context = LocalContext.current
//    val repo = remember { UserProgressRepository(context.applicationContext) }
//    HabitHearthTheme {
//        ProfileScreen(
//            gameUiState = GameUiState(),
//            userProgressRepository = repo,
//            onLogoutSuccess = {},
//        )
//    }
//}
