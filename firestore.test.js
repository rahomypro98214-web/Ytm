const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any user subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chatMessages").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("courseProgress").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customCourses").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "مرحبا",
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("KnowledgeSource: owner create, partial update, list, delete succeed; ghost fields & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").doc("src_1").set({
      userId: ALICE_UID,
      title: "موسوعة الفضاء",
      sourceLabel: "موسوعة الفضاء",
      sourceUrl: "https://space-encyclopedia.example/planets",
      sourceType: "site",
      summary: "معلومات عن الكواكب والمجموعة الشمسية.",
      content: "المجموعة الشمسية تضم ثمانية كواكب تدور حول الشمس.",
      keywords: ["الفضاء", "الكواكب", "المشتري"],
      factCount: 5,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List succeeds on empty or populated user subcollection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").get()
  );

  // Partial update on summary/keywords/updatedAt succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").doc("src_1").update({
      summary: "ملخص محدث عن المجموعة الشمسية.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").doc("src_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId 'id' ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").doc("src_2").set({
      id: "src_2",
      userId: ALICE_UID,
      title: "مصدر",
      sourceLabel: "نص",
      sourceType: "text",
      summary: "ملخص",
      content: "محتوى كافٍ للاختبار في قاعدة البيانات.",
      keywords: ["اختبار"],
      factCount: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").doc("src_future").set({
      userId: ALICE_UID,
      title: "مصدر",
      sourceLabel: "نص",
      sourceType: "text",
      summary: "ملخص",
      content: "محتوى كافٍ للاختبار في قاعدة البيانات.",
      keywords: [],
      factCount: 1,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user access denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").doc("src_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("knowledgeSources").doc("src_1").delete()
  );
});

test("ChatMessages: owner create & list & delete succeed; update forbidden; invalid role fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "model",
      text: "مرحبًا! أنا سنا.",
      sourceTitle: "موسوعة الفضاء",
      mode: "kb",
      createdAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages").orderBy("createdAt").get()
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("msg_1").update({
      text: "تم التعديل",
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("msg_2").set({
      userId: ALICE_UID,
      role: "admin",
      text: "غير مسموح",
      createdAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("msg_1").delete()
  );
});

test("CourseProgress & CustomCourse: valid payloads succeed, invalid shapes fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("courseProgress").doc("ai").set({
      userId: ALICE_UID,
      completedLessons: [0, 1],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("courseProgress").doc("ai").update({
      completedLessons: [0, 1, 2],
      quizBestScore: 3,
      quizTotal: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customCourses").doc("course_1").set({
      userId: ALICE_UID,
      title: "الأمن السيبراني",
      description: "مقدمة عملية لحماية حساباتك وبياناتك.",
      iconEmoji: "🛡️",
      lessons: [{ title: "كلمات المرور", body: "استخدم كلمات مرور طويلة وفريدة." }],
      quiz: [{ question: "ما هي المصادقة الثنائية؟", options: ["أ", "ب"], correctIndex: 0, explanation: "تضيف طبقة حماية ثانية." }],
      createdAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customCourses").doc("course_bad").set({
      userId: ALICE_UID,
      title: "فارغة",
      description: "وصف",
      iconEmoji: "✨",
      lessons: [],
      quiz: [],
      createdAt: created,
    })
  );
});
