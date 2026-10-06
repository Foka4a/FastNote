package com.quicknotes.app.domain.link

import com.quicknotes.app.domain.model.NoteRef

/**
 * Extracts and rewrites `[[Título]]` links. Pure Kotlin so the repository, the Room
 * migration and the test fakes share exactly the same rules.
 */
object LinkParser {
    data class LinkInfo(val title: String, val index: Int)

    private val LINK = Regex("""\[\[(.*?)]]""")

    /** Links in order of appearance; blank `[[ ]]` are skipped and do not consume an index. */
    fun extractLinks(content: String): List<LinkInfo> =
        LINK.findAll(content)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { i, title -> LinkInfo(title, i) }
            .toList()

    /** Match key: trimmed + lowercase. Done in Kotlin because SQLite LIKE/NOCASE only fold ASCII (Ã, Ç...). */
    fun normalize(title: String): String = title.trim().lowercase()

    /** normalized title -> note id. Blank titles never resolve; on duplicate titles the oldest note (lowest id) wins. */
    fun titleIndex(notes: List<NoteRef>): Map<String, Long> = buildMap {
        for (note in notes.sortedBy { it.id }) {
            val key = normalize(note.title)
            if (key.isNotEmpty()) putIfAbsent(key, note.id)
        }
    }

    /** Rewrites every `[[oldTitle]]` (any case/padding) to `[[newTitle]]`; the replacement is literal (no `$` groups). */
    fun renameLinks(content: String, oldTitle: String, newTitle: String): String {
        val old = normalize(oldTitle)
        return LINK.replace(content) { match ->
            if (normalize(match.groupValues[1]) == old) "[[${newTitle.trim()}]]" else match.value
        }
    }
}
