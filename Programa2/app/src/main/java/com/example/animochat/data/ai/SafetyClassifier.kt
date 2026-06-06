package com.example.animochat.data.ai

import java.text.Normalizer

object SafetyClassifier {
    fun classify(message: String): SafetyCategory {
        val normalized = message.normalizedForSafety()

        if (normalized.isBlank()) return SafetyCategory.OUT_OF_SCOPE
        if (crisisPatterns.any { it.containsMatchIn(normalized) }) {
            return SafetyCategory.CRISIS_OR_SELF_HARM
        }
        if (violencePatterns.any { it.containsMatchIn(normalized) }) {
            return SafetyCategory.VIOLENCE_OR_KILLING
        }
        if (academicPatterns.any { it.containsMatchIn(normalized) }) {
            return SafetyCategory.NORMAL_MOTIVATIONAL
        }
        if (outOfScopePatterns.any { it.containsMatchIn(normalized) }) {
            return SafetyCategory.OUT_OF_SCOPE
        }

        return SafetyCategory.NORMAL_MOTIVATIONAL
    }

    private fun String.normalizedForSafety(): String {
        val withoutAccents = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

        return withoutAccents
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val crisisPatterns = listOf(
        Regex("\\b(me quiero|quiero|voy a|planeo|pienso)\\s+(morir|suicidar|suicidarme|hacerme dano|lastimarme)\\b"),
        Regex("\\b(no quiero vivir|ya no quiero vivir|terminar con mi vida|quitarme la vida)\\b"),
        Regex("\\b(self harm|kill myself|hurt myself|suicide|suicidal)\\b"),
    )

    private val violencePatterns = listOf(
        Regex("\\b(como|ayudame a|quiero|voy a|planeo|dime como)\\s+(matar|asesinar|herir|golpear|lastimar)\\b"),
        Regex("\\b(matar a alguien|asesinar a alguien|hacer dano a alguien|lastimar a alguien)\\b"),
        Regex("\\b(kill someone|murder someone|hurt someone|harm someone)\\b"),
    )

    private val academicPatterns = listOf(
        Regex("\\b(examen|tarea|proyecto|clase|materia|matematicas|calculo|programacion|profesor|profesora)\\b"),
        Regex("\\b(escuela|universidad|calificacion|reprobar|estudiar|estudio|entrega|parcial|semestre)\\b"),
        Regex("\\b(miedo|estres|ansiedad|triste|preocupado|preocupada|nervioso|nerviosa|fracasar)\\b"),
    )

    private val outOfScopePatterns = listOf(
        Regex("\\b(chiste|meme|receta|clima|deporte|pelicula|videojuego|cancion)\\b"),
        Regex("\\b(joke|recipe|weather|sports|movie|videogame|song)\\b"),
    )
}

enum class SafetyCategory {
    NORMAL_MOTIVATIONAL,
    CRISIS_OR_SELF_HARM,
    VIOLENCE_OR_KILLING,
    OUT_OF_SCOPE,
}
