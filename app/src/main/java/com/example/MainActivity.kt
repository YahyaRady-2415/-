package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.CelebrationBurstOverlay
import com.example.ui.components.CelWinterBackground
import com.example.ui.screens.AdhkarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MoreSettingsScreen
import com.example.ui.screens.PrayersScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.theme.LocalCelColors
import com.example.ui.theme.TariqAlJannahTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory

object AppRoutes {
    const val DASHBOARD = "dashboard"
    const val PRAYERS = "prayers"
    const val QURAN = "quran"
    const val ADHKAR = "adhkar"
    const val MORE = "more"
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val vm: MainViewModel = viewModel(factory = MainViewModelFactory(context))
            val settings by vm.settings.collectAsStateWithLifecycle()

            TariqAlJannahTheme(darkTheme = settings.isDarkMode) {
                TariqAlJannahAppShell(vm = vm)
            }
        }
    }
}

@Composable
fun TariqAlJannahAppShell(vm: MainViewModel) {
    val cel = LocalCelColors.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.DASHBOARD

    val settings by vm.settings.collectAsStateWithLifecycle()
    val todayPrayerTimes by vm.todayPrayerTimes.collectAsStateWithLifecycle()
    val allPrayerLogs by vm.allPrayerLogs.collectAsStateWithLifecycle()
    val prayerStats by vm.prayerStats.collectAsStateWithLifecycle()
    val allNaflLogs by vm.allNaflLogs.collectAsStateWithLifecycle()
    val nawafilStats by vm.nawafilStats.collectAsStateWithLifecycle()
    val customNaflRoutines by vm.customNaflRoutines.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val quranDailyLogs by vm.allQuranDailyLogs.collectAsStateWithLifecycle()
    val surahHifzList by vm.surahHifzList.collectAsStateWithLifecycle()
    val quranChapterProgressToday by vm.quranChapterProgressToday.collectAsStateWithLifecycle()
    val audioState by vm.audioState.collectAsStateWithLifecycle()
    val dhikrProgressToday by vm.dhikrProgressToday.collectAsStateWithLifecycle()
    val customAdhkar by vm.customAdhkar.collectAsStateWithLifecycle()
    val hisnChapters by vm.hisnChapters.collectAsStateWithLifecycle()
    val customCategories by vm.customCategories.collectAsStateWithLifecycle()
    val compassReading by vm.compassReading.collectAsStateWithLifecycle()
    val achievements by vm.achievements.collectAsStateWithLifecycle()
    val selectedDateIso by vm.selectedDateIso.collectAsStateWithLifecycle()
    val celebration by vm.celebration.collectAsStateWithLifecycle()
    val locationStatusMessage by vm.locationStatusMessage.collectAsStateWithLifecycle()

    var prayersInitialTab by remember { mutableIntStateOf(0) }

    val navItems = remember {
        listOf(
            NavDestinationItem(AppRoutes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Home, Icons.Outlined.Home),
            NavDestinationItem(AppRoutes.PRAYERS, R.string.nav_prayers, Icons.Filled.Mosque, Icons.Outlined.Mosque),
            NavDestinationItem(AppRoutes.QURAN, R.string.nav_quran, Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
            NavDestinationItem(AppRoutes.ADHKAR, R.string.nav_adhkar, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
            NavDestinationItem(AppRoutes.MORE, R.string.nav_more, Icons.Filled.Settings, Icons.Outlined.Settings),
        )
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CelWinterBackground(snowfallEnabled = settings.snowfallEnabled) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail) {
                        Surface(
                            color = cel.cardSurface,
                            border = BorderStroke(2.dp, cel.cardBorder),
                        ) {
                            NavigationBar(
                                containerColor = cel.cardSurface,
                                tonalElevation = 0.dp,
                            ) {
                                navItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    val label = stringResource(id = item.labelRes)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (item.route == AppRoutes.PRAYERS) prayersInitialTab = 0
                                            navigateTo(item.route)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = label,
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = if (cel.isDark) Color(0xFF040D1A) else Color.White,
                                            selectedTextColor = cel.primaryAccent,
                                            indicatorColor = cel.primaryAccent,
                                            unselectedIconColor = cel.textSecondary,
                                            unselectedTextColor = cel.textSecondary,
                                        ),
                                        modifier = Modifier.testTag("nav_${item.route}"),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(containerColor = cel.cardSurface) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                val label = stringResource(id = item.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoutes.DASHBOARD,
                            modifier = Modifier
                                .widthIn(max = 760.dp)
                                .fillMaxSize(),
                        ) {
                            composable(AppRoutes.DASHBOARD) {
                                DashboardScreen(
                                    settings = settings,
                                    currentHijriDate = vm.currentHijriDate,
                                    todayHeader = vm.todayHeader,
                                    todayIso = vm.todayIso,
                                    prayerTimes = todayPrayerTimes,
                                    prayerLogs = allPrayerLogs,
                                    prayerStats = prayerStats,
                                    naflLogs = allNaflLogs,
                                    quranLogs = quranDailyLogs,
                                    dhikrProgressToday = dhikrProgressToday,
                                    surahs = surahs,
                                    dailyWisdom = vm.dailyWisdom,
                                    onToggleTheme = { vm.toggleDarkMode(!settings.isDarkMode) },
                                    onToggleSnow = { vm.toggleSnowfall(!settings.snowfallEnabled) },
                                    onQuickMarkPrayer = { prayer, status, mode ->
                                        vm.updatePrayerStatus(vm.todayIso, prayer, status, mode)
                                    },
                                    onNavigateToPrayers = { tab ->
                                        prayersInitialTab = tab
                                        navigateTo(AppRoutes.PRAYERS)
                                    },
                                    onNavigateToQuran = { navigateTo(AppRoutes.QURAN) },
                                    onNavigateToAdhkar = { navigateTo(AppRoutes.ADHKAR) },
                                    onNavigateToMore = { navigateTo(AppRoutes.MORE) },
                                )
                            }

                            composable(AppRoutes.PRAYERS) {
                                PrayersScreen(
                                    initialTab = prayersInitialTab,
                                    settings = settings,
                                    todayIso = vm.todayIso,
                                    selectedDateIso = selectedDateIso,
                                    prayerTimes = todayPrayerTimes,
                                    allPrayerLogs = allPrayerLogs,
                                    prayerStats = prayerStats,
                                    allNaflLogs = allNaflLogs,
                                    nawafilStats = nawafilStats,
                                    customNaflRoutines = customNaflRoutines,
                                    customCategories = customCategories,
                                    compassReading = compassReading,
                                    onSelectDate = vm::selectCalendarDate,
                                    onUpdatePrayer = vm::updatePrayerStatus,
                                    onTogglePrayerCompletion = vm::togglePrayerCompletion,
                                    onToggleNafl = vm::toggleNaflCompleted,
                                    onAddCustomNaflRoutine = vm::addCustomNaflRoutine,
                                    onDeleteCustomNaflRoutine = vm::deleteCustomNaflRoutine,
                                    onRequestLocationUpdate = vm::refreshLocationFromGps,
                                )
                            }

                            composable(AppRoutes.QURAN) {
                                QuranScreen(
                                    surahs = surahs,
                                    settings = settings,
                                    todayIso = vm.todayIso,
                                    quranDailyLogs = quranDailyLogs,
                                    surahHifzList = surahHifzList,
                                    chapterProgressToday = quranChapterProgressToday,
                                    audioState = audioState,
                                    onUpdateBookmark = vm::updateBookmark,
                                    onLogReadingToday = vm::logQuranReadingToday,
                                    onCompleteKhatmah = vm::completeKhatmah,
                                    onToggleChapterRead = vm::toggleChapterRead,
                                    onUpdateSurahHifz = vm::updateSurahHifz,
                                    onUpdateWirdTarget = vm::updateWirdTarget,
                                    onUpdateDisplay = vm::updateQuranDisplay,
                                    onSelectReciter = vm::selectReciter,
                                    onPlaySurahAudio = vm::playSurahAudio,
                                    onToggleAudioPlayPause = vm::toggleAudioPlayPause,
                                    onSeekAudio = vm::seekAudio,
                                    onSetAudioSpeed = vm::setAudioSpeed,
                                    onStopAudio = vm::stopAudio,
                                )
                            }

                            composable(AppRoutes.ADHKAR) {
                                AdhkarScreen(
                                    hisnChapters = hisnChapters,
                                    progressToday = dhikrProgressToday,
                                    customAdhkar = customAdhkar,
                                    customCategories = customCategories,
                                    onIncrementDhikr = vm::incrementDhikr,
                                    onResetDhikr = vm::resetDhikr,
                                    onToggleFavoriteDhikr = vm::toggleFavoriteDhikr,
                                    onAddCustomDhikr = vm::addCustomDhikr,
                                    onDeleteCustomDhikr = vm::deleteCustomDhikr,
                                )
                            }

                            composable(AppRoutes.MORE) {
                                MoreSettingsScreen(
                                    settings = settings,
                                    achievements = achievements,
                                    locationStatusMessage = locationStatusMessage,
                                    onToggleDarkMode = vm::toggleDarkMode,
                                    onToggleSnowfall = vm::toggleSnowfall,
                                    onSelectCityPreset = vm::selectCityPreset,
                                    onUpdateCalculationMethod = vm::updateCalculationMethod,
                                    onUpdateAsrMadhab = vm::updateAsrMadhab,
                                    onUpdatePrayerNotifications = vm::updatePrayerNotifications,
                                    onUpdateReminder = vm::updateReminder,
                                    onSendTestNotification = vm::sendTestNotification,
                                    onRefreshGpsLocation = vm::refreshLocationFromGps,
                                    onExportBackupJson = vm::exportBackupJson,
                                )
                            }
                        }
                    }
                }
            }

            CelebrationBurstOverlay(
                triggerKey = celebration.triggerKey,
                label = celebration.label,
                onFinished = vm::clearCelebration,
            )
        }
    }
}
