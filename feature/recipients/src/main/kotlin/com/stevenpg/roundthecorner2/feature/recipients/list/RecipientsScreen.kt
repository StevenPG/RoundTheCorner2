package com.stevenpg.roundthecorner2.feature.recipients.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stevenpg.roundthecorner2.core.designsystem.component.EmptyState
import com.stevenpg.roundthecorner2.core.designsystem.component.ListCard
import com.stevenpg.roundthecorner2.core.designsystem.component.ListCardAction
import com.stevenpg.roundthecorner2.core.designsystem.component.RtcScreen
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.core.model.Recipient
import com.stevenpg.roundthecorner2.core.model.RecipientDestination
import com.stevenpg.roundthecorner2.core.model.RecipientId
import com.stevenpg.roundthecorner2.core.ui.ChannelBadge
import com.stevenpg.roundthecorner2.core.ui.label
import com.stevenpg.roundthecorner2.feature.recipients.R

@Composable
fun RecipientsRoute(
    onAddRecipient: () -> Unit,
    onEditRecipient: (RecipientId) -> Unit,
    viewModel: RecipientsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(
                when (message) {
                    is RecipientsMessage.Deleted -> resources.getString(R.string.recipients_deleted, message.name)
                    is RecipientsMessage.InUse -> resources.getQuantityString(
                        R.plurals.recipients_in_use, message.triggerCount, message.name, message.triggerCount,
                    )
                },
            )
        }
    }

    RecipientsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddRecipient = onAddRecipient,
        onEditRecipient = { onEditRecipient(it.id) },
        onDeleteRecipient = viewModel::delete,
    )
}

@Composable
fun RecipientsScreen(
    uiState: RecipientsUiState,
    onAddRecipient: () -> Unit,
    onEditRecipient: (Recipient) -> Unit,
    onDeleteRecipient: (Recipient) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    RtcScreen(
        title = stringResource(R.string.recipients_title),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRecipient,
                icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                text = { Text(stringResource(R.string.recipients_add)) },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            RecipientsUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            is RecipientsUiState.Ready ->
                if (uiState.recipients.isEmpty()) {
                    BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
                        Box(
                            Modifier.verticalScroll(rememberScrollState()).fillMaxWidth().heightIn(min = maxHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                icon = Icons.Outlined.Group,
                                title = stringResource(R.string.recipients_empty_title),
                                body = stringResource(R.string.recipients_empty_body),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.lg,
                            end = Spacing.lg,
                            top = innerPadding.calculateTopPadding() + Spacing.sm,
                            bottom = innerPadding.calculateBottomPadding() + 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(uiState.recipients, key = { it.id.value }) { recipient ->
                            RecipientRow(
                                recipient = recipient,
                                onClick = { onEditRecipient(recipient) },
                                onDelete = { pendingDeleteId = recipient.id.value },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
        }
    }

    val pending = (uiState as? RecipientsUiState.Ready)?.recipients?.find { it.id.value == pendingDeleteId }
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.recipients_delete_confirm_title, pending.name)) },
            text = { Text(stringResource(R.string.recipients_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId = null
                    onDeleteRecipient(pending)
                }) { Text(stringResource(R.string.recipients_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.recipients_cancel)) }
            },
        )
    }
}

@Composable
private fun RecipientRow(
    recipient: Recipient,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListCard(
        modifier = modifier,
        onClick = onClick,
        leading = { ChannelBadge(recipient.destination.channel) },
        headline = { Text(recipient.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supporting = {
            if (recipient.needsReentry) {
                Text(stringResource(R.string.recipients_needs_reentry), color = MaterialTheme.colorScheme.error)
            } else {
                Text(recipient.destination.summary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        moreActionsDescription = stringResource(R.string.recipients_more_actions, recipient.name),
        actions = listOf(
            ListCardAction(stringResource(R.string.recipients_edit), Icons.Outlined.Edit, onClick),
            ListCardAction(stringResource(R.string.recipients_delete), Icons.Outlined.Delete, onDelete),
        ),
    )
}

/**
 * Phone numbers are shown in full; webhook URLs are secrets, so only the
 * service and the last few characters are shown (enough to tell them apart).
 */
@Composable
private fun RecipientDestination.summary(): String = when (this) {
    is RecipientDestination.Sms -> phoneNumber
    is RecipientDestination.SlackWebhook, is RecipientDestination.DiscordWebhook ->
        stringResource(R.string.recipients_webhook_summary, stringResource(channel.label), address.takeLast(4))
}
