package com.nandan.calorimeterapp.ui.add

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.nandan.calorimeterapp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSheet(
    uid: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onScanBarcode: () -> Unit,
    addViewModel: AddFoodViewModel = viewModel(),
) {
    val uiState by addViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val units = listOf("serving", "pieces", "g", "kg", "ml")
    var unitExpanded by remember { mutableStateOf(false) }

    // Handle success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSuccess()
            addViewModel.clearSuccess()
        }
    }

    // Handle errors
    LaunchedEffect(uiState.searchError) {
        uiState.searchError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            addViewModel.clearError()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Log Food",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                )
                IconButton(
                    onClick = onScanBarcode,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHighlight),
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Search field
            OutlinedTextField(
                value = uiState.foodName,
                onValueChange = { addViewModel.onFoodNameChanged(it) },
                label = { Text("Search food...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Emerald,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { keyboard?.hide(); addViewModel.searchNow() }) {
                            Icon(Icons.Default.Search, null, tint = OnSurfaceMuted)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); addViewModel.searchNow() }),
                singleLine = true,
                colors = addSheetFieldColors(),
            )

            Spacer(Modifier.height(12.dp))

            // Quantity + Unit row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = if (uiState.quantity == uiState.quantity.toLong().toDouble())
                        uiState.quantity.toLong().toString()
                    else
                        String.format(Locale.US, "%.1f", uiState.quantity),
                    onValueChange = { it.toDoubleOrNull()?.let { q -> addViewModel.onQuantityChanged(q) } },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(0.35f),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = addSheetFieldColors(),
                )

                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(0.65f),
                ) {
                    OutlinedTextField(
                        value = uiState.unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = addSheetFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false },
                        containerColor = SurfaceElevated,
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u, color = OnBackground) },
                                onClick = { addViewModel.onUnitChanged(u); unitExpanded = false },
                            )
                        }
                    }
                }
            }

            // Food image
            AnimatedVisibility(visible = uiState.imageUrl.isNotEmpty()) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    AsyncImage(
                        model = uiState.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Nutrition info
            AnimatedVisibility(visible = uiState.calories > 0 || uiState.foodName.isNotBlank()) {
                NutritionInfoCard(uiState)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { addViewModel.addFood(uid) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald,
                    contentColor = Color(0xFF0D1117),
                    disabledContainerColor = Emerald.copy(alpha = 0.3f),
                ),
                enabled = !uiState.isSaving && !uiState.isSearching && uiState.foodName.isNotBlank(),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color(0xFF0D1117), strokeWidth = 2.5.dp)
                } else {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add to Diary", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun NutritionInfoCard(uiState: AddFoodUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceHighlight),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NutritionPill("Cal", "${uiState.calories}", Emerald)
            NutritionPill("Protein", "${uiState.protein.toInt()}g", ProteinColor)
            NutritionPill("Carbs", "${uiState.carbs.toInt()}g", CarbColor)
            NutritionPill("Fat", "${uiState.fat.toInt()}g", FatColor)
        }
    }
}

@Composable
private fun NutritionPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 15.sp)
        Text(label, fontSize = 11.sp, color = OnSurfaceMuted)
    }
}

@Composable
private fun addSheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Emerald,
    unfocusedBorderColor = BorderColor,
    focusedLabelColor = Emerald,
    unfocusedLabelColor = OnSurfaceMuted,
    cursorColor = Emerald,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground,
    focusedContainerColor = SurfaceElevated,
    unfocusedContainerColor = SurfaceElevated,
)
