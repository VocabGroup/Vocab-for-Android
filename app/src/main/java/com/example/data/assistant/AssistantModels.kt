package com.example.data.assistant

data class AssistantConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val incognito: Boolean = false
)

data class AssistantMessage(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val kind: String = "TEXT",
    val payload: String = "",
    val attachmentUris: List<String> = emptyList()
)

data class AssistantPendingAction(
    val id: String,
    val type: String,
    val deckId: Long,
    val title: String,
    val description: String,
    val payload: String = ""
)

data class AssistantQuizOption(
    val text: String,
    val correct: Boolean
)

/** Library history is independent of whichever chat is currently selected. */
fun List<AssistantMessage>.savedArticles(): List<AssistantMessage> =
    filter { it.role == "ASSISTANT" && it.kind == "ARTICLE" }
        .distinctBy { it.id }.sortedByDescending { it.createdAt }

data class AssistantQuizQuestion(
    val prompt: String,
    val explanation: String,
    val options: List<AssistantQuizOption>
)
