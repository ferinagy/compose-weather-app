package com.example.androiddevchallenge

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.sin

@Composable fun WeatherAnimation(ratio: Float, data: DayData, modifier: Modifier = Modifier) {
    val hourlyData = data.hours[(ratio * 24).toInt().coerceAtMost(23)]
    val weather = hourlyData.weather

    val cloudColor: Color = getCloudColor(ratio)

    val alphas = calculateAlphas(weather = weather)

    val infiniteTransition = rememberInfiniteTransition()
    val infiniteAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10_000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier) {
        clipRect {
            drawSky(ratio = ratio)
            drawStars(ratio = ratio, alpha = alphas.sun, infiniteAnim = infiniteAnim)
            drawSun(ratio = ratio, alpha = alphas.sun, infiniteAnim = infiniteAnim)
            drawMoon(ratio = ratio, alpha = alphas.sun)

            drawRain(ratio = ratio, alpha = alphas.rain, infiniteAnim = infiniteAnim)
            drawSnow(alpha = alphas.snow, infiniteAnim = infiniteAnim)
            drawClouds(color = cloudColor, alpha = alphas.cloud, infiniteAnim = infiniteAnim)
            drawMoreClouds(color = cloudColor, alpha = alphas.moreClouds, infiniteAnim = infiniteAnim)
            drawFog(ratio = ratio, alpha = alphas.fog, infiniteAnim = infiniteAnim)
        }
    }
}

private fun getCloudColor(ratio: Float): Color {
    val hour = ratio * 24
    val dayCloudColor = Color.White
    val nightCloudColor = Color(0xff878383)
    return when {
        6 <= hour && hour < 7 -> lerp(dayCloudColor, nightCloudColor, 1 - (hour - 6))
        20 <= hour && hour < 21 -> lerp(dayCloudColor, nightCloudColor, hour - 20)
        7 <= hour && hour < 20 -> dayCloudColor
        else -> nightCloudColor
    }
}

private data class Alphas(
    val sun: Float,
    val cloud: Float,
    val moreClouds: Float,
    val rain: Float,
    val snow: Float,
    val fog: Float
)

private val animationSpec = spring<Float>(stiffness = Spring.StiffnessLow)

