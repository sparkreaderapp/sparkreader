/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* Modifications Copyright 2025 The SparkReader Creator
* Licensed under the Apache License, Version 2.0 (the "License");
* Changes relative to the original are documented in docs/upstream/google-ai-edge-gallery/CHANGES.md
* (original source: https://github.com/google-ai-edge/gallery)
*/

package app.sparkreader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.sparkreader.proto.Theme

private val lightScheme =
  lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
  )

private val darkScheme =
  darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
  )

@Immutable
data class CustomColors(
  val taskBgColors: List<Color> = listOf(),
  val taskIconColors: List<Color> = listOf(),
  val taskIconShapeBgColor: Color = Color.Transparent,
  val homeBottomGradient: List<Color> = listOf(),
  val userBubbleBgColor: Color = Color.Transparent,
  val agentBubbleBgColor: Color = Color.Transparent,
  //val linkColor: Color = Color.Transparent,
  val successColor: Color = Color.Transparent,
  val recordButtonBgColor: Color = Color.Transparent,
  val waveFormBgColor: Color = Color.Transparent,

  val linkColor: Color = Color.Transparent,
  val onLinkColor: Color = Color.Transparent,
  val linkColorContainer: Color = Color.Transparent,
  val onLinkColorContainer: Color = Color.Transparent,
  val temporalTag: Color = Color.Transparent,
  val onTemporalTag: Color = Color.Transparent,
  val temporalTagContainer: Color = Color.Transparent,
  val onTemporalTagContainer: Color = Color.Transparent,
  val regionalTag: Color = Color.Transparent,
  val onRegionalTag: Color = Color.Transparent,
  val regionalTagContainer: Color = Color.Transparent,
  val onRegionalTagContainer: Color = Color.Transparent,
  val disciplineTag: Color = Color.Transparent,
  val onDisciplineTag: Color = Color.Transparent,
  val disciplineTagContainer: Color = Color.Transparent,
  val onDisciplineTagContainer: Color = Color.Transparent,
  val genreFictionTag: Color = Color.Transparent,
  val onGenreFictionTag: Color = Color.Transparent,
  val genreFictionTagContainer: Color = Color.Transparent,
  val onGenreFictionTagContainer: Color = Color.Transparent,
  val genreNonFictionTag: Color = Color.Transparent,
  val onGenreNonFictionTag: Color = Color.Transparent,
  val genreNonFictionTagContainer: Color = Color.Transparent,
  val onGenreNonFictionTagContainer: Color = Color.Transparent
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

val lightCustomColors = CustomColors(
  taskBgColors = listOf(
    // green
    Color(0xFFE1F6DE),
    // blue
    Color(0xFFEDF0FF),
    // yellow
    Color(0xFFFFEFC9),
    // red
    Color(0xFFFFEDE6),
  ),
  taskIconColors = listOf(
    Color(0xFF34A853), // green
    Color(0xFF1967D2), // blue
    Color(0xFFE37400), // yellow/orange
    Color(0xFFD93025), // red
  ),
  taskIconShapeBgColor = Color.White,
  homeBottomGradient = listOf(Color(0x00F8F9FF), Color(0xffFFEFC9)),
  agentBubbleBgColor = Color(0xFFe9eef6),
  userBubbleBgColor = Color(0xFF32628D),
  linkColor = Color(0xFF32628D),
  successColor = Color(0xff3d860b),
  recordButtonBgColor = Color(0xFFEE675C),
  waveFormBgColor = Color(0xFFaaaaaa),

  onLinkColor = onLinkColorLight,
  linkColorContainer = linkColorContainerLight,
  onLinkColorContainer = onLinkColorContainerLight,
  temporalTag = temporalTagLight,
  onTemporalTag = onTemporalTagLight,
  temporalTagContainer = temporalTagContainerLight,
  onTemporalTagContainer = onTemporalTagContainerLight,
  regionalTag = regionalTagLight,
  onRegionalTag = onRegionalTagLight,
  regionalTagContainer = regionalTagContainerLight,
  onRegionalTagContainer = onRegionalTagContainerLight,
  disciplineTag = disciplineTagLight,
  onDisciplineTag = onDisciplineTagLight,
  disciplineTagContainer = disciplineTagContainerLight,
  onDisciplineTagContainer = onDisciplineTagContainerLight,
  genreFictionTag = genreFictionTagLight,
  onGenreFictionTag = onGenreFictionTagLight,
  genreFictionTagContainer = genreFictionTagContainerLight,
  onGenreFictionTagContainer = onGenreFictionTagContainerLight,
  genreNonFictionTag = genreNonFictionTagLight,
  onGenreNonFictionTag = onGenreNonFictionTagLight,
  genreNonFictionTagContainer = genreNonFictionTagContainerLight,
  onGenreNonFictionTagContainer = onGenreNonFictionTagContainerLight,
)

val darkCustomColors = CustomColors(
  taskBgColors = listOf(
    // green
    Color(0xFFE1F6DE),
    // blue
    Color(0xFFEDF0FF),
    // yellow
    Color(0xFFFFEFC9),
    // red
    Color(0xFFFFEDE6),
  ),
  taskIconColors = listOf(
    Color(0xFF34A853),
    Color(0xFF1967D2),
    Color(0xFFE37400),
    Color(0xFFD93025),
  ),
  taskIconShapeBgColor = Color.White,
  homeBottomGradient = listOf(Color(0x00F8F9FF), Color(0xffFFEFC9)),
  agentBubbleBgColor = Color(0xFFe9eef6),
  userBubbleBgColor = Color(0xFF32628D),
  linkColor = Color(0xFF32628D),
  successColor = Color(0xff3d860b),
  recordButtonBgColor = Color(0xFFEE675C),
  waveFormBgColor = Color(0xFFaaaaaa),

  onLinkColor = onLinkColorDark,
  linkColorContainer = linkColorContainerDark,
  onLinkColorContainer = onLinkColorContainerDark,
  temporalTag = temporalTagDark,
  onTemporalTag = onTemporalTagDark,
  temporalTagContainer = temporalTagContainerDark,
  onTemporalTagContainer = onTemporalTagContainerDark,
  regionalTag = regionalTagDark,
  onRegionalTag = onRegionalTagDark,
  regionalTagContainer = regionalTagContainerDark,
  onRegionalTagContainer = onRegionalTagContainerDark,
  disciplineTag = disciplineTagDark,
  onDisciplineTag = onDisciplineTagDark,
  disciplineTagContainer = disciplineTagContainerDark,
  onDisciplineTagContainer = onDisciplineTagContainerDark,
  genreFictionTag = genreFictionTagDark,
  onGenreFictionTag = onGenreFictionTagDark,
  genreFictionTagContainer = genreFictionTagContainerDark,
  onGenreFictionTagContainer = onGenreFictionTagContainerDark,
  genreNonFictionTag = genreNonFictionTagDark,
  onGenreNonFictionTag = onGenreNonFictionTagDark,
  genreNonFictionTagContainer = genreNonFictionTagContainerDark,
  onGenreNonFictionTagContainer = onGenreNonFictionTagContainerDark,
)


val MaterialTheme.customColors: CustomColors
  @Composable @ReadOnlyComposable get() = LocalCustomColors.current

/**
 * Controls the color of the phone's status bar icons based on whether the app is using a dark
 * theme.
 */
@Composable
fun StatusBarColorController(useDarkTheme: Boolean) {
  val view = LocalView.current
  val currentWindow = (view.context as? Activity)?.window

  if (currentWindow != null) {
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(currentWindow, false)
      val controller = WindowCompat.getInsetsController(currentWindow, view)
      controller.isAppearanceLightStatusBars = !useDarkTheme // Set to true for light icons
    }
  }
}

@Composable
fun SparkReaderTheme(content: @Composable () -> Unit) {
  val themeOverride = ThemeSettings.themeOverride
  val darkTheme: Boolean =
    (isSystemInDarkTheme() || themeOverride.value == Theme.THEME_DARK) &&
      themeOverride.value != Theme.THEME_LIGHT

  StatusBarColorController(useDarkTheme = darkTheme)

  val colorScheme =
    when {
      darkTheme -> darkScheme
      else -> lightScheme
    }

  val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors

  CompositionLocalProvider(LocalCustomColors provides customColorsPalette) {
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
  }
}
