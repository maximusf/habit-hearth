package com.project.habithearth.ui.profile

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.habithearth.BuildConfig
import com.project.habithearth.data.AccountSettings
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.ui.components.VerticalScrollIndicator
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.state.GameUiState
import kotlinx.coroutines.launch

private data class ProfilePicturePlaceholder(
    val id: Int,
    val label: String,
    val backgroundColor: Color,
    val imageAssetPath: String? = null,
)

private const val HedgehogPlaceholderAssetPath = "images/characters/hedgehog.PNG"
private const val BearPlaceholderAssetPath = "images/characters/bear.PNG"

private val profilePicturePlaceholders = listOf(
    ProfilePicturePlaceholder(0, "A", Color(0xFF5C7C6A), HedgehogPlaceholderAssetPath),
    ProfilePicturePlaceholder(1, "B", Color(0xFF6B5B7C), BearPlaceholderAssetPath),
    ProfilePicturePlaceholder(2, "C", Color(0xFF7C6B5B), HedgehogPlaceholderAssetPath),
    ProfilePicturePlaceholder(3, "D", Color(0xFF5B6B7C), HedgehogPlaceholderAssetPath),
    ProfilePicturePlaceholder(4, "E", Color(0xFF7C7C5B), HedgehogPlaceholderAssetPath),
    ProfilePicturePlaceholder(5, "F", Color(0xFF5B7C7C), HedgehogPlaceholderAssetPath),
)

@Composable
private fun profileTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    gameUiState: GameUiState,
    userProgressRepository: UserProgressRepository,
    gameStateViewModel: GameStateViewModel,
    debugPanelVisible: Boolean,
    onHideDebugPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val account by userProgressRepository.accountSettings.collectAsState(initial = AccountSettings.DEFAULT)

    var displayNameDraft by remember { mutableStateOf(account.displayName) }
    LaunchedEffect(account.displayName) {
        displayNameDraft = account.displayName
    }

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
                ) {
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
                        label = "Level",
                        value = "Lv ${com.project.habithearth.ui.state.levelFor(gameUiState.totalXp)} (${gameUiState.totalXp} XP)",
                        leading = {
                            GemPlaceholder(
                                backgroundColor = Color(0xFF8B6F47),
                                content = {
                                    Text(
                                        text = "L",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.95f),
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

            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleLarge,
            )

            SettingsToggleRow(
                label = "Push notifications",
                checked = account.pushNotifications,
                onCheckedChange = { enabled ->
                    scope.launch { userProgressRepository.setPushNotifications(enabled) }
                },
            )

            SettingsToggleRow(
                label = "Vacation mode",
                checked = account.vacationMode,
                onCheckedChange = { enabled ->
                    scope.launch { userProgressRepository.setVacationMode(enabled) }
                },
            )

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
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Display name shown on the home screen. Stored only on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                        notice = "Display name saved."
                    }
                },
                colors = profileTextButtonColors(),
            ) {
                Text("Save display name")
            }

            notice?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }

            if (BuildConfig.DEBUG && debugPanelVisible) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DebugResourcePanel(
                    onAdjust = { s, w, v, sp, c, x ->
                        gameStateViewModel.debugAdjustResources(
                            strength = s,
                            wisdom = w,
                            vitality = v,
                            spirit = sp,
                            coins = c,
                            xpDelta = x,
                        )
                    },
                    onUnlockAll = { gameStateViewModel.debugUnlockAllBuildings() },
                    onReset = { gameStateViewModel.debugResetProgress() },
                    onHide = onHideDebugPanel,
                    currentLevel = com.project.habithearth.ui.state.levelFor(gameUiState.totalXp),
                    xpInLevel = com.project.habithearth.ui.state.xpInLevel(gameUiState.totalXp),
                    totalXp = gameUiState.totalXp,
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
    val context = LocalContext.current
    val avatarBitmap = remember(selected.imageAssetPath) {
        selected.imageAssetPath?.let { path ->
            decodeAssetBitmap(
                context = context,
                assetPath = path,
                maxEdgePx = 512,
            )
        }
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .background(selected.backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = avatarBitmap,
                contentDescription = "Profile avatar ${selected.label}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = selected.label,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.92f),
            )
        }
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
                    text = "Tap one to use it.",
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
                            val context = LocalContext.current
                            val optionBitmap = remember(option.imageAssetPath) {
                                option.imageAssetPath?.let { path ->
                                    decodeAssetBitmap(
                                        context = context,
                                        assetPath = path,
                                        maxEdgePx = 512,
                                    )
                                }
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
                                if (optionBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = optionBitmap,
                                        contentDescription = "Profile avatar option ${option.label}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = profileTextButtonColors()) {
                Text("Close")
            }
        },
    )
}

private fun decodeAssetBitmap(
    context: android.content.Context,
    assetPath: String,
    maxEdgePx: Int,
): ImageBitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val sample = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val decode = BitmapFactory.Options().apply {
            inSampleSize = sample
            inScaled = false
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, decode)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun sampleSizeForMaxEdge(width: Int, height: Int, maxEdgePx: Int): Int {
    val longest = maxOf(width, height)
    if (longest <= maxEdgePx) return 1
    var sample = 1
    while (longest / sample > maxEdgePx) {
        sample *= 2
    }
    return sample
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                checkedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebugResourcePanel(
    onAdjust: (s: Int, w: Int, v: Int, sp: Int, c: Int, xp: Int) -> Unit,
    onUnlockAll: () -> Unit,
    onReset: () -> Unit,
    onHide: () -> Unit,
    currentLevel: Int,
    xpInLevel: Int,
    totalXp: Int,
) {
    val steps = listOf(1, 10, 100)
    var step by remember { mutableStateOf(10) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Debug",
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = onHide, colors = profileTextButtonColors()) {
                Text("Hide")
            }
        }
        Text(
            text = "Hidden by default. Tap the Profile tab 7 times to reveal again. Stripped from release builds.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Step",
                style = MaterialTheme.typography.bodyMedium,
            )
            steps.forEach { s ->
                FilterChip(
                    selected = step == s,
                    onClick = { step = s },
                    label = { Text("+$s") },
                )
            }
        }

        DebugRow("Strength", { onAdjust(step, 0, 0, 0, 0, 0) }, { onAdjust(-step, 0, 0, 0, 0, 0) })
        DebugRow("Wisdom", { onAdjust(0, step, 0, 0, 0, 0) }, { onAdjust(0, -step, 0, 0, 0, 0) })
        DebugRow("Vitality", { onAdjust(0, 0, step, 0, 0, 0) }, { onAdjust(0, 0, -step, 0, 0, 0) })
        DebugRow("Spirit", { onAdjust(0, 0, 0, step, 0, 0) }, { onAdjust(0, 0, 0, -step, 0, 0) })
        DebugRow("Coins", { onAdjust(0, 0, 0, 0, step, 0) }, { onAdjust(0, 0, 0, 0, -step, 0) })
        DebugRow(
            label = "XP (Lv $currentLevel · $xpInLevel/${com.project.habithearth.ui.state.XP_PER_LEVEL}, total $totalXp)",
            onPlus = { onAdjust(0, 0, 0, 0, 0, step) },
            onMinus = { onAdjust(0, 0, 0, 0, 0, -step) },
        )

        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = onUnlockAll,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Unlock all buildings")
        }
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Text("Reset progress")
        }
    }
}

@Composable
private fun DebugRow(label: String, onPlus: () -> Unit, onMinus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onMinus) { Text("-") }
            OutlinedButton(onClick = onPlus) { Text("+") }
        }
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
