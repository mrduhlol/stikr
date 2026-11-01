package com.stikr.app.data.backup

import com.stikr.app.data.PackRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class BackupArchiveTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun exportCapturesPackAssetsAndNoTemporaryFiles() {
        val root = folder.newFolder("library")
        val repository = PackRepository(root)
        val pack = repository.createPack("Saved")
        repository.addSticker(pack.id, byteArrayOf(1, 2, 3), "face.webp", source = byteArrayOf(4, 5), editData = "{\"rotation\":2}")
        repository.setPackThumbnail(pack.id, byteArrayOf(6))
        repository.setTrayImage(pack.id, byteArrayOf(7), "tray.png")
        val archive = File(folder.root, "backup.zip")

        repository.exportBackup(archive)

        val unpacked = BackupArchive.validate(archive, folder.root).use { valid ->
            val packFiles = valid.packs.single().files
            assertEquals(setOf("pack.json", "face.webp", "face.webp.source.png", "face.webp.edit.json", "thumbnail.png", "tray.png"), packFiles.keys)
            assertEquals("{\"rotation\":2}", packFiles.getValue("face.webp.edit.json").readText())
            true
        }
        assertTrue(unpacked)
        assertEquals(setOf("library", "backup.zip"), folder.root.list()!!.toSet())
    }

    @Test fun importKeepsNewerLocalPackAndInstallsNewArchivePack() {
        val sourceRepo = PackRepository(folder.newFolder("source"))
        val incoming = sourceRepo.createPack("Cloud")
        sourceRepo.addSticker(incoming.id, byteArrayOf(1), "cloud.webp")
        val archive = File(folder.root, "cloud.zip")
        sourceRepo.exportBackup(archive)

        val localRepo = PackRepository(folder.newFolder("local"))
        val localRoot = File(folder.root, "local")
        val original = File(localRoot, incoming.id)
        original.mkdirs()
        File(sourceRepoDir(sourceRepo, incoming.id), "pack.json").copyTo(File(original, "pack.json"))
        File(sourceRepoDir(sourceRepo, incoming.id), "cloud.webp").copyTo(File(original, "cloud.webp"))
        val localJson = File(original, "pack.json")
        localJson.writeText(localJson.readText().replace("\"name\":\"Cloud\"", "\"name\":\"Newer local\"").replace("\"contentVersion\":2", "\"contentVersion\":99"))
        val second = localRepo.createPack("Keep me")

        assertEquals(0, localRepo.importBackup(archive))
        assertEquals("Newer local", localRepo.getPack(incoming.id)!!.name)
        assertEquals("Keep me", localRepo.getPack(second.id)!!.name)
    }

    @Test fun invalidArchiveDoesNotMutateExistingLibrary() {
        val root = folder.newFolder("library")
        val repository = PackRepository(root)
        val local = repository.createPack("Keep")
        val before = File(root, local.id).list()!!.toSet()
        val bad = File(folder.root, "bad.zip")
        ZipOutputStream(bad.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../outside"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }

        val failed = runCatching { repository.importBackup(bad) }.isFailure

        assertTrue(failed)
        assertEquals(before, File(root, local.id).list()!!.toSet())
        assertTrue(repository.getPack(local.id) != null)
        assertFalse(File(folder.root, "outside").exists())
    }

    @Test fun restoringImportedStickersPreservesIdentityAndNewerChanges() {
        val source = PackRepository(folder.newFolder("roundtrip-source"))
        val created = source.createPack("Original")
        source.addSticker(created.id, byteArrayOf(1, 2), "import.webp")
        val before = source.getPack(created.id)!!
        val archive = File(folder.root, "roundtrip.zip")
        source.exportBackup(archive)
        val target = PackRepository(folder.newFolder("roundtrip-target"))
        assertEquals(1, target.importBackup(archive))
        assertEquals(before.displayId, target.getPack(created.id)!!.displayId)
        assertEquals(before.stickers, target.getPack(created.id)!!.stickers)
        assertEquals(0, target.importBackup(archive))
        source.renamePack(created.id, "Updated")
        source.exportBackup(archive)
        assertEquals(1, target.importBackup(archive))
        assertEquals("Updated", target.getPack(created.id)!!.name)
        assertEquals(before.displayId, target.getPack(created.id)!!.displayId)
    }

    private fun sourceRepoDir(repository: PackRepository, id: String): File = folder.root.resolve("source").resolve(id)
}
