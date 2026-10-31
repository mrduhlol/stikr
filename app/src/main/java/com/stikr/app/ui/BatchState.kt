package com.stikr.app.ui

import com.stikr.app.data.StickerPack

/** Which half of the batch flow the screen is showing. */
enum class BatchPhase { PROCESSING, DONE }

/**
 * One batch run: what was asked for, how far it has got, and what it made.
 *
 * Counters: [processed] = photos attempted, [saved] = stickers written
 * (includes fallbacks), [fellBack] = full-photo fallbacks, [failed] =
 * unreadable photos. [created] is filled when [phase] flips to [DONE].
 */
data class BatchState(
    val title: String,
    val author: String,
    val total: Int,
    val expectedPacks: Int,
    val removeBackground: Boolean = false,
    val processed: Int = 0,
    val saved: Int = 0,
    val fellBack: Int = 0,
    val failed: Int = 0,
    val created: List<StickerPack> = emptyList(),
    val phase: BatchPhase = BatchPhase.PROCESSING,
)
