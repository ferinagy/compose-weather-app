
# Composed Weather

<!--- Replace <OWNER> with your Github Username and <REPOSITORY> with the name of your repository. -->
<!--- You can find both of these in the url bar when you open your repository in github. -->
![Workflow result](https://github.com/ghus-raba/compose-weather-app/workflows/Check/badge.svg)


## :scroll: Description
This is a simple weather app made with Jetpack Compose for the final week of Android Dev Challenge. It uses dummy data 
to show 24 hours of forecast for a single city. The city can be selected from a list below. User can add new cities by 
clicking the add button and remove them by long press.


## :bulb: Motivation and Context
<!--- Optionally point readers to interesting parts of your submission. -->
<!--- What are you especially proud of? -->
In this app I played around with animations. The current weather is shown animated in the main image at the top - clouds 
move, rain and snow is falling, stars twinkle :-). User can move the animation to another time of day - then sun and 
moon are travelling across sky, stars rotate and color of the sky animates smoothly from day to night and back. 
Different weather types also animate in and out based on the data and time of day shown. 

The time of day displayed can be controlled in 3 ways:
 - dragging across the weather animation
 - scrolling the weather row with hourly forecast
 - moving the slider above the weather row - this acts as a scroll bar of sorts with the nice side effect of the pin 
   always pointing to current time in the weather row
   
All 3 of these share a single source of truth, so changing any one of them will also change the other 2.

## :camera_flash: Screenshots
<!-- You can add more screenshots here if you like -->
<img src="/results/screenshot_1.png" width="260">&emsp;<img src="/results/screenshot_2.png" width="260">


Here is a low quality (so it can be embedded here) gif of app in action. For better quality, look at [video](/results/video.mp4).
![video](https://user-images.githubusercontent.com/2743419/112151489-90e8a400-8be1-11eb-808c-9d97aef64834.gif)


## License
```
Copyright 2020 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
