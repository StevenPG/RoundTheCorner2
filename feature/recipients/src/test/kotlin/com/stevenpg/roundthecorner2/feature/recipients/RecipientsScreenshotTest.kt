package com.stevenpg.roundthecorner2.feature.recipients

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.stevenpg.roundthecorner2.core.messaging.SmsMode
import com.stevenpg.roundthecorner2.core.model.MessageChannel
import com.stevenpg.roundthecorner2.core.testing.SCREENSHOT_SDK
import com.stevenpg.roundthecorner2.core.testing.captureLightAndDark
import com.stevenpg.roundthecorner2.feature.recipients.editor.RecipientEditorScreen
import com.stevenpg.roundthecorner2.feature.recipients.editor.RecipientEditorUiState
import com.stevenpg.roundthecorner2.feature.recipients.list.RecipientsScreen
import com.stevenpg.roundthecorner2.feature.recipients.list.RecipientsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class RecipientsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listEmpty(): Unit = composeRule.captureLightAndDark("recipients_empty") {
        RecipientsScreen(RecipientsUiState.Ready(emptyList()), onAddRecipient = {}, onEditRecipient = {}, onDeleteRecipient = {})
    }

    @Test
    fun listPopulated(): Unit = composeRule.captureLightAndDark("recipients_list") {
        RecipientsScreen(
            RecipientsUiState.Ready(listOf(TestData.family, TestData.alex, TestData.restored, TestData.raidChat)),
            onAddRecipient = {},
            onEditRecipient = {},
            onDeleteRecipient = {},
        )
    }

    @Test
    fun editorSmsTapToSend(): Unit = composeRule.captureLightAndDark("recipient_editor_sms_play") {
        RecipientEditorScreen(
            RecipientEditorUiState(
                isNew = true,
                smsMode = SmsMode.TAP_TO_SEND,
                name = "Alex",
                addresses = mapOf(MessageChannel.SMS to "+1 (555) 555-0100"),
            ),
            onAction = {},
            onClose = {},
        )
    }

    @Test
    fun editorSmsNeedsPermission(): Unit = composeRule.captureLightAndDark("recipient_editor_sms_full") {
        RecipientEditorScreen(
            RecipientEditorUiState(isNew = true, smsMode = SmsMode.AUTOMATIC),
            onAction = {},
            onClose = {},
            needsSmsPermission = true,
        )
    }

    @Test
    fun editorSlackInvalid(): Unit = composeRule.captureLightAndDark("recipient_editor_slack_error") {
        RecipientEditorScreen(
            RecipientEditorUiState(
                isNew = true,
                smsMode = SmsMode.TAP_TO_SEND,
                name = "#family",
                channel = MessageChannel.SLACK,
                addresses = mapOf(MessageChannel.SLACK to TestData.DISCORD_URL),
            ),
            onAction = {},
            onClose = {},
        )
    }

    @Test
    fun editorDiscordExisting(): Unit = composeRule.captureLightAndDark("recipient_editor_discord") {
        RecipientEditorScreen(
            RecipientEditorUiState(
                isNew = false,
                smsMode = SmsMode.TAP_TO_SEND,
                name = "Ride share chat",
                channel = MessageChannel.DISCORD,
                addresses = mapOf(MessageChannel.DISCORD to TestData.DISCORD_URL),
            ),
            onAction = {},
            onClose = {},
        )
    }

    @Test
    fun editorNeedsReentry(): Unit = composeRule.captureLightAndDark("recipient_editor_reentry") {
        RecipientEditorScreen(
            RecipientEditorUiState(isNew = false, smsMode = SmsMode.AUTOMATIC, name = "Mom", needsReentry = true),
            onAction = {},
            onClose = {},
        )
    }
}
