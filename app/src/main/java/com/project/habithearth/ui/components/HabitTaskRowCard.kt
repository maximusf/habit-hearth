package com.project.habithearth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.habithearth.model.HabitTask
import com.project.habithearth.model.TaskCategory
import com.project.habithearth.model.toEpochMs
import com.project.habithearth.ui.theme.outlineColor

@Composable
fun HabitTaskRowCard(
    task: HabitTask,
    onCompletedChange: (Boolean) -> Unit,
    onOpenEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val canCollect = remember(task.collectionLog) {
        val lastMs = task.collectionLog.lastOrNull()?.timestamp?.toEpochMs() ?: 0L
        System.currentTimeMillis() - lastMs > 24L * 60 * 60 * 1000
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        border = BorderStroke(2.dp, task.category.outlineColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenEdit),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${task.category.displayName} · +${task.rewardAmount} " +
                        when (task.category) {
                            TaskCategory.UNSORTED -> "coins"
                            else -> "gems"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = task.category.outlineColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Button(
                onClick = { onCompletedChange(true) },
                enabled = canCollect,
            ) {
                Text("Collect")
            }
        }
    }
}
