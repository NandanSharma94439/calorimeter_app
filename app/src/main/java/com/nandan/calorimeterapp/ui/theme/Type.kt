package com.nandan.calorimeterapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.nandan.calorimeterapp.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val InterFont = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Bold),
)

val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Bold,     fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Bold,     fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Bold,     fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Bold,     fontSize = 32.sp),
    headlineMedium= TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Bold,     fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge    = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium   = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 16.sp, letterSpacing = 0.15.sp),
    titleSmall    = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Normal,   fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Normal,   fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Normal,   fontSize = 12.sp),
    labelLarge    = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 14.sp),
    labelMedium   = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 12.sp),
    labelSmall    = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 0.5.sp),
)