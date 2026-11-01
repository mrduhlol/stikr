package com.stikr.app.ui

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.data.PackRepository
import java.io.File
import java.time.Duration
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalBackupTest {
    @get:Rule val folder = TemporaryFolder()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val root get() = File(app.filesDir, "packs")

    @Before fun freshLibrary() { root.deleteRecursively() }

    @Test fun savedFileRestoresAfterPrivateLibraryIsRemoved() {
        val repo = PackRepository(root)
        val pack = repo.createPack("Keep these")
        repo.addSticker(pack.id, byteArrayOf(1, 2, 3), "face.webp", source = byteArrayOf(4, 5), editData = "{}")
        repo.setPackThumbnail(pack.id, byteArrayOf(6))
        val file = folder.newFile("stikr.zip")
        val first = MainViewModel(app)
        first.exportLocalBackup(Uri.fromFile(file))
        await { !first.localBackupBusy.value }
        assertTrue(file.length() > 0)
        root.deleteRecursively()
        val reinstalled = MainViewModel(app)
        reinstalled.restoreLocalBackup(Uri.fromFile(file))
        await { !reinstalled.localBackupBusy.value }
        assertEquals(pack.id, reinstalled.packs.value.single().id)
        assertEquals("Keep these", reinstalled.packs.value.single().name)
        assertArrayEquals(byteArrayOf(4, 5), File(root, "${pack.id}/face.webp.source.png").readBytes())
        assertArrayEquals(byteArrayOf(6), File(root, "${pack.id}/thumbnail.png").readBytes())
    }

    @Test fun corruptFileDoesNotChangeExistingLibrary() {
        val repo = PackRepository(root)
        val pack = repo.createPack("Stay safe")
        val file = folder.newFile("broken.zip").apply { writeText("not a backup") }
        val vm = MainViewModel(app)
        vm.restoreLocalBackup(Uri.fromFile(file))
        await { !vm.localBackupBusy.value }
        assertEquals(pack.id, repo.listPacks().single().id)
        assertTrue(vm.message.value!!.startsWith("Could not restore"))
    }

    @Test fun emptyBackupCanBeRestoredWithoutDeletingExistingPacks() {
        val empty = PackRepository(folder.newFolder("empty"))
        val archive = folder.newFile("empty.zip")
        empty.exportBackup(archive)
        val target = PackRepository(root)
        val keep = target.createPack("Keep")
        assertEquals(0, target.importBackup(archive))
        assertEquals(keep.id, target.listPacks().single().id)
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(40))
            Thread.sleep(10)
        }
        assertTrue("backup operation did not finish", condition())
    }
}
