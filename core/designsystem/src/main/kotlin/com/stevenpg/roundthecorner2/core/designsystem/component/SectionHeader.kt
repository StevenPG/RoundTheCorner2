package com.stevenpg.roundthecorner2.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing

/** Small, primary-tinted label that introduces a group of rows or cards. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = Spacing.xs, top = Spacing.xl, bottom = Spacing.sm)
            .semantics { heading() }, // Lets TalkBack users jump between sections.
    )
}
