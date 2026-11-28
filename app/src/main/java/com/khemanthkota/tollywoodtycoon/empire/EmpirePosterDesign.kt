package com.khemanthkota.tollywoodtycoon.empire

import kotlin.random.Random

enum class PosterFocus { ACTOR, ACTRESS, ENSEMBLE }

data class PosterDesign(
    val focus: PosterFocus,
    val setting: String,
    val palette: String,
    val composition: Int,
    val costume: Int,
    val lighting: Int,
    val typography: Int
) {
    val signature get() = listOf(focus.name, setting, palette, composition, costume, lighting, typography).joinToString(":")
}

object EmpirePosterDesign {
    private val settings = mapOf(
        Genre.ACTION to listOf("Industrial district", "Mountain chase", "Night city", "Desert road"),
        Genre.COMEDY to listOf("Wedding hall", "Busy market", "Colorful street", "Family home"),
        Genre.ROMANCE to listOf("Coastal sunset", "Flower garden", "Rainy avenue", "Hill station"),
        Genre.THRILLER to listOf("Foggy alley", "Empty station", "Shadowed mansion", "Neon skyline"),
        Genre.DRAMA to listOf("Village courtyard", "City rooftop", "Quiet home", "Open countryside")
    )
    private val palettes = mapOf(
        Genre.ACTION to listOf("Ember", "Steel", "Sand", "Midnight"),
        Genre.COMEDY to listOf("Confetti", "Citrus", "Candy", "Sunshine"),
        Genre.ROMANCE to listOf("Rose", "Lavender", "Peach", "Twilight"),
        Genre.THRILLER to listOf("Noir", "Cold blue", "Crimson", "Forest"),
        Genre.DRAMA to listOf("Earth", "Sepia", "Slate", "Amber")
    )
    // Persist the returned designs with the movie; supply previous signatures to avoid reuse.
    fun campaign(genre: Genre, seed: Int, previous: Set<String> = emptySet()): List<PosterDesign> {
        val rng = Random(seed)
        val used = previous.toMutableSet()
        val scenes = settings.getValue(genre)
        val colors = palettes.getValue(genre)
        val palette = colors[rng.nextInt(colors.size)]
        return PosterFocus.values().map { focus ->
            val candidates = scenes.flatMap { scene ->
                (0..7).flatMap { composition ->
                    (0..5).flatMap { costume ->
                        (0..3).flatMap { lighting ->
                            (0..3).map { type -> PosterDesign(focus, scene, palette, composition, costume, lighting, type) }
                        }
                    }
                }
            }
            val available = candidates.filterNot { it.signature in used }
            require(available.isNotEmpty()) { "Poster design pool exhausted for this palette" }
            available[rng.nextInt(available.size)].also { used.add(it.signature) }
        }
    }
}
