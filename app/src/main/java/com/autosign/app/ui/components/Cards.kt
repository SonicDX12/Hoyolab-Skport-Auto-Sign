package com.autosign.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Room left under scrolling content so the floating bottom bar never hides its last item. */
val BottomBarClearance = 112.dp

/** Widest a single column of content grows to on large screens, so lines stay readable. */
val ContentMaxWidth = 840.dp

/** True on screens wide enough for two panes, such as an unfolded Fold or a tablet. */
@Composable
fun isWideScreen() = LocalConfiguration.current.screenWidthDp >= 600

/** Fills the screen but keeps the content a centered column no wider than [ContentMaxWidth]. */
fun Modifier.centeredContent() = fillMaxSize()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = ContentMaxWidth)

val SectionShape = RoundedCornerShape(18.dp)
val InnerShape = RoundedCornerShape(14.dp)

@Composable
fun cardFill(): Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

/** Rounded, outlined card every section of the app is built from. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(SectionShape)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier),
        shape = SectionShape,
        color = cardFill(),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(content = content)
    }
}

/** Large screen title, with optional subtitle, back icon and round action buttons on the right. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null) {
            navigationIcon()
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

/** Round tonal button used in [ScreenHeader] (add, run…). */
@Composable
fun HeaderIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    enabled: Boolean = true
) {
    Surface(
        shape = CircleShape,
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else cardFill(),
        contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(52.dp)
    ) {
        IconButton(onClick = onClick, enabled = enabled, content = icon)
    }
}

@Composable
fun SectionTitle(text: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Icon, title, description and trailing content laid out as one settings row. */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

/** A settings row in its own card, leading to another screen or action. */
@Composable
fun SettingsItem(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
    }
) {
    SectionCard(onClick = onClick, enabled = enabled, modifier = if (enabled) Modifier else Modifier.alpha(0.38f)) {
        SettingsRow(
            title = title,
            subtitle = subtitle,
            icon = icon?.let { { Icon(it, null, tint = MaterialTheme.colorScheme.primary) } },
            trailing = trailing
        )
    }
}

/** A settings row in its own card with a switch; tapping anywhere toggles it. */
@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    SettingsItem(
        title = title, subtitle = subtitle, icon = icon, enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = { ToggleSwitch(checked = checked, enabled = enabled) }
    )
}

/** Switch showing a check or a cross in its thumb. Not clickable itself: its row is. */
@Composable
fun ToggleSwitch(checked: Boolean, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        enabled = enabled,
        colors = SwitchDefaults.colors(checkedIconColor = MaterialTheme.colorScheme.primary),
        thumbContent = {
            Icon(if (checked) Icons.Filled.Check else Icons.Filled.Close, null, Modifier.size(SwitchDefaults.IconSize))
        }
    )
}

/** Small rounded label, such as a status or a provider. */
@Composable
fun Pill(
    text: String,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(shape = CircleShape, color = containerColor, contentColor = contentColor) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Card stacking [rows] with dividers between them. */
@Composable
fun DividedCard(
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    SectionCard(modifier = modifier, borderColor = borderColor) {
        rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            row()
        }
    }
}

/** Selectable row of a [DividedCard]: a check circle, a title, an optional subtitle and badge. */
@Composable
fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.38f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            leading != null -> Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { leading() }
            selected -> Surface(shape = CircleShape, color = colors.primary, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Check, null, Modifier.padding(6.dp), tint = colors.onPrimary)
            }
            else -> Box(Modifier.size(32.dp).border(2.dp, colors.outline, CircleShape))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        badge?.invoke()
    }
}

/** Large pill-shaped action button; [primary] fills it, otherwise it is outlined. */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, null)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
    val buttonModifier = modifier.height(56.dp)
    if (primary) {
        Button(onClick = onClick, enabled = enabled, modifier = buttonModifier, content = content)
    } else {
        OutlinedButton(
            onClick = onClick, enabled = enabled, modifier = buttonModifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            content = content
        )
    }
}
