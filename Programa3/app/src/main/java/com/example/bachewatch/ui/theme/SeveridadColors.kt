package com.example.bachewatch.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.bachewatch.data.model.Severidad

val SeveridadLeve = Color(0xFF2E7D32)
val SeveridadModerado = Color(0xFFEF6C00)
val SeveridadSevero = Color(0xFFC62828)
val SeveridadSinDato = Color(0xFF757575)

/** Single color mapping for chips, markers (task 07) and badges. */
fun colorDeSeveridad(severidad: Severidad?): Color = when (severidad) {
    Severidad.LEVE -> SeveridadLeve
    Severidad.MODERADO -> SeveridadModerado
    Severidad.SEVERO -> SeveridadSevero
    null -> SeveridadSinDato
}
