package com.stikr.app.data

/**
 * Pack splitting and naming for batch runs: many photos become packs of at
 * most [PackRepository.MAX_WHATSAPP_STICKERS] stickers, named after the
 * batch title.
 */
object BatchPlanner {

    private val TRAILING_NUMBER = Regex("^(.*) (\\d+)$")

    /** Sizes of consecutive packs: 100 -> [30, 30, 30, 10]; never above [max]; 0 or less -> empty. */
    fun chunkSizes(total: Int, max: Int = PackRepository.MAX_WHATSAPP_STICKERS): List<Int> {
        if (total <= 0) return emptyList()
        val sizes = mutableListOf<Int>()
        var remaining = total
        while (remaining > 0) {
            val size = minOf(max, remaining)
            sizes += size
            remaining -= size
        }
        return sizes
    }

    /** "Beach trip" single pack; "Beach trip 2" for index 1 of 4 (index is 0-based). count <= 1 -> title. */
    fun packName(title: String, index: Int, count: Int): String =
        if (count <= 1) title else "$title ${index + 1}"

    /** "My stickers" -> "My stickers 2"; "My stickers 2" -> "My stickers 3"; "123" -> "123 2". */
    fun nextPackName(name: String): String {
        val match = TRAILING_NUMBER.matchEntire(name) ?: return "$name 2"
        val (base, number) = match.destructured
        return "$base ${number.toInt() + 1}"
    }
}
