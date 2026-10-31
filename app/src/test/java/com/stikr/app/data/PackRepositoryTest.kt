package com.stikr.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PackRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun repo() = PackRepository(File(temp.root, "packs"))

    @Test
    fun createAddRenameDelete() {
        val repository = repo()
        val pack = repository.createPack("Cats")
        assertEquals("Cats", pack.name)
        assertEquals(0, pack.stickerCount)

        val withSticker = repository.addSticker(pack.id, byteArrayOf(1, 2, 3))
        assertNotNull(withSticker)
        assertEquals(1, withSticker!!.stickerCount)

        val renamed = repository.renamePack(pack.id, "Street cats")
        assertEquals("Street cats", renamed?.name)

        val afterDelete = repository.deleteSticker(pack.id, withSticker.stickers.first())
        assertEquals(0, afterDelete?.stickerCount)

        assertTrue(repository.deletePack(pack.id))
        assertTrue(repository.listPacks().isEmpty())
    }

    @Test
    fun packsSurviveReopen() {
        val pack = repo().createPack("Keepers")
        repo().addSticker(pack.id, byteArrayOf(9, 9, 9), fileName = "stk_a.webp")

        val reopened = repo().listPacks()
        assertEquals(1, reopened.size)
        assertEquals("Keepers", reopened.first().name)
        assertEquals(listOf("stk_a.webp"), reopened.first().stickers)
    }

    @Test
    fun stickerFileOnlyServesListedFiles() {
        val repository = repo()
        val pack = repository.createPack("Guarded")
        repository.addSticker(pack.id, byteArrayOf(5), fileName = "stk_ok.webp")
        File(repository.stickerFile(pack.id, "stk_ok.webp")!!.parentFile, "stk_unlisted.webp")
            .writeBytes(byteArrayOf(6))

        assertNotNull(repository.stickerFile(pack.id, "stk_ok.webp"))
        assertNull(repository.stickerFile(pack.id, "stk_unlisted.webp"))
        assertNull(repository.stickerFile(pack.id, "../pack.json"))
    }

    @Test
    fun fullPackRejectsNewStickerButAllowsReplacementAndInvalidatesTray() {
        val repository = repo()
        val pack = repository.createPack("Full")
        repeat(30) { repository.addSticker(pack.id, byteArrayOf(1), "stk_$it.webp") }
        repository.setTrayImage(pack.id, byteArrayOf(7))
        assertNull(repository.addSticker(pack.id, byteArrayOf(2)))
        val replaced = repository.replaceSticker(pack.id, "stk_0.webp", byteArrayOf(3), byteArrayOf(9))!!
        assertEquals(30, replaced.stickerCount)
        assertEquals(30, replaced.stickers.distinct().size)
        assertNull(replaced.trayImage)
        assertEquals(3, repository.stickerFile(pack.id, "stk_0.webp")!!.readBytes().single().toInt())
        assertEquals(9, repository.sourceFile(pack.id, "stk_0.webp")!!.readBytes().single().toInt())
        assertNull(repository.stickerFile(pack.id, "stk_0.webp.source.png"))
        repository.deleteSticker(pack.id, "stk_0.webp")
        assertNull(repository.sourceFile(pack.id, "stk_0.webp"))
    }

    @Test
    fun replacementMetadataFailureRollsBackStickerOriginalAndEditorSettings() {
        val repository = repo()
        val pack = repository.createPack("Rollback")
        repository.addSticker(pack.id, byteArrayOf(1), "stk_a.webp", byteArrayOf(2), "old settings")
        repository.setTrayImage(pack.id, byteArrayOf(7))
        val file = repository.stickerFile(pack.id, "stk_a.webp")!!
        val dir = file.parentFile!!
        val originalMetadata = File(dir, PackRepository.PACK_FILE).readText()
        // Final asset moves succeed; opening the metadata staging file then fails.
        File(dir, "${PackRepository.PACK_FILE}.tmp").mkdir()
        var failed = false
        try {
            repository.replaceSticker(pack.id, "stk_a.webp", byteArrayOf(3), byteArrayOf(4), "new settings")
        } catch (_: Exception) { failed = true }
        assertTrue("Replacement must report failed metadata writes", failed)
        assertEquals(1, file.readBytes().single().toInt())
        assertEquals(2, repository.sourceFile(pack.id, "stk_a.webp")!!.readBytes().single().toInt())
        assertEquals("old settings", repository.editData(pack.id, "stk_a.webp"))
        assertEquals(originalMetadata, File(dir, PackRepository.PACK_FILE).readText())
        assertEquals(PackRepository.TRAY_FILE, repository.getPack(pack.id)!!.trayImage)
        assertTrue(dir.listFiles()!!.none { it.name.startsWith("replace_") })
    }

    @Test
    fun unreadablePackIsSkipped() {
        val root = File(temp.root, "packs")
        val broken = File(root, "pack_broken").apply { mkdirs() }
        File(broken, PackRepository.PACK_FILE).writeText("{ not json !!")

        assertTrue(repo().listPacks().isEmpty())
    }

    @Test
    fun unchangedMetadataIsReusedAndOtherInstancesInvalidateIt() {
        val writer = repo()
        val pack = writer.createPack("Before")
        writer.addSticker(pack.id, byteArrayOf(5), "stk_a.webp")
        val reader = repo()
        val first = reader.getPack(pack.id)
        assertSame(first, reader.getPack(pack.id))
        // Same-length rename without a sleep must be visible to the provider's repository.
        writer.renamePack(pack.id, "After!")
        assertEquals("After!", reader.getPack(pack.id)?.name)
        writer.deleteSticker(pack.id, "stk_a.webp")
        assertEquals(emptyList<String>(), reader.getPack(pack.id)?.stickers)
        writer.deletePack(pack.id)
        assertNull(reader.getPack(pack.id))
        assertTrue(reader.listPacks().isEmpty())
    }

    @Test
    fun cachedMetadataStillFiltersMissingFiles() {
        val repository = repo()
        val pack = repository.createPack("Files")
        repository.addSticker(pack.id, byteArrayOf(5), "stk_a.webp")
        val file = repository.stickerFile(pack.id, "stk_a.webp")!!
        file.delete()
        assertEquals(emptyList<String>(), repository.getPack(pack.id)?.stickers)
        file.writeBytes(byteArrayOf(6))
        assertEquals(listOf("stk_a.webp"), repository.getPack(pack.id)?.stickers)
    }

    @Test
    fun deletingFirstStickerInvalidatesGeneratedTray() {
        val repository = repo()
        val pack = repository.createPack("Tray")
        repository.addSticker(pack.id, byteArrayOf(1), "stk_a.webp")
        repository.addSticker(pack.id, byteArrayOf(2), "stk_b.webp")
        repository.setTrayImage(pack.id, byteArrayOf(3))
        assertNotNull(repository.deleteSticker(pack.id, "stk_b.webp")?.trayImage)
        assertNull(repository.deleteSticker(pack.id, "stk_a.webp")?.trayImage)
    }

    @Test
    fun newestPackComesFirst() {
        val repository = repo()
        val older = repository.createPack("Older")
        Thread.sleep(5)
        repository.createPack("Newer")

        val listed = repository.listPacks()
        assertEquals(2, listed.size)
        assertEquals("Newer", listed.first().name)
        assertEquals("Older", listed.last().name)
        assertNotNull(repository.getPack(older.id))
    }

    @Test
    fun packsHaveStableChronologicalDisplayIdentityAndOldMetadataStillLoads() {
        val repository = repo()
        val first = repository.createPack("First")
        val second = repository.createPack("Second")
        val reopened = repo()
        assertEquals(first.serialNumber, reopened.getPack(first.id)?.serialNumber)
        assertEquals(first.displayId, reopened.getPack(first.id)?.displayId)
        assertTrue(second.serialNumber > first.serialNumber)
        assertTrue(first.displayId.isNotBlank())
        val old = File(temp.root, "packs/legacy").apply { mkdirs() }
        File(old, PackRepository.PACK_FILE).writeText("""{"id":"legacy","name":"Legacy","author":"a","createdAt":1,"stickers":[],"trayImage":""}""")
        val migrated = reopened.getPack("legacy")!!
        assertFalse(migrated.isSingles)
        assertEquals(1L, migrated.contentVersion)
        assertTrue(migrated.displayId.isNotBlank())
    }

    @Test
    fun singlesHideAndRemoveStartersAtThreeUserStickersAndNeverRestoreThem() {
        val repository = repo()
        val pack = repository.getOrCreateSingles(listOf(byteArrayOf(1), byteArrayOf(2)))
        assertEquals("Stikr", pack.name)
        assertEquals(2, pack.stickerCount)
        assertEquals(0, pack.userStickerCount)
        repository.addSticker(pack.id, byteArrayOf(3), "user1.webp")
        repository.addSticker(pack.id, byteArrayOf(4), "user2.webp")
        repository.setTrayImage(pack.id, byteArrayOf(7), "tray.png")
        val third = repository.addSticker(pack.id, byteArrayOf(5), "user3.webp")!!
        assertNull("Removing starters invalidates their tray icon", third.trayImage)
        assertEquals(3, third.userStickerCount)
        assertTrue(third.starterStickers.isEmpty())
        assertEquals(listOf("user1.webp", "user2.webp", "user3.webp"), third.stickers)
        repository.deleteSticker(pack.id, "user3.webp")
        assertEquals(2, repository.getOrCreateSingles(listOf(byteArrayOf(9))).userStickerCount)
        assertEquals(2, repository.getPack(pack.id)!!.stickerCount)
    }

    @Test
    fun singlesRollOverAtThirtyAndKeepEarlierPackIdentity() {
        val repository = repo()
        val first = repository.getOrCreateSingles(emptyList())
        repeat(30) { repository.addSticker(first.id, byteArrayOf(it.toByte()), "user_$it.webp") }
        val second = repository.getOrCreateSingles(listOf(byteArrayOf(1), byteArrayOf(2)))
        assertEquals(2, second.starterStickers.size)
        assertEquals(0, second.userStickerCount)
        assertTrue(second.id != first.id)
        assertEquals("Stikr 2", second.name)
        assertEquals(30, repository.getPack(first.id)!!.userStickerCount)
        assertEquals(first.displayId, repository.getPack(first.id)!!.displayId)
    }

    @Test
    fun editsAndThumbnailsBumpPersistentVersionAndThumbnailIsPrivate() {
        val repository = repo()
        val pack = repository.createPack("Versioned")
        val added = repository.addSticker(pack.id, byteArrayOf(1), "one.webp")!!
        val edited = repository.replaceSticker(pack.id, "one.webp", byteArrayOf(2))!!
        assertTrue(edited.contentVersion > added.contentVersion)
        val thumb = repository.setPackThumbnail(pack.id, byteArrayOf(3))!!
        assertTrue(thumb.contentVersion > edited.contentVersion)
        assertNotNull(File(temp.root, "packs/${pack.id}/${PackRepository.THUMBNAIL_FILE}").takeIf { it.isFile })
        assertNull(repository.stickerFile(pack.id, PackRepository.THUMBNAIL_FILE))
    }
}
