package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SanaDatabase
import com.example.data.model.ChatMessage
import com.example.data.repository.LocalChatRepository
import com.google.firebase.Timestamp
import java.io.File
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalChatRepositoryTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: SanaDatabase
    private lateinit var repo: LocalChatRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.filesDir, "test_sana_chat.db")
        if (dbFile.exists()) dbFile.delete()
        db = Room.databaseBuilder(context, SanaDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        repo = LocalChatRepository(db.chatMessageDao())
    }

    @After
    fun tearDown() {
        if (db.isOpen) {
            db.close()
        }
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    fun insertedMessagesPersistAfterDatabaseReopen() = runTest {
        repo.insertMessage(
            userId = "user_1",
            role = "user",
            text = "مرحبا سنا",
            mode = "chat",
            createdAtMillis = 1000L
        )
        repo.insertMessage(
            userId = "user_1",
            role = "model",
            text = "أهلاً بك! كيف أساعدك؟",
            sourceTitle = "موسوعة",
            mode = "chat",
            createdAtMillis = 2000L
        )

        db.close()

        val reopenedDb = Room.databaseBuilder(context, SanaDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        db = reopenedDb
        val reopenedRepo = LocalChatRepository(reopenedDb.chatMessageDao())

        val persisted = reopenedRepo.observeMessages("user_1").first()
        assertEquals(2, persisted.size)
        assertEquals("user", persisted[0].role)
        assertEquals("مرحبا سنا", persisted[0].text)
        assertEquals("model", persisted[1].role)
        assertEquals("أهلاً بك! كيف أساعدك؟", persisted[1].text)
        assertEquals("موسوعة", persisted[1].sourceTitle)
    }

    @Test
    fun syncFromRemoteMergesSyncedMessagesAndPreservesOfflineLocalMessages() = runTest {
        repo.insertMessage(
            userId = "user_1",
            role = "user",
            text = "رسالة متزامنة",
            createdAtMillis = 1000L
        )
        repo.insertMessage(
            userId = "user_1",
            role = "user",
            text = "رسالة محلية غير متزامنة",
            createdAtMillis = 3000L
        )

        val remote = listOf(
            ChatMessage(
                id = "remote_1",
                userId = "user_1",
                role = "user",
                text = "رسالة متزامنة",
                createdAt = Timestamp(Date(1000L))
            ),
            ChatMessage(
                id = "remote_2",
                userId = "user_1",
                role = "model",
                text = "رد من السحابة",
                createdAt = Timestamp(Date(2000L))
            )
        )

        repo.syncFromRemote("user_1", remote)

        val merged = repo.observeMessages("user_1").first()
        assertEquals(3, merged.size)
        assertEquals("remote_1", merged[0].remoteId)
        assertEquals("remote_2", merged[1].remoteId)
        assertEquals(null, merged[2].remoteId)
        assertEquals("رسالة محلية غير متزامنة", merged[2].text)
    }

    @Test
    fun clearMessagesDeletesOnlyTargetUser() = runTest {
        repo.insertMessage(userId = "user_a", role = "user", text = "A1")
        repo.insertMessage(userId = "user_b", role = "user", text = "B1")

        repo.clearMessages("user_a")

        val listA = repo.observeMessages("user_a").first()
        val listB = repo.observeMessages("user_b").first()
        assertTrue(listA.isEmpty())
        assertEquals(1, listB.size)
        assertEquals("B1", listB[0].text)
    }
}
