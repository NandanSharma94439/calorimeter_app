@file:OptIn(ExperimentalMaterial3Api::class)

package com.nandan.calorimeterapp.ui.home

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.nandan.calorimeterapp.data.model.FoodItem
import com.nandan.calorimeterapp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uid: String,
    currentWeight: String,
    currentGoal: String,
    mInterstitialAd: InterstitialAd?,
    onAdShowComplete: () -> Unit,
    onLogout: () -> Unit,
    onUpdateSettings: (String, String) -> Unit,
    onShowAddFood: () -> Unit,
    onShowCamera: () -> Unit,
    onShowBarcode: () -> Unit,
    onShowStreak: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    streakViewModel: com.nandan.calorimeterapp.ui.streak.StreakViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showFab by remember { mutableStateOf(false) }

    val calorieTarget = when (currentGoal) {
        "Lose Weight" -> (currentWeight.toDoubleOrNull() ?: 70.0) * 22
        "Bulk" -> (currentWeight.toDoubleOrNull() ?: 70.0) * 35
        else -> (currentWeight.toDoubleOrNull() ?: 70.0) * 28
    }.toInt()

    LaunchedEffect(uid) { homeViewModel.loadFoods(uid) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { homeViewModel.clearError() }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            null,
                            tint = Emerald,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Calorimeter",
                            fontWeight = FontWeight.Bold,
                            color = OnBackground,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, null, tint = OnSurfaceMuted)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = OnSurfaceMuted)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background,
                    scrolledContainerColor = SurfaceCard,
                ),
            )
        },
        floatingActionButton = {
            ExpandableFab(
                onTypeFood = {
                    showFab = false
                    onShowAddFood()
                },
                onScanBarcode = {
                    showFab = false
                    onShowBarcode()
                },
                onCameraAI = {
                    showFab = false
                    onShowCamera()
                },
            )
        },
        bottomBar = {
            AndroidView(
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = "ca-app-pub-2842829612476029/4573881460"
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Dashboard header
            item {
                val totals = remember(uiState.foods) {
                    Triple(
                        uiState.foods.sumOf { it.calories },
                        Triple(
                            uiState.foods.sumOf { it.protein },
                            uiState.foods.sumOf { it.carbs },
                            uiState.foods.sumOf { it.fat },
                        ),
                        uiState.foods
                    )
                }
                DashboardCard(
                    consumed = totals.first,
                    target = calorieTarget,
                    protein = totals.second.first,
                    carbs = totals.second.second,
                    fat = totals.second.third,
                    modifier = Modifier.padding(16.dp),
                )
                
                val streakState by streakViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(uid) { streakViewModel.loadStreak(uid) }
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    com.nandan.calorimeterapp.ui.streak.StreakCard(
                        streakData = streakState.streakData,
                        onClick = onShowStreak
                    )
                }
            }

            // Insight banner
            item {
                val foods = uiState.foods
                val protein = foods.sumOf { it.protein }
                val totalCal = foods.sumOf { it.calories }
                InsightBanner(protein, totalCal.toDouble(), calorieTarget, Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(20.dp))
            }

            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Today's Diary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackground,
                    )
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Emerald,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "${uiState.foods.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Empty state
            if (!uiState.isLoading && uiState.foods.isEmpty()) {
                item {
                    EmptyDiaryState(Modifier.padding(32.dp))
                }
            }

            // Food items
            items(uiState.foods, key = { it.id }) { item ->
                FoodCard(
                    item = item,
                    onDelete = {
                        homeViewModel.deleteFood(item.id, uid)
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .animateItem(),
                )
            }
        }

        if (showSettings) {
            SettingsBottomSheet(
                currentWeight = currentWeight,
                currentGoal = currentGoal,
                onDismiss = { showSettings = false },
                onSave = { w, g ->
                    onUpdateSettings(w, g)
                    showSettings = false
                },
            )
        }
    }
}

// ── Dashboard Card ────────────────────────────────────────────────────────────

