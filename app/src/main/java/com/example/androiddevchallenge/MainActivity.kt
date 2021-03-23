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

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.androiddevchallenge.ui.theme.MyTheme
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MyTheme {
                MyApp()
            }
        }
    }
}

// Start building your app here!
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyApp() {
    var cities by remember {
        val initial = testCities.take(1)
        mutableStateOf(initial)
    }
    var selected by remember { mutableStateOf(cities[0]) }
    ProvideWindowInsets {
        Surface(color = MaterialTheme.colors.background) {
            val scrollState = rememberScrollState()

            val columns = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 4

            LazyColumn(Modifier.fillMaxSize()) {

                item {
                    val scope = rememberCoroutineScope()
                    WeatherAnimation(
                        ratio = scrollState.getRatio(),
                        data = selected.dayData,
                        modifier = Modifier.fillMaxWidth()
                            .height(300.dp)
                            .scrollable(
                                orientation = Orientation.Horizontal,
                                state = rememberScrollableState { delta ->
                                    scope.launch { scrollState.scrollTo((scrollState.value + delta).roundToInt()) }
                                    delta
                                }
                            )
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    CityHeader(
                        scrollState = scrollState,
                        selected = selected,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Timeline(selected.dayData, scrollState, Modifier.fillMaxWidth()) }
                item { Spacer(modifier = Modifier.height(24.dp)) }

                addCities(
                    columns = columns,
                    cities = cities,
                    selected = selected,
                    onAddCity = {
                        val remaining = testCities - cities
                        if (remaining.isNotEmpty()) {
                            cities = cities + remaining.first()
                        }
                    },
                    onRemoveCity = { cities = cities - it },
                    onSelectCity = { selected = it }
                )
            }
        }
    }
}

@Composable fun CityHeader(scrollState: ScrollState, selected: CityData, modifier: Modifier = Modifier) {
    val ratio = scrollState.getRatio()
    val minutesInDay = 24 * 60 - 1
    val time = (ratio * minutesInDay).roundToInt()
    val hours = time / 60
    val minutes = time % 60

    val hourlyData = selected.dayData.hours[(ratio * 24).toInt().coerceAtMost(23)]
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = selected.city,
            style = MaterialTheme.typography.h4,
            modifier = Modifier.weight(1f)
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "%02d:%02d".format(hours, minutes),
                style = MaterialTheme.typography.subtitle1,
            )
            Text(
                text = "${hourlyData.temperature}˚",
                style = MaterialTheme.typography.h5,
            )
        }
    }
}

@Composable fun TimelineItem(num: Int, data: HourlyData, modifier: Modifier = Modifier) {
    val isNight = num in 0 until 6 || num in 20..23

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(text = "%02d:00".format(num), style = MaterialTheme.typography.subtitle2)
            Icon(
                painter = painterResource(id = data.weather.icon(isNight)),
                contentDescription = "",
                modifier = Modifier.size(36.dp)
            )
            Text(text = "${data.temperature}˚", style = MaterialTheme.typography.subtitle1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun Timeline(data: DayData, scrollState: ScrollState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        val ratio = scrollState.getRatio()

        WeatherSlider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = ratio,
            onValueChange = {
                scope.launch { scrollState.scrollTo((it * scrollState.maxValue).roundToInt()) }
            }
        )

        Row(modifier = Modifier.horizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimelineItem(0, data.hours[0], modifier = Modifier.padding(start = 16.dp))
            repeat(22) {
                TimelineItem(it + 1, data.hours[it + 1])
            }
            TimelineItem(23, data.hours[23], modifier = Modifier.padding(end = 16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class) fun LazyListScope.addCities(
    columns: Int,
    cities: List<CityData>,
    selected: CityData,
    onAddCity: () -> Unit,
    onRemoveCity: (CityData) -> Unit,
    onSelectCity: (CityData) -> Unit
) {
    val items = cities.size + 1
    val rows = (items + columns - 1) / columns

    repeat(rows) { row ->
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (row == 0) 8.dp else 0.dp,
                        start = 8.dp,
                        end = 8.dp,
                        bottom = if (row == rows - 1) 8.dp else 0.dp
                    )
            ) {
                val rowCities = cities.drop(row * columns).take(columns)
                rowCities.forEach {
                    val isSelected = it == selected
                    CityItem(
                        name = it.city,
                        data = it.dayData.hours[10],
                        isNight = false,
                        isSelected = isSelected,
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .combinedClickable(
                                enabled = !isSelected,
                                onClick = { onSelectCity(it) },
                                onLongClick = { onRemoveCity(it) }
                            )
                    )
                }

                if (row == rows - 1) {
                    AddCityItem(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .clickable { onAddCity() }
                    )

                    val spacers = if (items % columns == 0) 0 else columns - items % columns
                    repeat(spacers) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable fun CityItem(
    name: String,
    data: HourlyData,
    isNight: Boolean,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    CityItem(
        title = name,
        painter = painterResource(id = data.weather.icon(isNight)),
        subtitle = "${data.temperature}˚",
        isSelected = isSelected,
        modifier = modifier
    )
}

@Composable
private fun AddCityItem(modifier: Modifier = Modifier) {
    CityItem(title = "Add", icon = Icons.Default.AddCircleOutline, modifier = modifier)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun CityItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null,
    subtitle: String = "",
    isSelected: Boolean = false
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
    }
    Card(
        modifier = modifier,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(text = title, style = MaterialTheme.typography.subtitle1)
                if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = "",
                        modifier = Modifier.size(48.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "",
                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(text = subtitle, style = MaterialTheme.typography.h6)
            }
        }
    }
}

@Preview("TimelineItem")
@Composable
fun TimelineItemPreview() {
    MyTheme {
        Surface(color = MaterialTheme.colors.background) {
            TimelineItem(8, testData.hours.first(), modifier = Modifier.padding(8.dp))
        }
    }
}

@Preview("CiyItem")
@Composable
fun CityItemPreview() {
    MyTheme {
        Surface(color = MaterialTheme.colors.background) {
            CityItem(name = "Tokyo", data = testData.hours[12], isNight = false)
        }
    }
}

@Preview("AddCiyItem")
@Composable
fun AddCityItemPreview() {
    MyTheme {
        Surface(color = MaterialTheme.colors.background) {
            AddCityItem()
        }
    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        MyApp()
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        MyApp()
    }
}

private fun ScrollState.getRatio() = value.toFloat() / maxValue

private fun WeatherType.icon(isNight: Boolean) = when (this) {
    WeatherType.Clear -> if (!isNight) R.drawable.weather_sunny else R.drawable.weather_night
    WeatherType.Overcast -> if (!isNight) R.drawable.weather_partly_cloudy else R.drawable.weather_night_partly_cloudy
    WeatherType.Cloudy -> R.drawable.weather_cloudy
    WeatherType.Rain -> R.drawable.weather_pouring
    WeatherType.Fog -> R.drawable.weather_fog
    WeatherType.Snow -> R.drawable.weather_snowy_heavy
}
