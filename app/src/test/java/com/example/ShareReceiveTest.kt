package com.example

import com.example.data.importer.ShareLinks
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
class ShareReceiveTest {
    @Test fun browserHandoffResolvesToVocabActivity() {
        val intent=android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("vocab://share/"+"a".repeat(48))).addCategory(android.content.Intent.CATEGORY_BROWSABLE)
        val result=RuntimeEnvironment.getApplication().packageManager.resolveActivity(intent,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        assertEquals("com.example.MainActivity",result?.activityInfo?.name)
    }
    @Test fun acceptsOnlyVocabCapabilityLinks() {
        val id="a".repeat(48)
        val canonical="https://vocab-adam.pages.dev/share#$id"
        assertEquals(canonical,ShareLinks.canonicalUrl(canonical))
        assertEquals(canonical,ShareLinks.canonicalUrl("vocab://share/$id"))
        listOf("https://evil.invalid/share#$id","https://vocab-adam.pages.dev@evil.invalid/share#$id","vocab://share/$id?redirect=evil","vocab://evil/$id","vocab://share/../$id","vocab://share/bad","file:///share#$id").forEach{assertNull(it,ShareLinks.canonicalUrl(it))}
    }
}
