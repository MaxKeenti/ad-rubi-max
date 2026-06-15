package com.example.bachewatch.data.model

/**
 * User-facing report category. Missing values from older documents are
 * interpreted as BACHE for backwards compatibility.
 */
enum class TipoIncidencia(val valor: String, val etiqueta: String) {
    BACHE("bache", "Bache"),
    OTRO("otro", "Otro");

    companion object {
        fun fromValor(valor: String?): TipoIncidencia =
            entries.firstOrNull { it.valor == valor } ?: BACHE
    }
}
