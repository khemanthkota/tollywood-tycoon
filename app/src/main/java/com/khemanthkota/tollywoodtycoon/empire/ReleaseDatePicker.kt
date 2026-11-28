package com.khemanthkota.tollywoodtycoon.empire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun ReleaseDatePicker(calendar: ReleaseMarketCalendar, earliestDay: Int, region: String, appeal: Int, screens: Int, selectedDay: Int?, onSelect: (Int) -> Unit) {
    var offset by remember(earliestDay) { mutableStateOf(0) }
    val dates = (0..27).map { earliestDay + offset + it }
    val forecasts = dates.associateWith { calendar.forecast(it,region,appeal,screens) }
    val best = dates.sortedByDescending { forecasts.getValue(it).let { f -> f.occupancy * f.availableScreens } }.take(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Plan your release", style = MaterialTheme.typography.titleMedium)
        Text("Earliest release: game day ${earliestDay+1}")
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(enabled = offset > 0, onClick = { offset -= 28 }) { Text("Previous") }
            TextButton(enabled = offset < 336, onClick = { offset += 28 }) { Text("Next") }
        }
        Text("Days ${dates.first()+1}–${dates.last()+1}")
        dates.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    val f = forecasts.getValue(day)
                    Surface(modifier = Modifier.weight(1f).height(54.dp).testTag(if (day == earliestDay) "earliest-release" else "release-day-$day").clickable { onSelect(day) },
                        shape = MaterialTheme.shapes.small,
                        color = if (day == selectedDay) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("${day%365+1}", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            LinearProgressIndicator(progress = (f.demandMultiplier/2).toFloat().coerceIn(0f,1f), modifier = Modifier.fillMaxWidth().height(4.dp))
                        }
                    }
                }
            }
        }
        Text("Demand bars • full bar = 200% of normal")
        Text("Recommended release dates", style = MaterialTheme.typography.titleSmall)
        best.forEach { day ->
            val f = forecasts.getValue(day)
            OutlinedButton(onClick = { onSelect(day) }, modifier = Modifier.fillMaxWidth()) {
                Text("Day ${day+1} • ${(f.occupancy*100).roundToInt()}% occupancy • ${f.availableScreens} screens")
            }
        }
        selectedDay?.let { day ->
            val f = calendar.forecast(day,region,appeal,screens)
            Text("Game day ${day+1} • Expected occupancy ${(f.occupancy*100).roundToInt()}%")
            Text("Available screens: ${f.availableScreens}")
            Text("Demand: ${(f.demandMultiplier*100).roundToInt()}% of normal")
            f.holidays.forEach { Text(it) }
            f.rivals.forEach { Text("Competitor: ${it.title} • Appeal ${it.appeal}") }
        }
    }
}
