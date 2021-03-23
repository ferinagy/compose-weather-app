/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun WeatherSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors()
) {
    val scope = rememberCoroutineScope()
    val position = remember(valueRange, steps, scope) {
        SliderPosition(value, valueRange, steps, scope, onValueChange)
    }
    position.onValueChange = onValueChange
    position.scaledValue = value
    BoxWithConstraints(
        modifier.sliderSemantics(value, position, enabled, onValueChange, valueRange, steps)
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val maxPx = constraints.maxWidth.toFloat()
        val minPx = 0f
        position.setBounds(minPx, maxPx)

        val gestureEndAction: (Float) -> Unit = { velocity: Float ->
            onValueChangeFinished?.invoke()
        }

        val press = if (enabled) {
            Modifier.pointerInput(position, interactionSource, maxPx, isRtl) {
                detectTapGestures(
                    onPress = { pos ->
                        position.snapTo(if (isRtl) maxPx - pos.x else pos.x)
                        val interaction = PressInteraction.Press(pos)
                        coroutineScope {
                            launch {
                                interactionSource.emit(interaction)
                            }
                            try {
                                val success = tryAwaitRelease()
                                if (success) gestureEndAction(0f)
                                launch {
                                    interactionSource.emit(PressInteraction.Release(interaction))
                                }
                            } catch (c: CancellationException) {
                                launch {
                                    interactionSource.emit(PressInteraction.Cancel(interaction))
                                }
                            }
                        }
                    }
                )
            }
        } else {
            Modifier
        }

        val drag = Modifier.draggable(
            orientation = Orientation.Horizontal,
            reverseDirection = isRtl,
            enabled = enabled,
            interactionSource = interactionSource,
            onDragStopped = { velocity -> gestureEndAction(velocity) },
            startDragImmediately = position.holder.isRunning,
            state = rememberDraggableState {
                position.snapTo(position.holder.value + it)
            }
        )
        val coerced = value.coerceIn(position.startValue, position.endValue)
        val fraction = calcFraction(position.startValue, position.endValue, coerced)
        WeatherSliderImpl(
            enabled,
            fraction,
            position.tickFractions,
            colors,
            maxPx,
            interactionSource,
            modifier = press.then(drag)
        )
    }
}

@Composable
private fun WeatherSliderImpl(
    enabled: Boolean,
    positionFraction: Float,
    tickFractions: List<Float>,
    colors: SliderColors,
    width: Float,
    interactionSource: MutableInteractionSource,
    modifier: Modifier
) {
    val widthDp = with(LocalDensity.current) {
        width.toDp()
    }
    Box(modifier.then(DefaultSliderConstraints)) {
        val thumbSize = ThumbRadius * 2
        val offset = (widthDp - thumbSize) * positionFraction
        val center = Modifier.align(Alignment.CenterStart)

        val trackStrokeWidth: Float
        val thumbPx: Float
        with(LocalDensity.current) {
            trackStrokeWidth = TrackHeight.toPx()
            thumbPx = ThumbRadius.toPx()
        }
        Track(
            center.fillMaxSize(),
            colors,
            enabled,
            positionFraction,
            tickFractions,
            thumbPx,
            trackStrokeWidth
        )
        Box(center.padding(start = offset)) {
            val interactions = remember { mutableStateListOf<Interaction>() }

            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> interactions.add(interaction)
                        is PressInteraction.Release -> interactions.remove(interaction.press)
                        is PressInteraction.Cancel -> interactions.remove(interaction.press)
                        is DragInteraction.Start -> interactions.add(interaction)
                        is DragInteraction.Stop -> interactions.remove(interaction.start)
                        is DragInteraction.Cancel -> interactions.remove(interaction.start)
                    }
                }
            }

            val hasInteraction = interactions.isNotEmpty()
            val elevation = if (hasInteraction) {
                ThumbPressedElevation
            } else {
                ThumbDefaultElevation
            }
            Surface(
                shape = GenericShape { size, direction ->
                    arcTo(Rect(0f, 0f, size.width, size.width), 160f, 220f, true)
                    lineTo(size.width / 2, size.height)
                },
                color = colors.thumbColor(enabled).value,
                elevation = if (enabled) elevation else 0.dp,
                modifier = Modifier
                    .focusable(interactionSource = interactionSource)
                    .indication(
                        interactionSource = interactionSource,
                        indication = rememberRipple(
                            bounded = false,
                            radius = ThumbRippleRadius
                        )
                    )
            ) {
                Spacer(Modifier.size(thumbSize, thumbSize * 2))
            }
        }
    }
}

