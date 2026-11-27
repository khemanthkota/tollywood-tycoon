package com.khemanthkota.tollywoodtycoon.empire

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun ReleaseDatePicker(calendar: ReleaseMarketCalendar, earliestDay: Int, region: String, appeal: Int, screens: Int, selectedDay: Int?, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Plan your release")
        val dates = (0..27).map { earliestDay + it }
        dates.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { day ->
                    val forecast = calendar.forecast(day,region,appeal,screens)
                    OutlinedButton(onClick = { onSelect(day) }, modifier = androidx.compose.ui.Modifier.weight(1f)) {
                        Text(if(day == selectedDay) "✓${day%365+1}" else "${day%365+1}")
                    }
                }
            }
        }
        selectedDay?.let { day ->
            val forecast = calendar.forecast(day,region,appeal,screens)
            Text("Game day ${day+1} • Expected occupancy ${(forecast.occupancy*100).roundToInt()}%")
            Text("Available screens: ${forecast.availableScreens}")
            Text("Demand: ${(forecast.demandMultiplier*100).roundToInt()}% of normal")
            forecast.holidays.forEach { Text(it) }
            forecast.rivals.forEach { Text("Competitor: ${it.title} • Appeal ${it.appeal}") }
        }
    }
}
