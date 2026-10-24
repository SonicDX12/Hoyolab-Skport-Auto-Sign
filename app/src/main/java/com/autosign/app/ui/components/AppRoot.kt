package com.autosign.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class Tab(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/** Glow background plus a floating tab bar (phones) or a side rail (≥ 600dp); tabs only show when [showTabs]. */
@Composable
fun AppRoot(tabs: List<Tab>, showTabs: Boolean, content: @Composable () -> Unit) {
    // The root Surface provides the content color; without it Text renders black on dark themes
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().glowBackground()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                val wide = isWideScreen()
                Row(Modifier.fillMaxSize()) {
                    if (wide && showTabs) SideRail(tabs)
                    Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                }
                if (!wide && showTabs) FloatingTabBar(tabs, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/** Two soft color halos behind the content, drawn as radial gradients that fade to nothing. */
@Composable
fun Modifier.glowBackground(): Modifier {
    val first = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val second = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
    return drawBehind {
        val radius = size.maxDimension * 0.6f
        val topRight = Offset(size.width, size.height * 0.15f)
        val bottomLeft = Offset(0f, size.height * 0.8f)
        drawCircle(Brush.radialGradient(listOf(first, Color.Transparent), topRight, radius), radius, topRight)
        drawCircle(Brush.radialGradient(listOf(second, Color.Transparent), bottomLeft, radius), radius, bottomLeft)
    }
}

@Composable
fun FloatingTabBar(tabs: List<Tab>, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp), // keep it opaque
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
        modifier = modifier.padding(bottom = 16.dp)
    ) {
        Row(Modifier.padding(6.dp)) {
            tabs.forEach { tab ->
                Surface(
                    selected = tab.selected,
                    onClick = tab.onClick,
                    shape = CircleShape,
                    color = if (tab.selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (tab.selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = if (tab.selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.width(104.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
                        Icon(tab.icon, null)
                        Text(tab.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/** Large-screen counterpart of [FloatingTabBar]: tab pills stacked left of the content. */
@Composable
fun SideRail(tabs: List<Tab>) {
    Row {
        Column(
            modifier = Modifier.width(200.dp).fillMaxHeight().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            tabs.forEach { tab ->
                Surface(
                    selected = tab.selected,
                    onClick = tab.onClick,
                    shape = CircleShape,
                    color = if (tab.selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (tab.selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        if (tab.selected) 1.5.dp else 1.dp,
                        if (tab.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(tab.icon, null)
                        Spacer(Modifier.width(12.dp))
                        Text(tab.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Box(Modifier.padding(vertical = 48.dp).width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
    }
}
