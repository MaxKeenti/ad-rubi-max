package com.example.animochat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.ChatMessage
import com.example.animochat.data.model.ChatRole
import com.example.animochat.data.model.FeedbackCategory
import com.example.animochat.ui.theme.AnimochatTheme

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    feedback: AiFeedback?,
    modifier: Modifier = Modifier,
) {
    val isStudent = message.role == ChatRole.STUDENT
    val colors = MaterialTheme.colorScheme
    val bubbleColor = if (isStudent) colors.primaryContainer else colors.surfaceContainerHigh
    val contentColor = if (isStudent) colors.onPrimaryContainer else colors.onSurface

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isStudent) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(
                topStart = 8.dp,
                topEnd = 8.dp,
                bottomStart = if (isStudent) 8.dp else 2.dp,
                bottomEnd = if (isStudent) 2.dp else 8.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!isStudent && feedback != null) {
                    FeedbackDetails(feedback = feedback)
                }
            }
        }
    }
}

@Composable
private fun FeedbackDetails(feedback: AiFeedback) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (feedback.nextSteps.isNotEmpty()) {
            Text(
                text = "Siguientes pasos",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                feedback.nextSteps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        feedback.followUpQuestion?.let { question ->
            Text(
                text = question,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (feedback.category == FeedbackCategory.CRISIS_OR_UNSAFE && feedback.resources.isNotEmpty()) {
            CrisisSupportCard(feedback = feedback)
        }
    }
}

@Composable
private fun CrisisSupportCard(feedback: AiFeedback) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Apoyo inmediato",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                feedback.resources.forEach { resource ->
                    Column(
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.38f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = resource.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = resource.value,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ChatMessageBubblePreview() {
    AnimochatTheme {
        ChatMessageBubble(
            message = fakeChatUiState().messages.last(),
            feedback = fakeChatUiState().latestFeedback,
            modifier = Modifier.padding(16.dp)
        )
    }
}
