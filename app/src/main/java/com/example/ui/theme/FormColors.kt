package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

@Composable
fun lyfTextFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        disabledTextColor = scheme.onSurfaceVariant,
        focusedPlaceholderColor = scheme.onSurfaceVariant,
        unfocusedPlaceholderColor = scheme.onSurfaceVariant,
        focusedContainerColor = scheme.surfaceContainerHigh,
        unfocusedContainerColor = scheme.surfaceContainerHigh,
        disabledContainerColor = scheme.surfaceContainer,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outline,
        cursorColor = scheme.primary,
        focusedLabelColor = scheme.onSurfaceVariant,
        unfocusedLabelColor = scheme.onSurfaceVariant
    )
}
