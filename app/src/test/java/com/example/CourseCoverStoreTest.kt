package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.settings.CourseCoverStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CourseCoverStoreTest {
    @Test fun photoPersistsThroughReopeningAndRenamingAndResetRemovesPrivateCopy(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CourseCoverStore(context); store.clear()
        val source = File(context.cacheDir, "cover-fixture.png")
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        val prepared = store.preparePhoto("旅行", Uri.fromFile(source))
        store.set("旅行", prepared)
        assertEquals(prepared, CourseCoverStore(context).value("旅行"))
        assertTrue(store.file(prepared)!!.exists())
        store.rename("旅行", "生活")
        assertEquals(prepared, store.value("生活")); assertEquals("preset:sky", store.value("旅行"))
        assertNull(store.file("file:../../outside.jpg"))
        assertTrue(runCatching { store.set("生活", "https://tracker.invalid/a.png") }.isFailure)
        store.set("生活", "preset:ice")
        assertNull(store.file(prepared)); store.clear(); source.delete(); Unit
    }
}
