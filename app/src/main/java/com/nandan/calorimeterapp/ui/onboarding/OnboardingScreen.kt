package com.nandan.calorimeterapp.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandan.calorimeterapp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: (weight: String, goal: String) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("Maintain Weight") }
    var expanded by remember { mutableStateOf(false) }

    val goals = listOf("Lose Weight", "Maintain Weight", "Bulk")
    val goalEmoji = mapOf("Lose Weight" to "🔥", "Maintain Weight" to "⚖️", "Bulk" to "💪")

    val w = weight.toDoubleOrNull() ?: 0.0
    val h = height.toDoubleOrNull() ?: 0.0
    val bmi = if (w > 0 && h > 0) w / ((h / 100.0) * (h / 100.0)) else 0.0
    val bmiCategory = when {
        bmi < 18.5 -> Pair("Underweight", AccentBlue)
        bmi < 25.0 -> Pair("Healthy", Emerald)
        bmi < 30.0 -> Pair("Overweight", AccentAmber)
        bmi > 0 -> Pair("Obese", AccentRed)
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(60.dp))

            Text(
                "Let's set up your profile",
                style = MaterialTheme.typography.headlineSmall,
                color = OnBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "We'll calculate your calorie target and BMI",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            // Input fields
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OnboardTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = "Weight (kg)",
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier.weight(1f),
                )
                OnboardTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = "Height (cm)",
                    icon = Icons.Default.Height,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(14.dp))

            OnboardTextField(
                value = age,
                onValueChange = { age = it },
                label = "Age",
                icon = Icons.Default.Cake,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // Goal picker
            Text(
                "What's your goal?",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceMuted,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                goals.forEach { g ->
                    val selected = goal == g
                    FilterChip(
                        selected = selected,
                        onClick = { goal = g },
                        label = {
                            Text(
                                "${goalEmoji[g]} $g",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = Emerald,
                            containerColor = SurfaceCard,
                            labelColor = OnSurfaceMuted,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            selectedBorderColor = Emerald.copy(alpha = 0.5f),
                            borderColor = BorderColor,
                        ),
                    )
                }
            }

            // BMI card
            AnimatedVisibility(
                visible = bmi > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BorderColor),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Your BMI", style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
                                Text(
                                    String.format(Locale.US, "%.1f", bmi),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bmiCategory?.second ?: Emerald,
                                )
                            }
                            bmiCategory?.let { (cat, color) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = color.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        cat,
                                        color = color,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        // BMI Scale bar
                        BmiScaleBar(bmi)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onComplete(weight, goal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald,
                    contentColor = Color(0xFF0D1117),
                    disabledContainerColor = Emerald.copy(alpha = 0.3f),
                ),
                enabled = weight.isNotBlank() && height.isNotBlank(),
            ) {
                Text(
                    "Get Started →",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BmiScaleBar(bmi: Double) {
    val progress = ((bmi - 10) / 30).coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(AccentBlue, Emerald, AccentAmber, AccentRed)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(Color.Transparent)
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .offset(x = (progress * 280).dp - 7.dp, y = (-3).dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, SurfaceCard, CircleShape)
        )
    }
}

@Composable
private fun OnboardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = { Icon(icon, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = Emerald,
            unfocusedLabelColor = OnSurfaceMuted,
            cursorColor = Emerald,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceCard,
        ),
    )
}
