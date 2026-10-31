package com.hush.todo.core

data class RawItem(
    val id: String,
    val source: String,
    val title: String,
    val body: String,
    val whenMs: Long?,
)

data class ExtractedDeadline(
    val title: String,
    val dueMs: Long?,
    val source: String,
    val sourceRef: String,
    val snippet: String,
)

private val DEADLINE_HINT = Regex("due|deadline|submit|submission|assignment|exam|quiz|last date|जमा|अंतिम", RegexOption.IGNORE_CASE)

fun extractDeadlines(items: List<RawItem>, domainFilter: String): List<ExtractedDeadline> {
    val domain = domainFilter.removePrefix("@").lowercase().trim()
    return items.flatMap { item ->
        if (item.source == "email" && domain.isNotBlank()) {
            val hay = "${item.id} ${item.title} ${item.body}".lowercase()
            if (!hay.contains(domain) && !item.id.lowercase().contains(domain)) return@flatMap emptyList()
        }
        if (item.source == "calendar" && item.whenMs != null) {
            return@flatMap listOf(
                ExtractedDeadline(
                    title = item.title.ifBlank { "Calendar event" },
                    dueMs = item.whenMs,
                    source = item.source,
                    sourceRef = item.id,
                    snippet = item.body.take(180),
                )
            )
        }
        val blob = "${item.title}. ${item.body}".take(800)
        if (item.source == "email" && !DEADLINE_HINT.containsMatchIn(blob)) return@flatMap emptyList()
        val parsed = DateParser.parse(blob)
        if (parsed.title.isBlank()) return@flatMap emptyList()
        listOf(
            ExtractedDeadline(
                title = parsed.title.take(140),
                dueMs = parsed.dueMs ?: item.whenMs,
                source = item.source,
                sourceRef = item.id,
                snippet = blob.take(180),
            )
        )
    }
}
