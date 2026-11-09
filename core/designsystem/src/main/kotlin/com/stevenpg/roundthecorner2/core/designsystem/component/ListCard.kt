package com.stevenpg.roundthecorner2.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** One entry in a [ListCard]'s overflow (⋮) menu. */
data class ListCardAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * The standard row for saved things (places, recipients, triggers): a rounded
 * tonal card with a leading visual, two lines of text and an overflow menu.
 */
@Composable
fun ListCard(
    headline: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    supporting: (@Composable () -> Unit)? = null,
    actions: List<ListCardAction> = emptyList(),
    moreActionsDescription: String? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = leading,
            headlineContent = headline,
            supportingContent = supporting,
            trailingContent = if (actions.isEmpty()) {
                null
            } else {
                {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = moreActionsDescription)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            actions.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.label) },
                                    leadingIcon = { Icon(action.icon, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        action.onClick()
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}