private class SliderPosition(
    initial: Float = 0f,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    val scope: CoroutineScope,
    var onValueChange: (Float) -> Unit
) {

    internal val startValue: Float = valueRange.start
    internal val endValue: Float = valueRange.endInclusive

    init {
        require(steps >= 0) {
            "steps should be >= 0"
        }
    }

    internal var scaledValue: Float = initial
        set(value) {
            val scaled = scale(startValue, endValue, value, startPx, endPx)
            // floating point error due to rescaling
            if ((scaled - holder.value) > floatPointMistakeCorrection) {
                snapTo(scaled)
            }
        }

    private val floatPointMistakeCorrection = (valueRange.endInclusive - valueRange.start) / 100

    private var endPx = Float.MAX_VALUE
    private var startPx = Float.MIN_VALUE

    internal fun setBounds(min: Float, max: Float) {
        if (startPx == min && endPx == max) return
        val newValue = scale(startPx, endPx, holder.value, min, max)
        startPx = min
        endPx = max
        holder.updateBounds(min, max)
        anchorsPx = tickFractions.map {
            lerp(startPx, endPx, it)
        }
        snapTo(newValue)
    }

    internal val tickFractions: List<Float> =
        if (steps == 0) emptyList() else List(steps + 2) { it.toFloat() / (steps + 1) }

    internal var anchorsPx: List<Float> = emptyList()
        private set

    internal val holder = Animatable(scale(startValue, endValue, initial, startPx, endPx))

    internal fun snapTo(newValue: Float) {
        scope.launch {
            holder.snapTo(newValue)
            onHolderValueUpdated(holder.value)
        }
    }

    internal val onHolderValueUpdated: (value: Float) -> Unit = {
        onValueChange(scale(startPx, endPx, it, startValue, endValue))
    }
}

private fun Modifier.sliderSemantics(
    value: Float,
    position: SliderPosition,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
): Modifier {
    val coerced = value.coerceIn(position.startValue, position.endValue)
    return semantics(mergeDescendants = true) {
        if (!enabled) disabled()
        setProgress(
            action = { targetValue ->
                val newValue = targetValue.coerceIn(position.startValue, position.endValue)
                val resolvedValue = if (steps > 0) {
                    position.tickFractions
                        .map { lerp(position.startValue, position.endValue, it) }
                        .minByOrNull { abs(it - newValue) } ?: newValue
                } else {
                    newValue
                }
                // This is to keep it consistent with AbsSeekbar.java: return false if no
                // change from current.
                if (resolvedValue == coerced) {
                    false
                } else {
                    onValueChange(resolvedValue)
                    true
                }
            }
        )
    }.progressSemantics(value, valueRange, steps)
}

@Composable
private fun Track(
    modifier: Modifier,
    colors: SliderColors,
    enabled: Boolean,
    positionFraction: Float,
    tickFractions: List<Float>,
    thumbPx: Float,
    trackStrokeWidth: Float
) {
    val inactiveTrackColor = colors.trackColor(enabled, active = false)
    val activeTrackColor = colors.trackColor(enabled, active = true)
    val inactiveTickColor = colors.tickColor(enabled, active = false)
    val activeTickColor = colors.tickColor(enabled, active = true)
    Canvas(modifier) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(thumbPx, center.y)
        val sliderRight = Offset(size.width - thumbPx, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        drawLine(
            inactiveTrackColor.value,
            sliderStart,
            sliderEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        val sliderValue = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * positionFraction,
            center.y
        )

        drawLine(
            activeTrackColor.value,
            sliderStart,
            sliderValue,
            trackStrokeWidth,
            StrokeCap.Round
        )
        tickFractions.groupBy { it > positionFraction }.forEach { (afterFraction, list) ->
            drawPoints(
                list.map {
                    Offset(androidx.compose.ui.geometry.lerp(sliderStart, sliderEnd, it).x, center.y)
                },
                PointMode.Points,
                (if (afterFraction) inactiveTickColor else activeTickColor).value,
                trackStrokeWidth,
                StrokeCap.Round
            )
        }
    }
}

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

private val ThumbRadius = 10.dp
private val TrackHeight = 4.dp

private val ThumbRippleRadius = 10.dp
private val ThumbDefaultElevation = 1.dp
private val ThumbPressedElevation = 6.dp

private val SliderHeight = 48.dp
private val SliderMinWidth = 144.dp // TODO: clarify min width
private val DefaultSliderConstraints =
    Modifier.widthIn(min = SliderMinWidth)
        .heightIn(max = SliderHeight)

private fun lerp(start: Float, stop: Float, fraction: Float) =
    (start * (1 - fraction) + stop * fraction)
