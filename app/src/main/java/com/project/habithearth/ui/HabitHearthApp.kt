package com.project.habithearth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBarItemDefaults
import com.project.habithearth.ui.theme.HearthBackground
import com.project.habithearth.ui.theme.HearthPanelWarm
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.habithearth.HabitHearthApplication
import com.project.habithearth.data.AccountSettings
import com.project.habithearth.ui.account.AccountCreationScreen
import com.project.habithearth.ui.account.LoginScreen
import com.project.habithearth.ui.home.BuildingDirectoryDialog
import com.project.habithearth.ui.home.HomeScreen
import com.project.habithearth.ui.map.BuildingDetailScreen
import com.project.habithearth.ui.map.MapScreen
import com.project.habithearth.ui.navigation.AppDestination
import com.project.habithearth.ui.navigation.TopChromeWithMenu
import com.project.habithearth.ui.navigation.TopResourceBar
import com.project.habithearth.ui.profile.ProfileScreen
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.state.GameStateViewModelFactory
import com.project.habithearth.ui.story.StoryScreen
import com.project.habithearth.ui.tasks.TaskMakerScreen
import kotlinx.coroutines.launch

private const val TaskMakerRoute = "task_maker"
private const val TaskMakerNewInBuildingRoute = "task_maker/building/{buildingId}"
private const val TaskMakerEditRoute = "task_maker/{taskId}"
private const val BuildingDetailRoute = "building_detail/{buildingId}"

private sealed interface AppShell {
    data object Loading : AppShell
    data object NeedAccount : AppShell
    data object NeedLogin : AppShell
    data object Main : AppShell
}