@Composable
private fun calculateAlphas(weather: WeatherType): Alphas {
    val cloudAlpha by animateFloatAsState(
        targetValue = if (weather in setOf(
                WeatherType.Overcast,
                WeatherType.Cloudy,
                WeatherType.Rain,
                WeatherType.Snow
            )
        ) 1f else 0f,
        animationSpec = animationSpec
    )

    val moreCloudAlpha by animateFloatAsState(
        targetValue = if (weather in setOf(
                WeatherType.Cloudy,
                WeatherType.Rain,
                WeatherType.Snow
            )
        ) 1f else 0f,
        animationSpec = animationSpec
    )

    val rainAlpha by animateFloatAsState(
        targetValue = if (weather == WeatherType.Rain) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val snowAlpha by animateFloatAsState(
        targetValue = if (weather == WeatherType.Snow) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val fogAlpha by animateFloatAsState(
        targetValue = if (weather == WeatherType.Fog) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    return Alphas(
        sun = 1f,
        cloud = cloudAlpha,
        moreClouds = moreCloudAlpha,
        rain = rainAlpha,
        snow = snowAlpha,
        fog = fogAlpha
    )
}

private fun DrawScope.drawSky(ratio: Float) {
    val dayColor = Color(0xff74a7f7)
    val nightColor = Color(0xff121f33)
    val hour = ratio * 24

    val steps = listOf(
        0f to nightColor,
        6f to nightColor,
        7f to dayColor,
        20f to dayColor,
        21f to nightColor,
        25f to nightColor
    )

    val i = steps.indexOfFirst { hour < it.first }
    val v1 = steps[i - 1]
    val v2 = steps[i]
    val color = lerp(v1.second, v2.second, (hour - v1.first) / (v2.first - v1.first))
    drawRect(color = color, topLeft = Offset.Zero)
}

private fun DrawScope.drawSun(ratio: Float, infiniteAnim: Float, alpha: Float) {
    val hour = ratio * 24
    if (hour < 6 || 21 <= hour) return

    val effectiveRatio = (hour - 6) / 15

    val canvasWidth = size.width
    val canvasHeight = size.height

    val sunColor = Color(0xfffac905)

    withTransform({
        val degrees = -90 + 180 * effectiveRatio
        rotate(degrees = degrees, pivot = center + Offset(0f, canvasHeight * 0.6f))
        translate(left = canvasWidth / 2, top = canvasHeight / 4)
        rotate(degrees = -degrees, pivot = Offset.Zero)
    }) {
        drawCircle(color = sunColor, radius = 40f, alpha = alpha, center = Offset.Zero)
        val rays = 16
        val step = 360f / rays
        val offset = 360f * infiniteAnim
        repeat(rays) {
            rotate(degrees = step * it + offset, pivot = Offset.Zero) {
                val end = if (it % 2 == 0) 80f else 70f
                drawLine(
                    color = sunColor,
                    start = Offset(0f, 45f),
                    end = Offset(0f, end),
                    alpha = alpha,
                    strokeWidth = 5f
                )
            }
        }
    }
}

private fun DrawScope.drawMoon(ratio: Float, alpha: Float) {
    val hour = ratio * 24
    if (6 <= hour && hour < 21) return

    val effectiveHour = if (21 <= hour) hour else hour + 24

    val effectiveRatio = (effectiveHour - 21) / 9

    val canvasWidth = size.width
    val canvasHeight = size.height

    val moonColor = Color(0xffe0e0de)

    withTransform({
        val degrees = -90 + 180 * effectiveRatio
        rotate(degrees = degrees, pivot = center + Offset(0f, canvasHeight * 0.6f))
        translate(left = canvasWidth / 2, top = canvasHeight / 4)

        rotate(degrees = -degrees + 10, pivot = Offset.Zero)

        val oval = Rect(-70f, -40f, 10f, 40f)
        val path = Path().apply { addArc(oval, 0f, 360f) }
        clipPath(path = path, clipOp = ClipOp.Difference)
    }) {
        drawCircle(color = moonColor, radius = 40f, center = Offset.Zero, alpha = alpha)
    }
}

private fun DrawScope.drawStars(ratio: Float, alpha: Float, infiniteAnim: Float) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    withTransform({
        rotate(degrees = 360 * ratio, pivot = center + Offset(-132f, -63f))
        translate(left = canvasWidth / 2, top = canvasHeight / 4)
    }) {
        starOffsets.forEach {
            drawStar(ratio, alpha, it, infiniteAnim)
        }
    }
}

private fun DrawScope.drawStar(ratio: Float, weatherAlpha: Float, offset: Offset, infiniteAnim: Float) {
    val hour = ratio * 24
    if (7 <= hour && hour < 20) return

    val alpha = when {
        6 <= hour && hour < 7 -> 1 - (hour - 6)
        20 <= hour && hour < 21 -> hour - 20
        else -> 1f
    }

    val starColor = Color(0xffe0e0de)

    val randomized = ((offset.x + offset.y) % 20) / 20f + 1
    val final = (infiniteAnim.speedUpAnimValue(4) + randomized) % 1f // <0,1> with some pseudo randomness based on offset
    val eased = (sin(PI / 2 * (4 * final - 1)) / 2).toFloat() // <-0.5,0.5> with continous cycle
    val scale = 1 + eased / 2

    withTransform({
        translate(left = offset.x, top = offset.y)
        scale(scale, scale, Offset.Zero)
    }) {
        val path = Path()
        path.moveTo(0f, -10f)
        path.lineTo(3f, -3f)
        path.lineTo(10f, 0f)
        path.lineTo(3f, 3f)
        path.lineTo(0f, 10f)
        path.lineTo(-3f, 3f)
        path.lineTo(-10f, 0f)
        path.lineTo(-3f, -3f)
        path.close()

        drawPath(color = starColor, alpha = alpha * weatherAlpha, path = path)
    }
}

private fun DrawScope.drawClouds(color: Color, alpha: Float, infiniteAnim: Float) {

    translate(left = size.width / 2, top = size.height / 4) {
        cloudOffsets.forEach { drawCloud(color, alpha, infiniteAnim, it) }
    }
}

private fun DrawScope.drawMoreClouds(color: Color, alpha: Float, infiniteAnim: Float) {
    translate(left = size.width / 2, top = size.height / 4) {
        moreCloudOffsets.forEach { drawCloud(color, alpha, infiniteAnim, it) }
    }
}

private fun DrawScope.drawFog(ratio: Float, alpha: Float, infiniteAnim: Float) {
    val hour = ratio * 24
    val dayColor = Color(0xffe6e8e8)
    val nightColor = Color(0xff909191)
    val fogColor: Color = when {
        6 <= hour && hour < 7 -> lerp(dayColor, nightColor, 1 - (hour - 6))
        20 <= hour && hour < 21 -> lerp(dayColor, nightColor, hour - 20)
        7 <= hour && hour < 20 -> dayColor
        else -> nightColor
    }

    translate(left = size.width / 2, top = size.height / 4) {
        fogRects.forEach { (offset, width) -> drawFogRect(offset, width, fogColor, alpha, infiniteAnim) }
    }
}

private fun DrawScope.drawFogRect(offset: Offset, width: Float, color: Color, alpha: Float, infiniteAnim: Float) {
    val randomized = (offset.y + 100) / 100 % 2 / 2f
    val final = (infiniteAnim + randomized) % 1f // <0,1> with some pseudo randomness based on offset
    val eased = (sin(PI / 2 * (4 * final - 1)) / 2).toFloat() // <-0.5,0.5> with continous cycle

    val animOffset = eased * 100

    drawRoundRect(
        color = color,
        topLeft = offset + Offset(animOffset, 0f),
        size = Size(width, 50f),
        cornerRadius = CornerRadius(25f, 25f),
        alpha = alpha
    )
}

private fun DrawScope.drawRain(ratio: Float, alpha: Float, infiniteAnim: Float) {
    val hour = ratio * 24
    val dayRainColor = Color(0xff2cdfe6)
    val nightRainColor = Color(0xff0f83db)
    val rainColor: Color = when {
        6 <= hour && hour < 7 -> lerp(dayRainColor, nightRainColor, 1 - (hour - 6))
        20 <= hour && hour < 21 -> lerp(dayRainColor, nightRainColor, hour - 20)
        7 <= hour && hour < 20 -> dayRainColor
        else -> nightRainColor
    }

    val start = 0f
    val end = size.height * 3 / 4 + 50f
    val animHeight = end - start
    val animOffset = start + animHeight * infiniteAnim.speedUpAnimValue(3)

    fun Offset.animated() = copy(y = (y + animOffset) % animHeight)

    translate(left = size.width / 2, top = size.height / 4) {
        rainOffsets.forEach {
            drawRaindrop(rainColor, alpha, offset = it.animated())
        }
    }
}

private fun Float.speedUpAnimValue(factor: Int) = (this % (1f / factor)) * factor

private fun DrawScope.drawSnow(alpha: Float, infiniteAnim: Float) {
    val start = 0f
    val end = size.height * 3 / 4 + 50f
    val animHeight = end - start
    val animOffset = start + animHeight * infiniteAnim

    fun Offset.animated() = copy(y = (y + animOffset) % animHeight)

    translate(left = size.width / 2, top = size.height / 4) {
        rainOffsets.forEach {
            drawSnowFlake(alpha, offset = it.animated())
        }
    }
}

private fun DrawScope.drawRaindrop(color: Color, alpha: Float, offset: Offset = Offset.Zero) {
    translate(offset.x, offset.y) {
        val path = Path()

        path.arcTo(
            rect = Rect(-10f, -10f, 10f, 10f),
            startAngleDegrees = -30f,
            sweepAngleDegrees = 240f,
            forceMoveTo = true
        )
        path.lineTo(0f, -20f)

        drawPath(path = path, color = color, alpha = alpha)
    }
}

private fun DrawScope.drawSnowFlake(alpha: Float, offset: Offset = Offset.Zero) {
    translate(offset.x, offset.y) {
        repeat(3) {
            rotate(120f * it, pivot = Offset.Zero) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, -10f),
                    end = Offset(0f, 10f),
                    alpha = alpha,
                    strokeWidth = 3f
                )
            }
        }
    }
}