@Composable
private fun DashboardCard(
    consumed: Int,
    target: Int,
    protein: Double,
    carbs: Double,
    fat: Double,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = (consumed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "calorie_progress",
    )
    val remaining = (target - consumed).coerceAtLeast(0)
    val isOver = consumed > target

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Daily Budget", color = OnSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "$target kcal",
                        color = OnBackground,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Consumed", color = OnSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "$consumed kcal",
                        color = if (isOver) AccentRed else Emerald,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Animated circular progress
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(110.dp),
                        strokeWidth = 10.dp,
                        color = SurfaceElevated,
                        strokeCap = StrokeCap.Round,
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(110.dp),
                        strokeWidth = 10.dp,
                        color = if (isOver) AccentRed else Emerald,
                        trackColor = Color.Transparent,
                        strokeCap = StrokeCap.Round,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$remaining",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (isOver) AccentRed else OnBackground,
                        )
                        Text(
                            if (isOver) "over" else "left",
                            fontSize = 11.sp,
                            color = OnSurfaceMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(16.dp))

            // Macro bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MacroBar("Protein", protein, (target * 0.25 / 4), ProteinColor, Modifier.weight(1f))
                MacroBar("Carbs", carbs, (target * 0.50 / 4), CarbColor, Modifier.weight(1f))
                MacroBar("Fat", fat, (target * 0.25 / 9), FatColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroBar(
    label: String,
    current: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = (current / goal.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(600),
        label = "macro_$label",
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
            Text(
                "${current.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = SurfaceHighlight,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "/ ${goal.toInt()}g",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceFaint,
            fontSize = 10.sp,
        )
    }
}

// ── Insight Banner ────────────────────────────────────────────────────────────

@Composable
private fun InsightBanner(protein: Double, cal: Double, target: Int, modifier: Modifier = Modifier) {
    val (message, icon) = when {
        cal > target -> Pair("You've hit your limit! Stay light on dinner tonight.", Icons.Rounded.Warning)
        protein < 40 -> Pair("Protein is low. Consider chicken, eggs, or lentils.", Icons.Rounded.TipsAndUpdates)
        cal > target * 0.8 -> Pair("Nearly at your goal — great discipline today!", Icons.Rounded.EmojiEvents)
        cal > 0 -> Pair("You're on track. Keep up the great work!", Icons.Rounded.CheckCircle)
        else -> Pair("Log your first meal to get personalized tips.", Icons.Rounded.TipsAndUpdates)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, Emerald.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Emerald, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = OnBackground)
        }
    }
}

// ── Food Card ─────────────────────────────────────────────────────────────────

@Composable
fun FoodCard(
    item: FoodItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp),
        ) {
            // Food image
            Box(
                modifier = Modifier
                    .width(85.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(SurfaceHighlight),
                contentAlignment = Alignment.Center,
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Rounded.Restaurant,
                        null,
                        tint = OnSurfaceFaint,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        item.foodName,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = AccentRed.copy(0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Text(
                    "${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${item.calories} kcal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Emerald,
                    )
                    Text("P ${item.protein.toInt()}g", fontSize = 10.sp, color = ProteinColor)
                    Text("C ${item.carbs.toInt()}g", fontSize = 10.sp, color = CarbColor)
                    Text("F ${item.fat.toInt()}g", fontSize = 10.sp, color = FatColor)
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyDiaryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, BorderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Restaurant,
                null,
                tint = OnSurfaceFaint,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No meals logged yet",
            style = MaterialTheme.typography.titleSmall,
            color = OnBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap the + button to log your first meal",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceMuted,
        )
    }
}

// ── Expandable FAB ────────────────────────────────────────────────────────────

@Composable
private fun ExpandableFab(
    onTypeFood: () -> Unit,
    onScanBarcode: () -> Unit,
    onCameraAI: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FabOption(label = "Camera AI", icon = Icons.Default.CameraAlt, tint = AccentPurple) {
                    expanded = false
                    onCameraAI()
                }
                FabOption(label = "Scan Barcode", icon = Icons.Default.QrCodeScanner, tint = AccentBlue) {
                    expanded = false
                    onScanBarcode()
                }
                FabOption(label = "Type Food", icon = Icons.Default.Edit, tint = Emerald) {
                    expanded = false
                    onTypeFood()
                }
                Spacer(Modifier.height(2.dp))
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = Emerald,
            contentColor = Color(0xFF0D1117),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (expanded) 45f else 0f,
                animationSpec = tween(200),
                label = "fab_rotation",
            )
            Icon(
                Icons.Default.Add,
                "Add food",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun FabOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceCard,
            tonalElevation = 4.dp,
            modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = OnBackground,
            )
        }
        Spacer(Modifier.width(10.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = tint.copy(alpha = 0.15f),
            contentColor = tint,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, tint.copy(alpha = 0.3f), CircleShape),
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Settings bottom sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    currentWeight: String,
    currentGoal: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var weight by remember { mutableStateOf(currentWeight) }
    var goal by remember { mutableStateOf(currentGoal) }
    val goals = listOf("Lose Weight", "Maintain Weight", "Bulk")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = settingsTextFieldColors(),
            )
            Spacer(Modifier.height(16.dp))

            Text("Goal", style = MaterialTheme.typography.labelLarge, color = OnSurfaceMuted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { g ->
                    val sel = goal == g
                    FilterChip(
                        selected = sel,
                        onClick = { goal = g },
                        label = { Text(g, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = Emerald,
                            containerColor = SurfaceHighlight,
                            labelColor = OnSurfaceMuted,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = sel,
                            selectedBorderColor = Emerald.copy(alpha = 0.5f),
                            borderColor = BorderColor,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { onSave(weight, goal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald,
                    contentColor = Color(0xFF0D1117),
                ),
            ) {
                Text("Save Changes", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Emerald,
    unfocusedBorderColor = BorderColor,
    focusedLabelColor = Emerald,
    unfocusedLabelColor = OnSurfaceMuted,
    cursorColor = Emerald,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground,
    focusedContainerColor = SurfaceHighlight,
    unfocusedContainerColor = SurfaceHighlight,
)