@Composable
fun HabitHearthApp(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as HabitHearthApplication
    val userProgressRepository = app.userProgressRepository

    val gameVm: GameStateViewModel = viewModel(
        factory = GameStateViewModelFactory(userProgressRepository),
    )
    val game by gameVm.uiState.collectAsState()
    val account by userProgressRepository.accountSettings.collectAsState(initial = AccountSettings.DEFAULT)
    val scope = rememberCoroutineScope()

    var shell by remember { mutableStateOf<AppShell>(AppShell.Loading) }
    LaunchedEffect(Unit) {
        shell = when {
            !userProgressRepository.isAccountSetupComplete() -> AppShell.NeedAccount
            userProgressRepository.shouldShowLoginGate() -> AppShell.NeedLogin
            else -> AppShell.Main
        }
    }

    var showBuildingDirectory by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    val route = current?.route.orEmpty()
    val isTaskMaker =
        route == TaskMakerRoute ||
            route.startsWith("task_maker/building/") ||
            (route.startsWith("task_maker/") && !route.startsWith("task_maker/building/"))
    val isBuildingDetail = route.startsWith("building_detail/")
    // Keep resource bar + bottom navigation visible on building detail screens.
    val hideMainChrome = isTaskMaker
    val isHome = current?.route == AppDestination.Home.route

    LaunchedEffect(isHome) {
        if (!isHome) showBuildingDirectory = false
    }

    val welcomeName = account.displayName.ifBlank { "Traveler" }

    when (shell) {
        AppShell.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        AppShell.NeedAccount -> {
            AccountCreationScreen(
                userProgressRepository = userProgressRepository,
                onAccountCreated = {
                    scope.launch {
                        // Ensure the UI shows the freshly initialized zeroed resources.
                        gameVm.reloadFromRepositoryNow()
                        shell = AppShell.Main
                    }
                },
                modifier = modifier.fillMaxSize(),
            )
        }
        AppShell.NeedLogin -> {
            LoginScreen(
                userProgressRepository = userProgressRepository,
                defaultUsername = account.username,
                onSignedIn = { shell = AppShell.Main },
                onCreateAccount = {
                    scope.launch {
                        userProgressRepository.clearAllLocalData()
                        gameVm.reloadFromRepositoryNow()
                        shell = AppShell.NeedAccount
                    }
                },
                modifier = modifier.fillMaxSize(),
            )
        }
        AppShell.Main -> {
            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    topBar = {
                        if (!hideMainChrome) {
                            if (isHome) {
                                TopChromeWithMenu(
                                    onMenuClick = { showBuildingDirectory = true },
                                    strengthGems = game.strengthGems,
                                    wisdomGems = game.wisdomGems,
                                    vitalityGems = game.vitalityGems,
                                    spiritGems = game.spiritGems,
                                    coins = game.coins,
                                    xpProgress = game.xpProgress,
                                )
                            } else {
                                TopResourceBar(
                                    strengthGems = game.strengthGems,
                                    wisdomGems = game.wisdomGems,
                                    vitalityGems = game.vitalityGems,
                                    spiritGems = game.spiritGems,
                                    coins = game.coins,
                                    xpProgress = game.xpProgress,
                                )
                            }
                        }
                    },
                    bottomBar = {
                        if (!hideMainChrome) {
                        NavigationBar(
                            containerColor = HearthPanelWarm,
                            contentColor = HearthBackground,
                        ) {
                                AppDestination.entries.forEach { destination ->
                                    val selected =
                                        current?.hierarchy?.any { it.route == destination.route } == true
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = destination.icon,
                                                contentDescription = destination.label,
                                            )
                                        },
                                        label = { Text(destination.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = HearthBackground,
                                            selectedTextColor = HearthBackground,
                                            unselectedIconColor = HearthBackground,
                                            unselectedTextColor = HearthBackground,
                                            indicatorColor = HearthBackground.copy(alpha = 0.18f),
                                        ),
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(destination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    val layoutDirection = LocalLayoutDirection.current
                    val navPadding =
                        if (route == AppDestination.Map.route) {
                            PaddingValues(
                                start = innerPadding.calculateStartPadding(layoutDirection),
                                top = 0.dp,
                                end = innerPadding.calculateEndPadding(layoutDirection),
                                bottom = innerPadding.calculateBottomPadding(),
                            )
                        } else {
                            innerPadding
                        }
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Home.route,
                        modifier = Modifier.padding(navPadding),
                    ) {
                        composable(
                            route = BuildingDetailRoute,
                            arguments = listOf(
                                navArgument("buildingId") { type = NavType.StringType },
                            ),
                        ) { entry ->
                            val buildingId = entry.arguments?.getString("buildingId")
                            if (buildingId != null) {
                                BuildingDetailScreen(
                                    buildingId = buildingId,
                                    onBack = { navController.popBackStack() },
                                    onAddHabitInBuilding = { bid ->
                                        navController.navigate("task_maker/building/$bid")
                                    },
                                    onEditTask = { taskId ->
                                        navController.navigate("task_maker/$taskId")
                                    },
                                    gameStateViewModel = gameVm,
                                )
                            }
                        }
                        composable(AppDestination.Map.route) {
                            MapScreen(
                                ownedBuildingIds = game.ownedBuildingIds,
                                gameUiState = game,
                                onOpenBuilding = { building ->
                                    navController.navigate("building_detail/${building.id}")
                                },
                                onPurchaseBuilding = { buildingId ->
                                    gameVm.tryPurchaseBuilding(buildingId)
                                },
                            )
                        }
                        composable(AppDestination.Home.route) {
                            HomeScreen(
                                welcomeDisplayName = welcomeName,
                                onOpenTasks = { navController.navigate(TaskMakerRoute) },
                                onEditTask = { taskId ->
                                    navController.navigate("task_maker/$taskId")
                                },
                                gameStateViewModel = gameVm,
                            )
                        }
                        composable(AppDestination.Story.route) { StoryScreen() }
                        composable(AppDestination.Profile.route) {
                            ProfileScreen(
                                gameUiState = game,
                                userProgressRepository = userProgressRepository,
                                onLogoutSuccess = { shell = AppShell.NeedLogin },
                            )
                        }
                        composable(
                            route = TaskMakerNewInBuildingRoute,
                            arguments = listOf(
                                navArgument("buildingId") { type = NavType.StringType },
                            ),
                        ) { entry ->
                            val buildingId = entry.arguments?.getString("buildingId")
                            if (buildingId != null) {
                                TaskMakerScreen(
                                    taskId = null,
                                    initialBuildingId = buildingId,
                                    onBack = { navController.popBackStack() },
                                    gameStateViewModel = gameVm,
                                )
                            }
                        }
                        composable(TaskMakerRoute) {
                            TaskMakerScreen(
                                taskId = null,
                                initialBuildingId = null,
                                onBack = { navController.popBackStack() },
                                gameStateViewModel = gameVm,
                            )
                        }
                        composable(
                            route = TaskMakerEditRoute,
                            arguments = listOf(
                                navArgument("taskId") { type = NavType.StringType },
                            ),
                        ) { entry ->
                            val id = entry.arguments?.getString("taskId")
                            if (id != null) {
                                TaskMakerScreen(
                                    taskId = id,
                                    initialBuildingId = null,
                                    onBack = { navController.popBackStack() },
                                    gameStateViewModel = gameVm,
                                )
                            }
                        }
                    }
                }

                if (showBuildingDirectory) {
                    BuildingDirectoryDialog(
                        onDismiss = { showBuildingDirectory = false },
                    )
                }
            }
        }
    }
}
