package com.stikr.app.data

/** One pack of stickers, as stored on disk. */
data class StickerPack(
    val id: String,
    val name: String,
    val author: String,
    val createdAt: Long,
    val stickers: List<String>,
    val trayImage: String? = null,
    val isSingles: Boolean = false,
    val serialNumber: Int = 0,
    val displayId: String = "",
    val thumbnailImage: String? = null,
    val starterStickers: List<String> = emptyList(),
    val contentVersion: Long = 1L,
) {
    val stickerCount: Int get() = stickers.size
    val userStickerCount: Int get() = stickers.count { it !in starterStickers }
}
