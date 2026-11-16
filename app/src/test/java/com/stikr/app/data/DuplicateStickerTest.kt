package com.stikr.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DuplicateStickerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun repo() = PackRepository(File(temp.root, "packs"))

    @Test
    fun duplicateKeepsTheStickerItsSourceAndItsEditSettings() {
        val repository = repo()
        val pack = repository.createPack("Dupes")
        val first = repository.addSticker(
            pack.id,
            byteArrayOf(1, 2, 3),
            fileName = "stk_first.webp",
            source = byteArrayOf(9, 8),
            editData = "{\"outline\":4}",
        )!!

        val updated = repository.duplicateSticker(pack.id, "stk_first.webp")

        assertNotNull(updated)
        assertEquals(2, updated!!.stickerCount)
        val copy = updated.stickers.last()
        assertTrue(copy != "stk_first.webp")
        val dir = File(repository.stickerFile(pack.id, copy)!!.parentFile!!)
        assertEquals(byteArrayOf(1, 2, 3).toList(), File(dir, copy).readBytes().toList())
        assertEquals(byteArrayOf(9, 8).toList(), File(dir, "$copy.source.png").readBytes().toList())
        assertEquals("{\"outline\":4}", File(dir, "$copy.edit.json").readText())
        assertEquals(first.stickerCount + 1, updated.stickerCount)
    }

    @Test
    fun anUnknownStickerCannotBeDuplicated() {
        val repository = repo()
        val pack = repository.createPack("Dupes")
        repository.addSticker(pack.id, byteArrayOf(1))
        assertEquals(null, repository.duplicateSticker(pack.id, "missing.webp"))
        assertEquals(1, repository.getPack(pack.id)!!.stickerCount)
    }

    @Test
    fun aFullPackRefusesDuplicates() {
        val repository = repo()
        val pack = repository.createPack("Full")
        repeat(PackRepository.MAX_WHATSAPP_STICKERS) { index ->
            repository.addSticker(pack.id, byteArrayOf(index.toByte()), fileName = "stk_$index.webp")
        }
        assertEquals(null, repository.duplicateSticker(pack.id, "stk_0.webp"))
        assertEquals(PackRepository.MAX_WHATSAPP_STICKERS, repository.getPack(pack.id)!!.stickerCount)
    }
}
