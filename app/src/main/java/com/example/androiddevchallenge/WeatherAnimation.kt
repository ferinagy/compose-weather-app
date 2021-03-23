package com.example.androiddevchallenge

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

@Composable fun WeatherAnimation(ratio: Float, data: DayData, modifier: Modifier = Modifier) {
    val hourlyData = data.hours[(ratio * 24).toInt().coerceAtMost(23)]
    val weather = hourlyData.weather

    val cloudColor: Color = getCloudColor(ratio)

    val alphas = calculateAlphas(weather = weather)

    Canvas(modifier = modifier) {
        clipRect {
            drawSky(ratio = ratio)
            drawStars(ratio = ratio, alpha = alphas.sun)
            drawSun(ratio = ratio, alpha = alphas.sun)
            drawMoon(ratio = ratio, alpha = alphas.sun)

            drawRain(ratio, alphas.rain)
            drawSnow(alphas.snow)
            drawClouds(color = cloudColor, alpha = alphas.cloud)
            drawMoreClouds(color = cloudColor, alpha = alphas.moreClouds)
            drawFog(ratio, alphas.fog)
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

private data class Alphas(val sun: Float, val cloud: Float, val moreClouds: Float, val rain: Float, val snow: Float, val fog: Float)
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

    val rainAlpha by animateFloatAsState(targetValue = if (weather == WeatherType.Rain) 1f else 0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val snowAlpha by animateFloatAsState(targetValue = if (weather == WeatherType.Snow) 1f else 0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val fogAlpha by animateFloatAsState(targetValue = if (weather == WeatherType.Fog) 1f else 0f, animationSpec = spring(stiffness = Spring.StiffnessLow))

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
    val color = androidx.compose.ui.graphics.lerp(v1.second, v2.second, (hour - v1.first) / (v2.first - v1.first))
    drawRect(color = color, topLeft = Offset.Zero)
}

private fun DrawScope.drawSun(ratio: Float, alpha: Float) {
    val hour = ratio * 24
    if (hour < 6 || 21 <= hour) return

    val effectiveRatio = (hour - 6) / 15

    val canvasWidth = size.width
    val canvasHeight = size.height

    val sunColor = Color(0xfffac905)

    withTransform({
        rotate(degrees = -90 + 180 * effectiveRatio, pivot = center + Offset(0f, canvasHeight * 0.6f))
        translate(left = canvasWidth / 2, top = canvasHeight / 4)
    }) {
        drawCircle(color = sunColor, radius = 40f, alpha = alpha, center = Offset.Zero)
        val rays = 16
        val step = 360f / rays
        repeat(rays) {
            rotate(degrees = step * it, pivot = Offset.Zero) {
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

private fun DrawScope.drawStars(ratio: Float, alpha: Float) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    withTransform({
        rotate(degrees = 360 * ratio, pivot = center + Offset(-132f, -63f))
        translate(left = canvasWidth / 2, top = canvasHeight / 4)
    }) {
        drawStar(ratio, alpha, Offset(100f, 100f))
        drawStar(ratio, alpha, Offset(-120f, 70f))
        drawStar(ratio, alpha, Offset(-132f, -63f))
        drawStar(ratio, alpha, Offset(10f, -45f))
        drawStar(ratio, alpha, Offset(341f, 295f))
        drawStar(ratio, alpha, Offset(352f, 320f))
        drawStar(ratio, alpha, Offset(284f, 10f))
        drawStar(ratio, alpha, Offset(-234f, 150f))
        drawStar(ratio, alpha, Offset(-220f, -23f))
        drawStar(ratio, alpha, Offset(-371f, 78f))
        drawStar(ratio, alpha, Offset(-411f, 253f))
        drawStar(ratio, alpha, Offset(-31f, 278f))
        drawStar(ratio, alpha, Offset(-158f, 392f))
        drawStar(ratio, alpha, Offset(158f, -249f))
        drawStar(ratio, alpha, Offset(356f, -341f))
        drawStar(ratio, alpha, Offset(22f, -459f))
        drawStar(ratio, alpha, Offset(-56f, -200f))
        drawStar(ratio, alpha, Offset(-58f, -321f))
        drawStar(ratio, alpha, Offset(166f, 412f))
        drawStar(ratio, alpha, Offset(-294f, -423f))
        drawStar(ratio, alpha, Offset(-166f, -333f))
    }
}

private fun DrawScope.drawStar(ratio: Float, weatherAlpha: Float, offset: Offset) {
    val hour = ratio * 24
    if (7 <= hour && hour < 20) return

    val alpha = when {
        6 <= hour && hour < 7 -> 1 - (hour - 6)
        20 <= hour && hour < 21 -> hour - 20
        else -> 1f
    }

    val starColor = Color(0xffe0e0de)

    withTransform({
        translate(left = offset.x, top = offset.y)
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

private fun DrawScope.drawClouds(color: Color, alpha: Float) {

    translate(left = size.width / 2, top = size.height / 4) {
        drawCloud(color, alpha)
        drawCloud(color, alpha, offset = Offset(-250f, 29f))
        drawCloud(color, alpha, offset = Offset(289f, 41f))
    }
}

private fun DrawScope.drawMoreClouds(color: Color, alpha: Float) {
    translate(left = size.width / 2, top = size.height / 4) {
        drawCloud(color, alpha, offset = Offset(-450f, -15f))
        drawCloud(color, alpha, offset = Offset(351f, 123f))
        drawCloud(color, alpha, offset = Offset(72f, 99f))
        drawCloud(color, alpha, offset = Offset(511f, -69f))
        drawCloud(color, alpha, offset = Offset(534f, 18f))
        drawCloud(color, alpha, offset = Offset(185f, -61f))
        drawCloud(color, alpha, offset = Offset(-162f, -54f))
        drawCloud(color, alpha, offset = Offset(-333f, 100f))
        drawCloud(color, alpha, offset = Offset(-73f, 86f))
    }
}

private fun DrawScope.drawFog(ratio: Float, alpha: Float) {
    val hour = ratio * 24
    val dayColor = Color(0xffe6e8e8)
    val nightColor = Color(0xff909191)
    val fogColor: Color = when {
        6 <= hour && hour < 7 -> lerp(dayColor, nightColor, 1 - (hour - 6))
        20 <= hour && hour < 21 -> lerp(dayColor, nightColor, hour - 20)
        7 <= hour && hour < 20 -> dayColor
        else -> nightColor
    }

    fun drawFogRect(offset: Offset, width: Float) = drawRoundRect(
        color = fogColor,
        topLeft = offset,
        size = Size(width, 50f),
        cornerRadius = CornerRadius(25f, 25f),
        alpha = alpha
    )

    translate(left = size.width / 2, top = size.height / 4) {
        drawFogRect(offset = Offset(-100f, -30f), width = 250f)
        drawFogRect(offset = Offset(190f, -30f), width = 220f)
        drawFogRect(offset = Offset(-360f, -30f), width = 220f)
        drawFogRect(offset = Offset(-10f, 70f), width = 550f)
        drawFogRect(offset = Offset(-410f, 70f), width = 250f)
        drawFogRect(offset = Offset(100f, 170f), width = 270f)
        drawFogRect(offset = Offset(-100f, 170f), width = 170f)
        drawFogRect(offset = Offset(-450f, 170f), width = 300f)
        drawFogRect(offset = Offset(130f, 270f), width = 310f)
        drawFogRect(offset = Offset(-40f, 270f), width = 130f)
        drawFogRect(offset = Offset(-240f, 270f), width = 180f)
        drawFogRect(offset = Offset(-390f, 270f), width = 120f)
        drawFogRect(offset = Offset(80f, 370f), width = 110f)
        drawFogRect(offset = Offset(250f, 370f), width = 170f)
        drawFogRect(offset = Offset(-430f, 370f), width = 410f)
    }
}

private fun DrawScope.drawRain(ratio: Float, alpha: Float) {
    val hour = ratio * 24
    val dayRainColor = Color(0xff2cdfe6)
    val nightRainColor = Color(0xff0f83db)
    val rainColor: Color = when {
        6 <= hour && hour < 7 -> lerp(dayRainColor, nightRainColor, 1 - (hour - 6))
        20 <= hour && hour < 21 -> lerp(dayRainColor, nightRainColor, hour - 20)
        7 <= hour && hour < 20 -> dayRainColor
        else -> nightRainColor
    }

    translate(left = size.width / 2, top = size.height / 4) {
        drawRaindrop(rainColor, alpha, offset = Offset(0f, 150f))
        drawRaindrop(rainColor, alpha, offset = Offset(20f, 350f))
        drawRaindrop(rainColor, alpha, offset = Offset(130f, 210f))
        drawRaindrop(rainColor, alpha, offset = Offset(170f, 360f))
        drawRaindrop(rainColor, alpha, offset = Offset(-140f, 180f))
        drawRaindrop(rainColor, alpha, offset = Offset(-130f, 430f))
        drawRaindrop(rainColor, alpha, offset = Offset(-70f, 230f))
        drawRaindrop(rainColor, alpha, offset = Offset(-250f, 229f))
        drawRaindrop(rainColor, alpha, offset = Offset(-300f, 400f))
        drawRaindrop(rainColor, alpha, offset = Offset(-340f, 200f))
        drawRaindrop(rainColor, alpha, offset = Offset(289f, 341f))
        drawRaindrop(rainColor, alpha, offset = Offset(342f, 161f))
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

private fun DrawScope.drawSnow(alpha: Float) {
    translate(left = size.width / 2, top = size.height / 4) {
        drawSnowFlake(alpha, offset = Offset(0f, 150f))
        drawSnowFlake(alpha, offset = Offset(20f, 350f))
        drawSnowFlake(alpha, offset = Offset(130f, 210f))
        drawSnowFlake(alpha, offset = Offset(170f, 360f))
        drawSnowFlake(alpha, offset = Offset(-140f, 180f))
        drawSnowFlake(alpha, offset = Offset(-130f, 430f))
        drawSnowFlake(alpha, offset = Offset(-70f, 230f))
        drawSnowFlake(alpha, offset = Offset(-250f, 229f))
        drawSnowFlake(alpha, offset = Offset(-300f, 400f))
        drawSnowFlake(alpha, offset = Offset(-340f, 200f))
        drawSnowFlake(alpha, offset = Offset(289f, 341f))
        drawSnowFlake(alpha, offset = Offset(342f, 161f))
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

private fun DrawScope.drawCloud(cloudColor: Color, alpha: Float, offset: Offset = Offset.Zero) {
    translate(offset.x, offset.y) {
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