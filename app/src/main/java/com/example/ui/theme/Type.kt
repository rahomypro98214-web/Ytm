package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NotoKufiArabicFamily = FontFamily(
    Font(R.font.noto_kufi_arabic, FontWeight.Normal),
    Font(R.font.noto_kufi_arabic, FontWeight.Bold),
    Font(R.font.noto_kufi_arabic, FontWeight.ExtraBold)
)

val IbmPlexArabicFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Bold)
)

val SanaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 23.sp
    ),
    bodySmall = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)
