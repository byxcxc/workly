package com.workly.app.ui.background

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.workly.app.data.prefs.AppSettings
import com.workly.app.data.prefs.BackgroundMode
import com.workly.app.ui.theme.LocalBaseBackgroundColor

/**
 * Draws the user's chosen background behind the whole app.
 *
 * The picture is blurred by the amount chosen in Settings and then washed with a
 * scrim, which is what keeps text readable over an arbitrary photo. The frosted
 * look comes from the surfaces above it being translucent (see the theme), so the
 * backdrop shows through every card.
 */
@Composable
fun WorklyBackground(
    settings: AppSettings,
    render: BackgroundRender,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Always painted first: it guarantees something opaque under the UI even when
    // the picture is still decoding or could not be read.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalBaseBackgroundColor.current),
    ) {
        when (settings.backgroundMode) {
            BackgroundMode.DEFAULT -> Unit

            BackgroundMode.COLOR -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(settings.backgroundColorArgb)),
            )

            BackgroundMode.IMAGE -> {
                val image = render.image
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur((settings.backgroundBlurPercent / 100f * MAX_BLUR_DP).dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LocalBaseBackgroundColor.current.copy(alpha = SCRIM_ALPHA)),
                    )
                }
            }
        }

        content()
    }
}

/** 100% blur maps to this radius; more than this turns the picture to soup. */
private const val MAX_BLUR_DP = 28f

/** How much the picture is washed out so text stays readable on top of it. */
private const val SCRIM_ALPHA = 0.42f
