package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.CourseProgress
import com.example.data.model.CourseUi
import com.example.data.model.CustomCourseDocument
import com.example.data.model.KnowledgeSource
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SanaFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google قبل الوصول إلى البيانات.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeKnowledgeSources(userId: String): Flow<List<KnowledgeSource>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Mismatch in authenticated user ID" }
        val path = "users/$uid/knowledgeSources"
        emitAll(
            userDoc(uid).collection("knowledgeSources")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        KnowledgeSource::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Mismatch in authenticated user ID" }
        val path = "users/$uid/chatMessages"
        emitAll(
            userDoc(uid).collection("chatMessages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCourseProgress(userId: String): Flow<Map<String, CourseProgress>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Mismatch in authenticated user ID" }
        val path = "users/$uid/courseProgress"
        emitAll(
            userDoc(uid).collection("courseProgress")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CourseProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).associateBy { it.id }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCustomCourses(userId: String): Flow<List<CourseUi>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Mismatch in authenticated user ID" }
        val path = "users/$uid/customCourses"
        emitAll(
            userDoc(uid).collection("customCourses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomCourseDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toCourseUi() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addKnowledgeSource(
        title: String,
        sourceLabel: String,
        sourceUrl: String?,
        sourceType: String,
        summary: String,
        content: String,
        keywords: List<String>,
        factCount: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/knowledgeSources"
        return try {
            val activeUid = requireUserId()
            val safeContent = content.trim().take(15000)
            require(safeContent.length >= 10) { "المحتوى قصير جدًا للحفظ." }
            val safeTitle = title.trim().ifBlank { "مصدر معرفي" }.take(150)
            val safeLabel = sourceLabel.trim().ifBlank { safeTitle }.take(200)
            val safeSummary = summary.trim().take(1500)
            val safeKeywords = keywords.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(10)
            val safeFactCount = factCount.coerceIn(1, 500)
            val safeType = if (sourceType in listOf("site", "text", "url")) sourceType else "text"

            val docRef = userDoc(activeUid).collection("knowledgeSources").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to activeUid,
                "title" to safeTitle,
                "sourceLabel" to safeLabel,
                "sourceType" to safeType,
                "summary" to safeSummary,
                "content" to safeContent,
                "keywords" to safeKeywords,
                "factCount" to safeFactCount,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!sourceUrl.isNullOrBlank()) {
                payload["sourceUrl"] = sourceUrl.trim().take(500)
            }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteKnowledgeSource(sourceId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/knowledgeSources/$sourceId"
        return try {
            val activeUid = requireUserId()
            require(sourceId.isNotBlank()) { "Source ID is blank" }
            val docRef = userDoc(activeUid).collection("knowledgeSources").document(sourceId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addChatMessage(
        role: String,
        text: String,
        sourceTitle: String? = null,
        mode: String? = null
    ): Result<String> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/chatMessages"
        return try {
            val activeUid = requireUserId()
            val safeText = text.trim().take(15000)
            require(safeText.isNotEmpty()) { "نص الرسالة فارغ." }
            val safeRole = if (role == "model") "model" else "user"

            val docRef = userDoc(activeUid).collection("chatMessages").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to activeUid,
                "role" to safeRole,
                "text" to safeText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!sourceTitle.isNullOrBlank()) {
                payload["sourceTitle"] = sourceTitle.trim().take(200)
            }
            if (!mode.isNullOrBlank() && mode in listOf("chat", "summarize", "translate", "kb")) {
                payload["mode"] = mode
            }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearChatHistory(messageIds: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid
        val basePath = "users/${uid ?: "unauthenticated"}/chatMessages"
        return try {
            val activeUid = requireUserId()
            if (messageIds.isEmpty()) return Result.success(Unit)
            val col = userDoc(activeUid).collection("chatMessages")
            messageIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    if (id.isNotBlank()) batch.delete(col.document(id))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, basePath)
            Result.failure(e)
        }
    }

    suspend fun markLessonCompleted(courseId: String, lessonIndex: Int, existing: CourseProgress?): Result<Unit> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/courseProgress/$courseId"
        return try {
            val activeUid = requireUserId()
            require(courseId.isNotBlank()) { "Course ID is blank" }
            val docRef = userDoc(activeUid).collection("courseProgress").document(courseId)
            docPath = docRef.path

            val updatedLessons = ((existing?.completedLessons ?: emptyList()) + lessonIndex.toLong())
                .distinct()
                .sorted()
                .take(20)

            if (existing == null || existing.createdAt == null) {
                val payload = mapOf(
                    "userId" to activeUid,
                    "completedLessons" to updatedLessons,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "completedLessons" to updatedLessons,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveQuizResult(
        courseId: String,
        score: Int,
        total: Int,
        existing: CourseProgress?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/courseProgress/$courseId"
        return try {
            val activeUid = requireUserId()
            require(courseId.isNotBlank()) { "Course ID is blank" }
            val docRef = userDoc(activeUid).collection("courseProgress").document(courseId)
            docPath = docRef.path

            val bestScore = maxOf(existing?.quizBestScore?.toInt() ?: 0, score).coerceIn(0, 20)
            val safeTotal = total.coerceIn(1, 20)
            val existingLessons = existing?.completedLessons ?: emptyList()

            if (existing == null || existing.createdAt == null) {
                val payload = mapOf(
                    "userId" to activeUid,
                    "completedLessons" to existingLessons,
                    "quizBestScore" to bestScore,
                    "quizTotal" to safeTotal,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "quizBestScore" to bestScore,
                        "quizTotal" to safeTotal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveCustomCourse(course: CourseUi): Result<String> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/customCourses"
        return try {
            val activeUid = requireUserId()
            val docRef = userDoc(activeUid).collection("customCourses").document()
            docPath = docRef.path

            val lessonsPayload = course.lessons.take(10).map { lesson ->
                mapOf(
                    "title" to lesson.title.take(120),
                    "body" to lesson.body.take(4000)
                )
            }
            val quizPayload = course.quiz.take(10).map { q ->
                mapOf(
                    "question" to q.question.take(300),
                    "options" to q.options.take(6).map { it.take(200) },
                    "correctIndex" to q.correctIndex.coerceIn(0, q.options.lastIndex.coerceAtLeast(0)),
                    "explanation" to q.explanation.take(400)
                )
            }

            val payload = mapOf(
                "userId" to activeUid,
                "title" to course.title.take(120),
                "description" to course.description.take(300),
                "iconEmoji" to course.iconEmoji.ifBlank { "✨" }.take(16),
                "lessons" to lessonsPayload,
                "quiz" to quizPayload,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomCourse(courseId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/customCourses/$courseId"
        return try {
            val activeUid = requireUserId()
            require(courseId.isNotBlank()) { "Course ID is blank" }
            val docRef = userDoc(activeUid).collection("customCourses").document(courseId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
