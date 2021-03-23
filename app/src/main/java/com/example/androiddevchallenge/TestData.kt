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

enum class WeatherType { Clear, Overcast, Cloudy, Rain, Fog, Snow }

enum class WindDirection { N, NE, E, SE, S, SW, W, NW }

data class WindData(val direction: WindDirection, val speed: Int)

data class HourlyData(val wind: WindData, val weather: WeatherType, val temperature: Int)

data class DayData(val hours: List<HourlyData>)

val testData = DayData(
    hours = listOf(
        HourlyData(wind = WindData(WindDirection.N, 1), weather = WeatherType.Clear, -2), // 0
        HourlyData(wind = WindData(WindDirection.N, 1), weather = WeatherType.Clear, -2),
        HourlyData(wind = WindData(WindDirection.NW, 1), weather = WeatherType.Overcast, -3),
        HourlyData(wind = WindData(WindDirection.NW, 2), weather = WeatherType.Overcast, -3),
        HourlyData(wind = WindData(WindDirection.NW, 6), weather = WeatherType.Cloudy, -3),
        HourlyData(wind = WindData(WindDirection.N, 7), weather = WeatherType.Cloudy, -3),
        HourlyData(wind = WindData(WindDirection.N, 10), weather = WeatherType.Fog, -4),
        HourlyData(wind = WindData(WindDirection.N, 10), weather = WeatherType.Fog, -3),
        HourlyData(wind = WindData(WindDirection.NW, 10), weather = WeatherType.Fog, -1), // 8
        HourlyData(wind = WindData(WindDirection.NW, 10), weather = WeatherType.Snow, 0),
        HourlyData(wind = WindData(WindDirection.W, 15), weather = WeatherType.Snow, 3),
        HourlyData(wind = WindData(WindDirection.SW, 15), weather = WeatherType.Rain, 3),
        HourlyData(wind = WindData(WindDirection.NW, 15), weather = WeatherType.Rain, 5),
        HourlyData(wind = WindData(WindDirection.NW, 14), weather = WeatherType.Overcast, 5),
        HourlyData(wind = WindData(WindDirection.W, 11), weather = WeatherType.Overcast, 6),
        HourlyData(wind = WindData(WindDirection.W, 10), weather = WeatherType.Clear, 6), // 16
        HourlyData(wind = WindData(WindDirection.W, 7), weather = WeatherType.Clear, 6),
        HourlyData(wind = WindData(WindDirection.W, 7), weather = WeatherType.Clear, 6),
        HourlyData(wind = WindData(WindDirection.SW, 6), weather = WeatherType.Overcast, 6),
        HourlyData(wind = WindData(WindDirection.SW, 7), weather = WeatherType.Overcast, 6),
        HourlyData(wind = WindData(WindDirection.SW, 5), weather = WeatherType.Overcast, 6),
        HourlyData(wind = WindData(WindDirection.S, 3), weather = WeatherType.Rain, 6),
        HourlyData(wind = WindData(WindDirection.S, 3), weather = WeatherType.Rain, 6),
        HourlyData(wind = WindData(WindDirection.S, 3), weather = WeatherType.Clear, 6),
    )
)

data class CityData(val city: String, val dayData: DayData)

val testCities = listOf(
    CityData("San Francisco", testData),
    CityData("Tokyo", testData.copy(hours = testData.hours.shuffled())),
    CityData("New York", testData.copy(hours = testData.hours.shuffled())),
    CityData("London", testData.copy(hours = testData.hours.shuffled())),
    CityData("Moscow", testData.copy(hours = testData.hours.shuffled())),
)
