package org.thoughtcrime.securesms.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import org.thoughtcrime.securesms.ui.GetString
import org.thoughtcrime.securesms.ui.LaunchedEffectAsync
import org.thoughtcrime.securesms.ui.LocalButtonColor
import org.thoughtcrime.securesms.ui.colorDestructive
import org.thoughtcrime.securesms.ui.contentDescription
import kotlin.time.Duration.Companion.seconds

val LocalButtonSize = staticCompositionLocalOf { mediumButton }
val LocalButtonShape = staticCompositionLocalOf<Shape> { RoundedCornerShape(percent = 50) }

@Composable
fun Modifier.applyButtonSize() = then(LocalButtonSize.current)

val mediumButton = Modifier.height(41.dp)
val smallButton = Modifier.wrapContentHeight()

@Composable
fun OutlineButton(@StringRes textId: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlineButton(stringResource(textId), modifier, onClick)
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlineButton(modifier.contentDescription(text), onClick = onClick) { Text(text) }
}

@Composable
fun OutlineButton(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    OutlinedButton(
        modifier = modifier.applyButtonSize(),
        interactionSource = interactionSource,
        onClick = onClick,
        border = BorderStroke(1.dp, LocalButtonColor.current),
        shape = LocalButtonShape.current,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LocalButtonColor.current,
            backgroundColor = Color.Unspecified
        )
    ) {
        content()
    }
}

@Composable
fun TemporaryStateButton(
    content: @Composable (MutableInteractionSource, Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }

    var clicked by remember { mutableStateOf(false) }

    content(interactions, clicked)

    LaunchedEffectAsync {
        interactions.releases.collectLatest {
            clicked = true
            delay(2.seconds)
            clicked = false
        }
    }
}

@Composable
fun FilledButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier.size(108.dp, 34.dp),
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colors.background,
            backgroundColor = LocalButtonColor.current
        )
    ) {
        Text(text = text)
    }
}

@Composable
fun BorderlessButtonSecondary(
    text: String,
    onClick: () -> Unit
) {
    BorderlessButton(
        text,
        contentColor = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        onClick = onClick
    )
}

@Composable
fun BorderlessButton(
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: GetString = GetString(text),
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    contentColor: Color = MaterialTheme.colors.onBackground,
    backgroundColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.contentDescription(contentDescription),
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            backgroundColor = backgroundColor
        )
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = fontSize,
            lineHeight = lineHeight,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

private val MutableInteractionSource.releases
    get() = interactions.filter { it is PressInteraction.Release }

@Composable
fun SmallButtons(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalButtonSize provides smallButton) { content() }
}

@Composable
fun DestructiveButtons(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalButtonColor provides colorDestructive) { content() }
}
