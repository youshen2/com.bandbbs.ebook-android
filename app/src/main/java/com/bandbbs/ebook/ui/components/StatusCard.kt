package com.bandbbs.ebook.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun StatusCard(
    statusText: String,
    descriptionText: String,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "StatusCardColor"
    )
    val contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    val icon: ImageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = contentColor
            )
            Column(modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isConnected) descriptionText else "点击重试连接",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            if (!isConnected) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重连",
                    modifier = Modifier.size(24.dp),
                    tint = contentColor
                )
            }
        }
    }
}
