package com.autosign.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalReducedMotion = staticCompositionLocalOf { false }

// MaterialTheme.motionScheme (Material3 1.4.0) is still internal in the resolved BOM version,
// so these M3-token-equivalent specs (medium spatial / short effects, standard easing) are the
// public fallback — swap for MaterialTheme.motionScheme once it's a public API.
fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = tween(durationMillis = 300, easing = FastOutSlowInEasing)
fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = tween(durationMillis = 150, easing = LinearOutSlowInEasing)

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
