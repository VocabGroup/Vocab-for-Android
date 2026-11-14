package com.example

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.data.entity.*
import com.example.data.assistant.AssistantMessage
import com.example.data.settings.AppSettings
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers="w360dp-h640dp-420dpi",sdk=[36])
class PageAuditTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 private val decks=listOf(Deck(id=1,name="日常英文第一課",category="日常英文"))
 private val cards=(1..3).map{Flashcard(id=it.toLong(),deckId=1,word="journey$it",definition="旅程，學習與探索",exampleSentence="Enjoy your journey.",exampleTranslation="享受旅程。")}
 private val note=GrammarNote(id=1,title="too … to",course="日常英文",folder="第一課",summary="太…而無法…",structure="too + adjective + to + V")
 private val question=GrammarQuestion(id=1,grammarNoteId=1,prompt="We {{answer}} to school.",answer="go")
 private val library=GrammarLibrary(id=1,name="第一課",course="日常英文")
 private val record=GrammarWritingRecord(id=1,title="My journey",originalText="I go school.",revisedText="I go to school.")
 private val article=AssistantMessage("article","fixture","ASSISTANT","A journey begins with learning.",1L,kind="ARTICLE")
 private val pages:List<Pair<String,@Composable ()->Unit>> = listOf(
"AddEditCardScreen" to { AddEditCardScreen(editingCard=cards.first(), allCards=cards, decks=decks, onGetDictionaryMatch={_->null}, onAddFolder={_,_,_,_->}, onSaveBatchCards={_,_->Result.failure(IllegalStateException("測試資料"))}, onBack={}) },
"AiAssistantScreen" to { AiAssistantScreen(conversations=emptyList(), currentConversationId=null, messages=emptyList(), busy=false, status=null, sendOutcome=null, pendingAction=null, undoAction=null, apiConfigured=false, contextDeckName=null, decks=decks, cards=cards, selectedDeckIds=emptySet(), onScopeChange={_->}, onSend={_,_->}, onStop={}, onQuizCompleted={_,_,_->}, onQuizRestart={_->}, onQuizProgress={_,_,_,_,_->}, onSpeak={_->}, onStopSpeaking={}, onPronunciationResult={_,_->}, onReadingMistake={_->}, onCopyReadingCard={_,_,_->}, onCreateFolderAndCopyReadingCard={_,_,_,_,_,_->}, onLookupWord={_->Result.failure(IllegalStateException("測試資料"))}, onDeleteMessage={_->}, onConfirmAction={}, onCancelAction={}, onUndoAction={}, onNewConversation={_->}, onSelectConversation={_->}, onDeleteConversation={_->}, onClearChats={}, onOpenSettings={}, onBack={}) },
"AiGenerationLoadingScreen" to { AiGenerationLoadingScreen(title="課程中的教材", status=null, onBack={}) },
"CardListScreen" to { CardListScreen(cards=cards, onToggleFavorite={_->}, onDeleteCard={_->}, onEditCard={_->}, onSpeak={_->}, onAddNewCard={}, onBack={}) },
"DashboardScreen" to { DashboardScreen(decks=decks, allCards=cards, dueCards=cards, totalCount=3, masteredCount=0, dueCount=3, logs=emptyList(), makeUpDates=emptySet(), dailyGoalCards=20, selectedDeckId=null, onSelectDeck={_->}, onAddFolder={_,_,_,_->}, onStartReview={}, onOpenAddCard={}, onOpenPhotoOcr={}, onOpenQuizGames={_->}, onOpenCardList={}, onOpenSettings={}, onOpenExternalImport={}, onStartInteractiveReading={_->}, onOpenStudyCalendar={}, onOpenQualityCheck={}) },
"ExternalImportScreen" to { ExternalImportScreen(decks=decks, onImportCards={_,_,_->}, onBack={}) },
"FlashcardReviewScreen" to { FlashcardReviewScreen(cards=emptyList(), onRecordReview={_,_->}, onToggleFavorite={_->}, onSpeak={_->}, onBackToDashboard={}) },
"FoldersManagementScreen" to { FoldersManagementScreen(decks=decks, allCards=cards, onAddFolder={_,_,_,_->}, onUpdateFolder={_->}, onDeleteFolder={_->}, onOpenCardList={_->}, onJumpToStudy={_->}, onJumpToQuiz={_->}) },
"GrammarImportScreen" to { GrammarImportScreen(onBack={}, onManual={}, onAiCreate={}, onRecognize={_,_->emptyList()}, onRecognizeText={_,_->GrammarDraft()}, onImportShare={_->emptyList()}, onPreview={_->}) },
"GrammarNoteDetailScreen" to { GrammarNoteDetailScreen(note=note, patterns=emptyList(), examples=emptyList(), questionCount=3, onBack={}, onEdit={}, onDelete={}, onFavorite={}, onSpeak={_->}, onLearn={}, onQuiz={}) },
"GrammarImportPreviewScreen" to { GrammarImportPreviewScreen(initialDrafts=listOf(GrammarDraft(title="辨識文法")), courses=listOf("日常英文"), onBack={}, onSave={_,_,_->}) },
"GrammarQuizResultScreen" to { GrammarQuizResultScreen(noteTitle="課程中的教材", result=GrammarQuizResult(1,1,1,1,emptyList(),emptyMap(),0,10), questions=listOf(question), onBack={}, onPracticeWrong={}, onRetryAll={}) },
"GrammarQuizFlowScreen" to { GrammarQuizFlowScreen(noteId=1L, noteTitle="課程中的教材", masteryBefore=0, questions=listOf(question), onAnswer={_,_,_->}, onBack={}, onComplete={_->}) },
"GrammarLibrariesScreen" to { GrammarLibrariesScreen(libraries=listOf(library), notes=listOf(note), highlightedAction="manage", onAdd={_,_,_,_->}, onUpdate={_->}, onDelete={_->}, onReorder={_->}, onManage={_->}, onLearn={_->}, onQuiz={_->}) },
"GrammarLibraryManageScreen" to { GrammarLibraryManageScreen(library=library, notes=listOf(note), onEdit={_->}, onDelete={_->}, onReorder={_->}, onAdd={}, onBack={}) },
"GrammarDashboardScreen" to { GrammarDashboardScreen(notes=listOf(note), onAdd={}, onImport={}, onOpen={_->}, onLearn={_->}, onQuiz={_->}) },
"GrammarNotesScreen" to { GrammarNotesScreen(notes=listOf(note), onAdd={}, onImport={}, onReorderNotes={_->}, onOpen={_->}) },
"GrammarStudyHubScreen" to { GrammarStudyHubScreen(notes=listOf(note), onLearn={_->}, onQuiz={_->}, onOpen={_->}) },
"GrammarEditorScreen" to { GrammarEditorScreen(note=null, questions=listOf(question), patterns=emptyList(), examples=emptyList(), courses=listOf("日常英文"), onGenerateAi={_->GrammarDraft()}, onScanImages={_->GrammarDraft()}, onSave={_,_,_->}, onBack={}) },
"GrammarDetailScreen" to { GrammarDetailScreen(note=note, questionCount=3, onBack={}, onEdit={}, onDelete={}, onFavorite={}, onLearn={}, onQuiz={}) },
"GrammarLearnScreen" to { GrammarLearnScreen(note=note, patterns=emptyList(), examples=emptyList(), onProgress={_->}, onRatePattern={_,_->}, onBack={}, onQuiz={}) },
"GrammarQuizScreen" to { GrammarQuizScreen(note=note, questions=listOf(question), onAnswer={_,_->}, onBack={}, onRetry={}) },
"GrammarWritingCheckScreen" to { GrammarWritingCheckScreen(weaknesses=emptyList(), onCheck={_->emptyList()}, onAccept={_->}) },
"GrammarQuizHubScreen" to { GrammarQuizHubScreen(notes=listOf(note), questions=listOf(question), weaknesses=emptyList(), onStartTopicQuiz={_,_->}, onGenerateAiTopicQuiz={_->}, onWritingCheck={_->emptyList()}, onAcceptWritingIssue={_->}) },
"GrammarSearchScreen" to { GrammarSearchScreen(notes=listOf(note), onOpen={_->}, onBack={}) },
"InteractiveReadingScreen" to { InteractiveReadingScreen(message=article, cards=cards, decks=decks, onSpeak={_->}, onStopSpeaking={}, onPronunciationResult={_,_->}, onReadingMistake={_->}, onCopyCardToDeck={_,_,_->}, onCreateFolderAndCopyCard={_,_,_,_,_,_->}, onLookupWord={_->Result.failure(IllegalStateException("測試資料"))}, onDelete={}, onBack={}) },
"MainScreen" to { MainScreen(currentTab=0, onTabSelected={_->}, dashboardContent={}, foldersContent={}, studyContent={}, quizContent={}) },
"OnboardingGoalScreen" to { OnboardingGoalScreen(onComplete={_,_,_->}) },
"PhotoOcrScreen" to { PhotoOcrScreen(decks=decks, candidates=emptyList(), onImportCandidates={_,_,_->}, onBack={}) },
"QuizGamesScreen" to { QuizGamesScreen(cards=cards,decks=decks) },
"ReadingLibraryScreen" to { ReadingLibraryScreen(decks=decks, articleMessages=listOf(article), onGenerate={_->}, onGenerateTopic={_->}, onImport={_->}, onRecognizeSources={_->GrammarWritingScanResult("A journey.")}, onOpenArticle={_->}) },
"WritingLibraryScreen" to { WritingLibraryScreen(records=listOf(record), onNew={}, onImport={}, onOpen={_->}) },
"WritingRecordDetailScreen" to { WritingRecordDetailScreen(record=record, onBack={}, onEdit={}, onDelete={}) },
"SettingsScreen" to { SettingsScreen(settings=AppSettings(), onBooleanChange={_,_->}, onIntChange={_,_->}, onReminderTimesChange={_->}, onThemeChange={_->}, onSpeechRateChange={_->}, onDictionarySourceChange={_->}, ttsVoices=emptyList(), onTtsVoiceStyleChange={_->}, onTtsVoiceNameChange={_->}, onPreviewTtsVoice={}, dataMessage=null, apiKeyConfigured=false, aiApiProfiles=emptyList(), availableAiModels=emptyList(), aiConnectionStatus=null, onAiProviderChange={_->}, onAiModelChange={_->}, onAiChatStyleChange={_->}, onAiPromptChange={_->}, onResetAiPrompt={}, onAiImagePromptChange={_->}, onResetAiImagePrompt={}, onSaveApiKey={_->}, onClearApiKey={_->}, onRefreshAiModels={}, onTestAiConnection={_->}, onExportBackup={_->}, onExportCsv={_->}, onImportBackup={_->}, onClearAllData={}, onBack={}) },
"StudyCalendarScreen" to { StudyCalendarScreen(logs=emptyList(), dailyGoalCards=20, makeUpDates=emptySet(), onToggleMakeUp={_->}, onBack={}) },
"StudyHubScreen" to { StudyHubScreen(decks=decks, allCards=cards, onStartReviewForDeck={_->}, onStartReviewForCourse={_->}) },
"VocabularyQualityScreen" to { VocabularyQualityScreen(cards=cards, decks=decks, onEditCard={_->}, onRunAiAudit={}, onBack={}) },
"WordLookupScreen" to { WordLookupScreen(decks=decks, cards=cards, onBack={}, onLookup={_->Result.failure(IllegalStateException("測試資料"))}, onAiComplete={_->null}, onOpenLocalCard={_->}, onAddToDeck={_,_,_->}, onCreateFolderAndAdd={_,_,_,_,_,_->}) },
 )
 private fun sweep(dark:Boolean){
  var selected by mutableIntStateOf(0)
  compose.setContent { MyApplicationTheme(darkTheme=dark){Surface(Modifier.fillMaxSize()){key(selected){pages[selected].second()}}} }
  pages.indices.forEach { index ->
   compose.runOnIdle { selected=index };compose.waitForIdle()
   compose.onRoot().captureRoboImage("build/outputs/page-audit/${if(dark)"dark-landscape" else "light-large-text"}-${pages[index].first}.png")
   val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
   assertTrue("${pages[index].first}: visible root",root.width>0&&root.height>0)
  }
 }
 @Test fun everyScreenNarrowWithLargeText(){RuntimeEnvironment.setFontScale(1.3f);sweep(false)}
 @Test fun everySettingsSubpageCanReturnWithoutChangingData(){
  RuntimeEnvironment.setFontScale(1.3f)
  compose.setContent{MyApplicationTheme{pages.first{it.first=="SettingsScreen"}.second()}}
  listOf("外觀","學習","AI","掃描與文件","單字查詢","發音與提醒","更新","資料").forEachIndexed{index,title->
   compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)).performScrollToNode(hasText(title))
   compose.onNodeWithText(title,useUnmergedTree=true).performScrollTo().performClick()
   compose.onRoot().captureRoboImage("build/outputs/page-audit/settings-$index.png")
   compose.onNodeWithContentDescription("返回").assertIsDisplayed().performClick()
   compose.onNodeWithText("依照你的方式學習").assertExists()
  }
 }
 @Test @Config(qualifiers="w360dp-h320dp-420dpi",sdk=[36]) fun compactFormDialogKeepsActionsReachable(){
  RuntimeEnvironment.setFontScale(1.3f)
  var created=""
  compose.setContent{MyApplicationTheme{
   var name by remember{mutableStateOf("")}
   com.example.ui.components.ModernAlertDialog(onDismissRequest={},title={Text("編輯資料夾")},text={
    Column(Modifier.verticalScroll(rememberScrollState())){TextField(name,{name=it},label={Text("名稱")},modifier=Modifier.testTag("folder_name_input"));Text("內容說明。".repeat(30))}
   },confirmButton={Button(onClick={created=name},enabled=name.isNotBlank(),modifier=Modifier.testTag("confirm_create_folder_button")){Text("儲存全部變更")}},dismissButton={TextButton(onClick={}){Text("取消")}})
  }}
  compose.onNodeWithTag("folder_name_input").performScrollTo().performTextInput("保留完整名稱")
  compose.onNodeWithTag("confirm_create_folder_button").assertIsDisplayed().assertIsEnabled().performClick()
  compose.runOnIdle{assertEquals("保留完整名稱",created)}
 }
 @Test @Config(qualifiers="w844dp-h390dp-420dpi",sdk=[36]) fun everyScreenLandscapeDark(){sweep(true)}
 @Test fun grammarTypingAndConfirmStayAboveSimulatedKeyboard(){
  var observedIme=0;var answers=0
  WindowCompat.setDecorFitsSystemWindows(compose.activity.window,false)
  compose.activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
  compose.setContent{MyApplicationTheme{val bottom=WindowInsets.ime.getBottom(LocalDensity.current);SideEffect{observedIme=bottom};GrammarQuizFlowScreen(1,"文法填空",0,listOf(question),{_,_,_->answers++},{},{})}}
  compose.onNodeWithText("輸入完整答案").performTextInput("go")
  val density=compose.activity.resources.displayMetrics.density;val height=(240*density).toInt()
  val insets=WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.ime(),Insets.of(0,0,0,height)).setVisible(WindowInsetsCompat.Type.ime(),true).build()
  compose.runOnIdle {
   fun dispatch(view:View){ViewCompat.dispatchApplyWindowInsets(view,insets);if(view is ViewGroup)(0 until view.childCount).forEach{dispatch(view.getChildAt(it))}}
   dispatch(compose.activity.window.decorView)
  }
  compose.waitForIdle();assertEquals("IME fixture must actually reach Compose",height,observedIme)
  val screen=compose.onRoot().fetchSemanticsNode().boundsInRoot
  val button=compose.onNodeWithText("確認答案").fetchSemanticsNode().boundsInRoot
  assertTrue("confirm must be above keyboard",button.bottom<=screen.bottom-height+1)
  compose.onNodeWithText("確認答案").performClick();compose.runOnIdle{assertEquals(1,answers)}
  compose.onRoot().captureRoboImage("build/outputs/page-audit/grammar-keyboard.png")
 }
 @Test @Config(qualifiers="w640dp-h320dp-420dpi",sdk=[36]) fun longGrammarCanScrollWithoutCoveringNext(){
  var progress=0
  compose.setContent{MyApplicationTheme{GrammarLearnScreen(note.copy(summary=("說明內容與例句。\n").repeat(50)),emptyList(),emptyList(),{progress=it},{_,_->},{},{})}}
  compose.onNodeWithTag("grammar_learn_content").assert(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollBy))
  compose.onNodeWithTag("grammar_learn_content").performTouchInput{swipeUp()}
  compose.onNodeWithText("下一個").assertIsDisplayed().performClick();compose.runOnIdle{assertEquals(1,progress)}
 }
}