private fun DrawScope.drawCloud(cloudColor: Color, alpha: Float, infiniteAnim: Float, offset: Offset = Offset.Zero) {
    val randomized = ((offset.x + offset.y) % 20) / 20f + 1
    val final = (infiniteAnim + randomized) % 1f // <0,1> with some pseudo randomness based on offset
    val eased = (sin(PI / 2 * (4 * final - 1)) / 2).toFloat() // <-0.5,0.5> with continous cycle

    val animOffset = eased * 100

    translate(offset.x + animOffset, offset.y) {
        val path = Path()

        path.addOval(Rect(-118.215f, -72.545f, -50.844997f, -5.174999f))
        path.addOval(Rect(-88.625f, -42.565f, -27.735f, 18.325f))
        path.addOval(Rect(-56.105003f, -45.805f, 11.265001f, 21.565002f))
        path.addOval(Rect(-79.785f, -88.935f, -12.414997f, -21.564999f))
        path.addOval(Rect(-31.27f, -68.57f, 8.73f, -28.57f))
        path.addOval(Rect(-2.545f, -78.135f, 54.745003f, -20.845001f))
        path.addOval(Rect(34.36f, -48.43f, 74.36f, -8.43f))
        path.addOval(Rect(-0.38500023f, -43.005f, 54.745003f, 12.125001f))
        path.addOval(Rect(-0.38500023f, -43.005f, 54.745003f, 12.125001f))

        drawPath(path = path, color = cloudColor, alpha = alpha)
    }
}

