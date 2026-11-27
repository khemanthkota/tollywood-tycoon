package com.khemanthkota.tollywoodtycoon.empire

// Scenario dates and economics are balancing assumptions, not technology forecasts.
object EmpireTechnologyRoadmap {
    data class Family(val id: String, val achievement: String, val names: List<String>)
    data class Generation(val year: Int, val improvement: String, val costFactor: Int,
        val crewTraining: Int, val energyIndex: Int)
    val families = listOf(
        Family("camera", "cinematography", listOf("Digital cinema camera", "Large-format camera", "High-speed camera", "Low-light camera", "Anamorphic lens", "Macro lens", "Remote camera head", "Stereo camera", "Underwater camera", "Aerial camera")),
        Family("lighting", "technical", listOf("LED panel", "Spotlight", "Soft light", "Lighting console", "Portable power unit", "Color calibration system", "Projection light", "Weatherproof light", "Moving light", "Light measurement system")),
        Family("sound", "technical", listOf("Boom microphone", "Wireless microphone", "Field recorder", "Sound mixer", "Acoustic booth", "Spatial microphone array", "Noise isolation system", "Foley stage", "Monitoring system", "Audio restoration workstation")),
        Family("studio", "production", listOf("Soundstage", "Water tank stage", "Backlot", "Modular stage", "Acoustic stage", "Vehicle stage", "Weather controlled stage", "Miniature stage", "Green screen stage", "Workshop")),
        Family("sets", "production", listOf("Historical street", "Palace set", "Urban street", "Village set", "Hospital set", "Airport set", "Railway set", "Industrial set", "Spacecraft interior", "Residential set")),
        Family("vfx", "technical", listOf("Rendering cluster", "Compositing workstation", "Simulation cluster", "Digital asset scanner", "Texture capture system", "Facial capture system", "Crowd simulation system", "Color grading suite", "Digital matte system", "Fluid simulation system")),
        Family("virtual", "technical", listOf("LED volume", "Camera tracking system", "Virtual scouting suite", "Real-time rendering stage", "Projection stage", "Virtual lighting system", "Digital twin workstation", "Previsualization suite", "Virtual art department", "Calibration laboratory")),
        Family("motion", "technical", listOf("Optical motion capture", "Inertial motion capture", "Hand tracking", "Facial tracking", "Performance capture stage", "Animal motion capture", "Motion analysis suite", "Body scanning system", "Markerless capture", "Volumetric capture")),
        Family("stunt", "production", listOf("Wire rig", "Crash rig", "Vehicle platform", "Hydraulic motion base", "Safety mat system", "Mechanical creature rig", "Robotic stunt rig", "Remote vehicle rig", "Fall arrest system", "Fire effects stage")),
        Family("costume", "production", listOf("Costume workshop", "Textile printer", "Prosthetics laboratory", "Makeup studio", "Armor workshop", "Pattern cutting system", "Fabric archive", "Wig workshop", "Costume scanning system", "Animatronic workshop")),
        Family("transport", "production", listOf("Camera vehicle", "Crew bus", "Equipment truck", "Mobile dressing room", "Mobile generator", "Mobile editing unit", "Production trailer", "Location support vehicle", "Mobile catering unit", "Remote logistics unit")),
        Family("post", "technical", listOf("Editing suite", "Media storage array", "Archival system", "Film scanner", "Mastering suite", "Subtitle workstation", "Quality control suite", "Restoration suite", "Transcoding cluster", "Secure delivery system")),
        Family("music", "creative", listOf("Recording studio", "Orchestral stage", "Mixing room", "Instrument collection", "Synthesizer suite", "Mastering room", "Scoring workstation", "Acoustic rehearsal room", "Music archive", "Remote recording suite")),
        Family("special", "technical", listOf("Underwater facility", "Wind tunnel stage", "Rain stage", "Snow stage", "Desert backlot", "Mountain facility", "High-altitude support base", "Controlled motion stage", "Environmental chamber", "Remote location base")),
        Family("distribution", "distribution", listOf("Single screen cinema", "Multiplex", "Large-format theatre", "Outdoor cinema", "Immersive audio theatre", "Digital distribution hub", "Regional booking office", "Premium screening room", "Mobile cinema", "Content delivery infrastructure"))
    )
    val generations = listOf(
        Generation(2026,"Conventional",100,1,100),
        Generation(2030,"Digitally monitored",120,2,95),
        Generation(2037,"Computer assisted",145,3,90),
        Generation(2045,"Precision automated",175,4,85),
        Generation(2055,"Adaptive control",210,5,80),
        Generation(2065,"Integrated simulation",250,6,78),
        Generation(2078,"Resource optimized",285,7,70),
        Generation(2090,"Advanced modular",320,8,65),
        Generation(2105,"Highly autonomous",370,9,60),
        Generation(2120,"Next-generation integrated",420,10,55)
    )
    data class Variant(val technology: AssetTechnology, val generation: Generation,
        val category: String, val baseType: String)
    val variants: List<Variant> = families.flatMapIndexed { f, family ->
        family.names.flatMapIndexed { n, name ->
            generations.mapIndexed { g, generation ->
                val root = "roadmap_${family.id}_$n"
                val basePrice = 25 + f * 12 + n * 8
                Variant(AssetTechnology(
                    id = "${root}_$g", name = "$name – ${generation.improvement}",
                    earliestYear = generation.year, price = basePrice * generation.costFactor / 100,
                    supply = (20 - f / 2 - g).coerceAtLeast(2),
                    requiredFilms = 3 + g * 3,
                    requiredAchievements = if (g == 0) emptySet() else setOf(family.achievement),
                    prerequisites = if (g == 0) emptySet() else setOf("${root}_${g-1}"),
                    maintenancePercent = 3 + g / 3,
                    annualDepreciationPercent = 5 + g / 2
                ), generation, family.id, name)
            }
        }
    }
    // Research points are earned externally. Discovery requires predecessor research,
    // earliest plausible date, and sufficient investment; money alone cannot unlock ownership.
    fun discover(industry: IndustryTechnology, id: String, researchPoints: Int): IndustryTechnology {
        val variant = variants.single { it.technology.id == id }
        val t = variant.technology
        require(id !in industry.discovered && industry.year >= t.earliestYear)
        require(industry.discovered.containsAll(t.prerequisites))
        require(researchPoints >= variant.generation.crewTraining * 100)
        return industry.copy(discovered = industry.discovered + id)
    }
}
