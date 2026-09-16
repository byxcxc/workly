package com.workly.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Workly's own icon set.
 *
 * The app only needs a handful of glyphs, so they are drawn here as vectors in a
 * single, consistent line style instead of depending on
 * `androidx.compose.material:material-icons-extended`, which is a very large and
 * no longer maintained artifact. `Icon` tints them like any other vector.
 *
 * Every glyph is drawn on the standard 24x24 grid with a 1.8dp stroke and round
 * caps, so they all share the same optical weight.
 */
private const val STROKE = 1.8f

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

/** Stroked (outline) glyph. */
private fun ImageVector.Builder.line(name: String = "line", block: PathBuilder.() -> Unit) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        name = name,
        pathBuilder = block,
    )
}

/** Solid glyph. */
private fun ImageVector.Builder.solid(name: String = "solid", block: PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Black),
        name = name,
        pathBuilder = block,
    )
}

/** Rounded rectangle sub-path, reused by several glyphs. */
private fun PathBuilder.roundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float) {
    moveTo(x + radius, y)
    lineTo(x + width - radius, y)
    quadTo(x + width, y, x + width, y + radius)
    lineTo(x + width, y + height - radius)
    quadTo(x + width, y + height, x + width - radius, y + height)
    lineTo(x + radius, y + height)
    quadTo(x, y + height, x, y + height - radius)
    lineTo(x, y + radius)
    quadTo(x, y, x + radius, y)
    close()
}

/** Three bars of increasing height: the Statistics tab. */
val BarChartIcon: ImageVector by lazy {
    icon("Workly.BarChart") {
        line("chart") {
            moveTo(5.2f, 20f)
            lineTo(5.2f, 13.5f)
            moveTo(12f, 20f)
            lineTo(12f, 8.5f)
            moveTo(18.8f, 20f)
            lineTo(18.8f, 4.5f)
        }
    }
}

/** A month view: the calendar toggle in Records. */
val CalendarIcon: ImageVector by lazy {
    icon("Workly.Calendar") {
        line("frame") { roundedRect(3.5f, 5f, 17f, 15.5f, 3.5f) }
        line("hanger") {
            moveTo(8f, 3f)
            lineTo(8f, 6.5f)
            moveTo(16f, 3f)
            lineTo(16f, 6.5f)
        }
        line("divider") {
            moveTo(3.5f, 10f)
            lineTo(20.5f, 10f)
        }
    }
}

/** Bulleted rows: the list view in Records. */
val ListIcon: ImageVector by lazy {
    icon("Workly.List") {
        solid("bullets") {
            roundedRect(4f, 6.2f, 2.2f, 2.2f, 1.1f)
            roundedRect(4f, 10.9f, 2.2f, 2.2f, 1.1f)
            roundedRect(4f, 15.6f, 2.2f, 2.2f, 1.1f)
        }
        line("rows") {
            moveTo(9.5f, 7.3f)
            lineTo(20f, 7.3f)
            moveTo(9.5f, 12f)
            lineTo(20f, 12f)
            moveTo(9.5f, 16.7f)
            lineTo(20f, 16.7f)
        }
    }
}

/** A house: the Home tab. */
val HomeIcon: ImageVector by lazy {
    icon("Workly.Home") {
        line("house") {
            moveTo(3.6f, 10.6f)
            lineTo(12f, 3.8f)
            lineTo(20.4f, 10.6f)
            lineTo(20.4f, 19.4f)
            lineTo(3.6f, 19.4f)
            close()
        }
        line("door") {
            moveTo(9.4f, 19.4f)
            lineTo(9.4f, 13.6f)
            lineTo(14.6f, 13.6f)
            lineTo(14.6f, 19.4f)
        }
    }
}

/** Sliders: the Settings tab (simpler and calmer than a gear). */
val SettingsIcon: ImageVector by lazy {
    icon("Workly.Settings") {
        line("rails") {
            moveTo(4f, 7.5f)
            lineTo(20f, 7.5f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 16.5f)
            lineTo(20f, 16.5f)
        }
        solid("knobs") {
            roundedRect(7f, 5.6f, 3.2f, 3.8f, 1.4f)
            roundedRect(13.8f, 10.1f, 3.2f, 3.8f, 1.4f)
            roundedRect(7f, 14.6f, 3.2f, 3.8f, 1.4f)
        }
    }
}

/** Filled triangle: "Start work". */
val PlayIcon: ImageVector by lazy {
    icon("Workly.Play") {
        solid("triangle") {
            moveTo(8f, 4.8f)
            lineTo(19.5f, 12f)
            lineTo(8f, 19.2f)
            close()
        }
    }
}

/** Tick: "Finish work" and confirmation. */
val CheckIcon: ImageVector by lazy {
    icon("Workly.Check") {
        line("tick") {
            moveTo(4.8f, 12.6f)
            lineTo(9.8f, 17.6f)
            lineTo(19.2f, 6.6f)
        }
    }
}

/** Chevron: back navigation. */
val BackIcon: ImageVector by lazy {
    icon("Workly.Back") {
        line("chevron") {
            moveTo(15f, 4.5f)
            lineTo(8.2f, 12f)
            lineTo(15f, 19.5f)
        }
    }
}

/** Trash can: delete a record. */
val DeleteIcon: ImageVector by lazy {
    icon("Workly.Delete") {
        line("lid") {
            moveTo(4f, 6.8f)
            lineTo(20f, 6.8f)
            moveTo(9.4f, 6.8f)
            lineTo(9.4f, 4.2f)
            lineTo(14.6f, 4.2f)
            lineTo(14.6f, 6.8f)
        }
        line("body") {
            moveTo(6.4f, 6.8f)
            lineTo(7.3f, 20.2f)
            lineTo(16.7f, 20.2f)
            lineTo(17.6f, 6.8f)
        }
        line("ribs") {
            moveTo(10.4f, 10.4f)
            lineTo(10.4f, 16.6f)
            moveTo(13.6f, 10.4f)
            lineTo(13.6f, 16.6f)
        }
    }
}

/** Plus sign: add a record or work type. */
val AddIcon: ImageVector by lazy {
    icon("Workly.Add") {
        line("plus") {
            moveTo(12f, 4.8f)
            lineTo(12f, 19.2f)
            moveTo(4.8f, 12f)
            lineTo(19.2f, 12f)
        }
    }
}

/** Chevron pointing right, used on tappable setting rows. */
val ChevronRightIcon: ImageVector by lazy {
    icon("Workly.ChevronRight") {
        line("chevron") {
            moveTo(9.5f, 4.5f)
            lineTo(16.3f, 12f)
            lineTo(9.5f, 19.5f)
        }
    }
}

/** Pencil: edit an existing record. */
val EditIcon: ImageVector by lazy {
    icon("Workly.Edit") {
        line("body") {
            moveTo(6f, 18f)
            lineTo(6f, 15f)
            lineTo(15.5f, 5.5f)
            lineTo(18.5f, 8.5f)
            lineTo(9f, 18f)
            close()
        }
        line("ferrule") {
            moveTo(13.6f, 7.4f)
            lineTo(16.6f, 10.4f)
        }
    }
}
