package com.stevenpg.roundthecorner2.feature.recipients.editor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stevenpg.roundthecorner2.core.designsystem.component.SectionHeader
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.core.domain.validation.RecipientValidation
import com.stevenpg.roundthecorner2.core.messaging.SendResult
import com.stevenpg.roundthecorner2.core.messaging.SmsMode
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.ui.imageVector
import com.stevenpg.roundthecorner2.core.ui.label
import com.stevenpg.roundthecorner2.feature.recipients.R
import kotlinx.coroutines.launch

private const val SLACK_GUIDE_URL = "https://api.slack.com/messaging/webhooks"
private const val DISCORD_GUIDE_URL = "https://support.discord.com/hc/en-us/articles/228383668"

/**
 * Stateful wrapper: owns the Android-specific bits (runtime permission,
 * contact picker, snackbars) and feeds plain values to [RecipientEditorScreen].
 */
@Composable
fun RecipientEditorRoute(
    recipientId: Long?,
    onClose: () -> Unit,
    viewModel: RecipientEditorViewModel = hiltViewModel<RecipientEditorViewModel, RecipientEditorViewModel.Factory>(
        creationCallback = { it.create(recipientId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val testMessage = stringResource(R.string.recipient_editor_test_message)

    // SEND_SMS is only relevant to the sideload build (automatic SMS).
    fun hasSmsPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
        PackageManager.PERMISSION_GRANTED
    var smsPermissionGranted by remember { mutableStateOf(hasSmsPermission()) }
    // Re-check when returning from system Settings.
    LifecycleResumeEffect(Unit) {
        smsPermissionGranted = hasSmsPermission()
        onPauseOrDispose { }
    }
    var testAfterPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        smsPermissionGranted = granted
        if (granted && testAfterPermission) viewModel.onAction(RecipientEditorAction.SendTest(testMessage))
        testAfterPermission = false
    }
    val needsSmsPermission = uiState.channel == MessageChannel.SMS &&
        uiState.smsMode == SmsMode.AUTOMATIC && !smsPermissionGranted

    val contactLauncher = rememberLauncherForActivityResult(PickPhoneNumber()) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.readPickedContact(uri)?.let {
                    viewModel.onAction(RecipientEditorAction.ContactPicked(it.displayName, it.phoneNumber))
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RecipientEditorEvent.Saved, RecipientEditorEvent.Deleted -> onClose()
                is RecipientEditorEvent.InUse -> snackbarHostState.showSnackbar(
                    resources.getQuantityString(
                        R.plurals.recipients_in_use, event.triggerCount, uiState.name, event.triggerCount,
                    ),
                )
                is RecipientEditorEvent.TestFinished -> snackbarHostState.showSnackbar(
                    when (val r = event.result) {
                        SendResult.Sent -> resources.getString(R.string.recipient_editor_test_sent)
                        SendResult.AwaitingUser -> resources.getString(R.string.recipient_editor_test_awaiting)
                        is SendResult.Failed -> resources.getString(R.string.recipient_editor_test_failed, r.detail)
                    },
                )
            }
        }
    }

    RecipientEditorScreen(
        uiState = uiState,
        onAction = { action ->
            if (action is RecipientEditorAction.SendTest && needsSmsPermission) {
                testAfterPermission = true
                permissionLauncher.launch(Manifest.permission.SEND_SMS)
            } else {
                viewModel.onAction(action)
            }
        },
        onClose = onClose,
        testMessage = testMessage,
        needsSmsPermission = needsSmsPermission,
        onRequestSmsPermission = { permissionLauncher.launch(Manifest.permission.SEND_SMS) },
        onPickContact = {
            runCatching { contactLauncher.launch(Unit) } // No contacts app (rare): just type the number.
        },
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientEditorScreen(
    uiState: RecipientEditorUiState,
    onAction: (RecipientEditorAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    testMessage: String = stringResource(R.string.recipient_editor_test_message),
    needsSmsPermission: Boolean = false,
    onRequestSmsPermission: () -> Unit = {},
    onPickContact: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isNew) R.string.recipient_editor_new_title else R.string.recipient_editor_edit_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.recipient_editor_close))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(RecipientEditorAction.Save) },
                        enabled = uiState.canSave,
                        modifier = Modifier.padding(end = Spacing.sm),
                    ) { Text(stringResource(R.string.recipient_editor_save)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
        ) {
            if (uiState.needsReentry) {
                Notice(
                    icon = Icons.Outlined.ErrorOutline,
                    text = stringResource(R.string.recipient_editor_needs_reentry),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }

            SectionHeader(stringResource(R.string.recipient_editor_section_channel))
            ChannelSelector(uiState.channel, onSelected = { onAction(RecipientEditorAction.ChannelSelected(it)) })

            SectionHeader(stringResource(R.string.recipient_editor_section_details))
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onAction(RecipientEditorAction.NameChanged(it)) },
                label = { Text(stringResource(R.string.recipient_editor_name_label)) },
                placeholder = { Text(stringResource(R.string.recipient_editor_name_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(Spacing.md))
            when (uiState.channel) {
                MessageChannel.SMS -> SmsDetails(uiState, onAction, onPickContact, needsSmsPermission, onRequestSmsPermission)
                MessageChannel.SLACK, MessageChannel.DISCORD -> WebhookDetails(uiState, onAction)
            }

            OutlinedButton(
                onClick = { onAction(RecipientEditorAction.SendTest(testMessage)) },
                enabled = uiState.canSendTest,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
            ) {
                if (uiState.isSendingTest) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.recipient_editor_test))
            }

            if (!uiState.isNew) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.recipient_editor_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.recipients_delete_confirm_title, uiState.name)) },
            text = { Text(stringResource(R.string.recipients_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onAction(RecipientEditorAction.Delete)
                }) { Text(stringResource(R.string.recipients_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.recipients_cancel)) }
            },
        )
    }
}