private val rainOffsets = listOf(
    Offset(0f, 150f),
    Offset(20f, 350f),
    Offset(130f, 210f),
    Offset(170f, 360f),
    Offset(-140f, 180f),
    Offset(-130f, 430f),
    Offset(-70f, 230f),
    Offset(-250f, 229f),
    Offset(-300f, 400f),
    Offset(-340f, 200f),
    Offset(289f, 341f),
    Offset(342f, 161f),
)

private val starOffsets = listOf(
    Offset(100f, 100f),
    Offset(-120f, 70f),
    Offset(-132f, -63f),
    Offset(10f, -45f),
    Offset(341f, 295f),
    Offset(352f, 320f),
    Offset(284f, 10f),
    Offset(-234f, 150f),
    Offset(-220f, -23f),
    Offset(-371f, 78f),
    Offset(-411f, 253f),
    Offset(-31f, 278f),
    Offset(-158f, 392f),
    Offset(158f, -249f),
    Offset(356f, -341f),
    Offset(22f, -459f),
    Offset(-56f, -200f),
    Offset(-58f, -321f),
    Offset(166f, 412f),
    Offset(-294f, -423f),
    Offset(-166f, -333f),
)

private val cloudOffsets = listOf(
    Offset(0f, 0f),
    Offset(-250f, 29f),
    Offset(289f, 41f),
)

private val moreCloudOffsets = listOf(
    Offset(-450f, -15f),
    Offset(351f, 123f),
    Offset(72f, 99f),
    Offset(511f, -69f),
    Offset(534f, 18f),
    Offset(185f, -61f),
    Offset(-162f, -54f),
    Offset(-333f, 100f),
    Offset(-73f, 86f),
)

private val fogRects = listOf(
    Offset(-100f, -30f) to 250f,
    Offset(190f, -30f) to 220f,
    Offset(-360f, -30f) to 220f,
    Offset(-10f, 70f) to 550f,
    Offset(-410f, 70f) to 250f,
    Offset(100f, 170f) to 270f,
    Offset(-100f, 170f) to 170f,
    Offset(-450f, 170f) to 300f,
    Offset(130f, 270f) to 310f,
    Offset(-40f, 270f) to 130f,
    Offset(-240f, 270f) to 180f,
    Offset(-390f, 270f) to 120f,
    Offset(80f, 370f) to 110f,
    Offset(250f, 370f) to 170f,
    Offset(-430f, 370f) to 410f,
)