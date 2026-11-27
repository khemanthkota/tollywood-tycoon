package com.khemanthkota.tollywoodtycoon.empire

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmpirePortfolioDashboard(portfolio:FilmPortfolio,day:Int,onAdvance:(String)->Unit,onRelease:(String,Response?)->Unit,onLaunch:()->Unit) {
    val limit=if(portfolio.career.completed>=15) 3 else if(portfolio.career.completed>=8) 2 else 1
    val stages=listOf("Pre-production","Principal photography","Post-production","Premiere")
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Production slate",style=MaterialTheme.typography.titleLarge)
        Text("${portfolio.active.size}/$limit active • Day $day")
        Text("Reserved ₹${portfolio.reserved} lakh • Available ₹${portfolio.availableCash} lakh")
        Button(onClick=onLaunch,enabled=portfolio.active.size<limit && portfolio.availableCash>=145) { Text("New production") }
        portfolio.active.forEach { film ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(film.production.plan.title,style=MaterialTheme.typography.titleMedium)
                    Text(stages[film.stage.coerceIn(0,3)])
                    LinearProgressIndicator(progress=(film.stage+1)/4f,modifier=Modifier.fillMaxWidth())
                    Text("Reserved ₹${film.booking.reservedBudget} lakh • Release day ${film.booking.end}")
                    if(film.stage<3) Button(onClick={onAdvance(film.id)}) { Text("Advance production") }
                    else if(film.production.event) Response.values().forEach { response ->
                        Button(enabled=day>=film.booking.end,onClick={onRelease(film.id,response)}) { Text("Release • ${response.name}") }
                    } else Button(enabled=day>=film.booking.end,onClick={onRelease(film.id,null)}) { Text("Release movie") }
                }
            }
        }
    }
}
