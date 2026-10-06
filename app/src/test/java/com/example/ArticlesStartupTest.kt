package com.example

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.assistant.AssistantChatStore
import com.example.data.assistant.AssistantConversation
import com.example.data.assistant.AssistantMessage
import com.example.viewmodel.VocabularyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ArticlesStartupTest {
    @Test fun savedLibraryIsAvailableAtStartupWhileNoChatHasBeenOpened() {
        Dispatchers.setMain(StandardTestDispatcher())
        val application = ApplicationProvider.getApplicationContext<Application>()
        val store = AssistantChatStore(application)
        store.clear()
        store.save(listOf(AssistantConversation("old-chat", "舊文章", 1, 2)),
            listOf(AssistantMessage("old-article", "old-chat", "ASSISTANT", "A saved article.", 2, "ARTICLE", "{}")))
        val owner = ViewModelStore()
        try {
            val viewModel = VocabularyViewModel(application)
            owner.put("vocab", viewModel)
            assertTrue(viewModel.assistantMessages.value.isEmpty())
            assertNull(viewModel.assistantCurrentConversationId.value)
            assertEquals("old-article", viewModel.savedArticles.value.single().id)
        } finally {
            owner.clear(); store.clear(); Dispatchers.resetMain()
        }
    }
}
