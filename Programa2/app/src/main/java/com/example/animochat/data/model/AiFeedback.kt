package com.example.animochat.data.model

data class AiFeedback(
    val category: FeedbackCategory,
    val message: String,
    val nextSteps: List<String> = emptyList(),
    val followUpQuestion: String? = null,
    val resources: List<SupportResource> = emptyList(),
)

enum class FeedbackCategory(val jsonName: String) {
    MOTIVATIONAL("motivational"),
    ACADEMIC_STRESS("academic_stress"),
    CRISIS_OR_UNSAFE("crisis_or_unsafe"),
    OUT_OF_SCOPE("out_of_scope");

    companion object {
        fun fromJsonName(value: String): FeedbackCategory? =
            entries.firstOrNull { it.jsonName == value }
    }
}
