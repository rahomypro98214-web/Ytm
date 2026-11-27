# سنا (Sana) — المساعد الذكي بالعربية لنظام Android

تطبيق Android مبني بـ **Kotlin** و **Jetpack Compose (Material 3)** يدعم المحادثات المتدفقة بالذكاء الاصطناعي عبر Firebase AI Logic، وتسجيل الدخول بحساب Google، وحفظ سجل المحادثات محليًا عبر Room مع البحث، والإدخال والقراءة الصوتية، وتوليد اختبارات تفاعلية من سياق المحادثة، واستوديو إنشاء الصور والسيناريوهات.

---

## تصدير وبناء ملف APK عبر GitHub Actions

المشروع جاهز للرفع إلى GitHub ويحتوي على ملفات **Gradle Wrapper** الرسمية (gradlew / gradle-wrapper.jar) ومخطط عمل آلي في:
`.github/workflows/build-apk.yml`

### خطوات الحصول على ملف APK من GitHub:
1. ادفع (Push) هذا المشروع إلى مستودعك على GitHub (الفرع `main` أو `master`).
2. افتح تبويب **Actions** في المستودع واختر مسار العمل **Build Android APK** (أو اضغط **Run workflow** يدويًا).
3. بعد اكتمال البناء، انزل إلى قسم **Artifacts** وحمّل حزمة **`sana-debug-apk`** التي تحتوي على `Sana-debug.apk` الجاهز للتثبيت مباشرة على أجهزة Android.
4. لإنشاء **GitHub Release** تلقائي مرفق به ملف APK، أنشئ وسم إصدار (Tag) يبدأ بحرف `v` مثل:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

### بناء APK موقع للنسخة النهائية (Release APK - اختياري)
لبناء `Sana-release.apk` تلقائيًا في GitHub Actions، أضف هذه المتغيرات في **Settings → Secrets and variables → Actions** بالمستودع:
- `RELEASE_KEYSTORE_BASE64`: محتوى ملف `.jks` مشفّرًا بصيغة Base64 (بـ `keyAlias = upload`).
- `STORE_PASSWORD`: كلمة مرور الـ Keystore.
- `KEY_PASSWORD`: كلمة مرور المفتاح.

---

## البناء محليًا (Android Studio / Terminal)

تأكد من توفر **JDK 21** و **Android SDK (API 36)** ثم نفّذ:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

سيتم إنشاء ملف APK في:
`app/build/outputs/apk/debug/app-debug.apk`

ولتشغيل الاختبارات المحلية:
```bash
./gradlew testDebugUnitTest
```
