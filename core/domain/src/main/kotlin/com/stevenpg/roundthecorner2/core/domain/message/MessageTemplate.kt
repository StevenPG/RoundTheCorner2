package com.stevenpg.roundthecorner2.core.domain.message

/**
 * Fills `{placeholder}` tokens in a trigger's message.
 *
 * Supported placeholders (see [Placeholder]): `{place}`, `{distance}`,
 * `{radius}`, `{time}`. Unknown tokens are left untouched (so a literal
 * "{hello}" survives), and matching is case-insensitive.
 */
object MessageTemplate {

    enum class Placeholder(val token: String, val description: String) {
        PLACE("place", "Name of the place"),
        DISTANCE("distance", "Your current distance from the place"),
        RADIUS("radius", "The trigger distance you configured"),
        TIME("time", "Time the message is sent"),
    }

    /** Values substituted for each placeholder, already formatted for display. */
    data class Values(
        val place: String,
        val distance: String,
        val radius: String,
        val time: String,
    ) {
        internal fun forPlaceholder(p: Placeholder) = when (p) {
            Placeholder.PLACE -> place
            Placeholder.DISTANCE -> distance
            Placeholder.RADIUS -> radius
            Placeholder.TIME -> time
        }
    }

    private val TOKEN = Regex("""\{(\w+)\}""")
    private val byToken = Placeholder.entries.associateBy { it.token }

    fun render(template: String, values: Values): String =
        TOKEN.replace(template) { match ->
            byToken[match.groupValues[1].lowercase()]?.let(values::forPlaceholder) ?: match.value
        }

    /** Placeholders the template uses, e.g. to preview or highlight them in the editor. */
    fun placeholdersIn(template: String): Set<Placeholder> =
        TOKEN.findAll(template).mapNotNull { byToken[it.groupValues[1].lowercase()] }.toSet()
}