@Composable
private fun ChannelSelector(selected: MessageChannel, onSelected: (MessageChannel) -> Unit) {
    val channels = MessageChannel.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        channels.forEachIndexed { index, channel ->
            SegmentedButton(
                selected = channel == selected,
                onClick = { onSelected(channel) },
                shape = SegmentedButtonDefaults.itemShape(index, channels.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = channel == selected) {
                        Icon(channel.imageVector, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
            ) { Text(stringResource(channel.label), maxLines = 1) }
        }
    }
}

@Composable
private fun SmsDetails(
    uiState: RecipientEditorUiState,
    onAction: (RecipientEditorAction) -> Unit,
    onPickContact: () -> Unit,
    needsSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
) {
    OutlinedTextField(
        value = uiState.address,
        onValueChange = { onAction(RecipientEditorAction.AddressChanged(it)) },
        label = { Text(stringResource(R.string.recipient_editor_phone_label)) },
        trailingIcon = {
            IconButton(onClick = onPickContact) {
                Icon(Icons.Outlined.Contacts, contentDescription = stringResource(R.string.recipient_editor_pick_contact))
            }
        },
        isError = uiState.problem != null,
        supportingText = uiState.problem?.let { { Text(problemText(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.size(Spacing.sm))
    when {
        uiState.smsMode == SmsMode.TAP_TO_SEND -> Notice(
            icon = Icons.Outlined.Info,
            text = stringResource(R.string.recipient_editor_sms_tap_to_send),
        )
        needsSmsPermission -> Notice(
            icon = Icons.Outlined.Lock,
            text = stringResource(R.string.recipient_editor_sms_permission),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            action = {
                FilledTonalButton(onClick = onRequestSmsPermission) {
                    Text(stringResource(R.string.recipient_editor_sms_permission_grant))
                }
            },
        )
        else -> Notice(icon = Icons.Outlined.Info, text = stringResource(R.string.recipient_editor_sms_automatic))
    }
}

@Composable
private fun WebhookDetails(uiState: RecipientEditorUiState, onAction: (RecipientEditorAction) -> Unit) {
    val isSlack = uiState.channel == MessageChannel.SLACK
    val uriHandler = LocalUriHandler.current
    OutlinedTextField(
        value = uiState.address,
        onValueChange = { onAction(RecipientEditorAction.AddressChanged(it)) },
        label = { Text(stringResource(R.string.recipient_editor_webhook_label)) },
        isError = uiState.problem != null,
        supportingText = {
            Text(
                uiState.problem?.let { problemText(it) } ?: stringResource(
                    if (isSlack) R.string.recipient_editor_slack_help else R.string.recipient_editor_discord_help,
                ),
            )
        },
        minLines = 2,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = { uriHandler.openUri(if (isSlack) SLACK_GUIDE_URL else DISCORD_GUIDE_URL) }) {
        Text(stringResource(R.string.recipient_editor_webhook_guide))
        Spacer(Modifier.width(Spacing.xs))
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
    }
    Notice(icon = Icons.Outlined.Lock, text = stringResource(R.string.recipient_editor_secret_note))
}

/** A soft, rounded informational panel with an optional action button. */
@Composable
private fun Notice(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(shape = MaterialTheme.shapes.medium, color = containerColor, contentColor = contentColor, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            if (action != null) {
                Box(Modifier.fillMaxWidth().padding(top = Spacing.sm), contentAlignment = Alignment.CenterEnd) { action() }
            }
        }
    }
}

@Composable
private fun problemText(problem: RecipientValidation.Problem): String = stringResource(
    when (problem) {
        RecipientValidation.Problem.BLANK, RecipientValidation.Problem.INVALID_PHONE_NUMBER -> R.string.recipient_problem_phone
        RecipientValidation.Problem.NOT_HTTPS -> R.string.recipient_problem_https
        RecipientValidation.Problem.NOT_A_SLACK_WEBHOOK -> R.string.recipient_problem_slack
        RecipientValidation.Problem.NOT_A_DISCORD_WEBHOOK -> R.string.recipient_problem_discord
    },
)
