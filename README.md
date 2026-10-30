# تطبيق Focus On Top للعملاء (Android + iPhone)

تطبيق Kotlin Multiplatform + Compose: **كود واحد** يطلع منه تطبيق أندرويد وتطبيق آيفون.
للعملاء فقط — حساب الأدمن أو الموظف يُرفض عند الدخول.
ثلاث لغات: العربية (من اليمين لليسار) / Deutsch / English — تتغير من شاشة البداية أو من "حسابي".

يتصل مباشرة بالسيستم: `https://www.focusontop.de` (نفس الحسابات، نفس الملفات، نفس الفواتير، نفس الإشعارات).

---

## ١) أندرويد — Android Studio

1. ثبّت **Android Studio** (آخر نسخة) وافتح مجلد `client-app` كما هو (File → Open).
2. انتظر حتى ينتهي Gradle Sync (أول مرة يأخذ عدة دقائق لأنه ينزّل المكتبات).
3. اختر `composeApp` من الأعلى واضغط ▶ Run على هاتف أو محاكي.
4. لنسخة المتجر: Build → Generate Signed App Bundle → استعمل مفتاحك الموجود في `~/focusontop-keys`
   (لا تضع المفتاح داخل المشروع).

### الإشعارات على أندرويد (Firebase)
1. Firebase Console → مشروع **focus-on-top** → Project settings → Add app → Android.
2. Package name: `de.focusontop.client`
3. نزّل `google-services.json` وضعه في: `client-app/composeApp/google-services.json`
4. أعد Sync. (بدون الملف التطبيق يشتغل عادي لكن بدون إشعارات).

السيرفر يرسل الإشعارات بنفس ملف الخدمة الموجود أصلاً: `data/push/firebase-service-account.json`.

---

## ٢) آيفون — Xcode (يحتاج جهاز Mac)

1. ثبّت **Xcode** و **JDK 17** (مثلاً: `brew install openjdk@17`).
2. أول مرة: من الطرفية داخل `client-app` شغّل `./gradlew :composeApp:assemble` مرة واحدة (أو افتح المشروع في Android Studio).
3. افتح `iosApp/iosApp.xcodeproj` في Xcode.
4. في `iosApp/Configuration/Config.xcconfig` اكتب `TEAM_ID=` (رقم فريقك من developer.apple.com → Membership).
5. Signing & Capabilities: تأكد أن **Push Notifications** مفعّل (الملف `iosApp.entitlements` موجود).
6. اختر جهازك واضغط ▶ Run. Xcode يبني كود Kotlin تلقائياً (خطوة "Compile Kotlin Framework").
7. للمتجر: Product → Archive، ثم في الملف `iosApp.entitlements` غيّر `development` إلى `production`.

### الإشعارات على آيفون (APNs)
- developer.apple.com → Identifiers → أنشئ App ID: `de.focusontop.client` مع Push Notifications.
- Keys → أنشئ مفتاح APNs (ملف `.p8`) — احتفظ به في `~/focusontop-keys`.
- على السيرفر: `data/push/apns.json` (نفس الإعداد المستعمل حالياً). السيرفر يرسل للتطبيق الجديد
  باستعمال `appId` الذي يرسله التطبيق مع التوكن (`de.focusontop.client`) — لا يتأثر تطبيق الموبايل الحالي.

---

## ماذا يوجد في التطبيق (v1)

| الشاشة | ماذا يرى العميل |
|---|---|
| البداية | الشعار، اختيار اللغة، "تسجيل الدخول" |
| الدخول | بريد + كلمة مرور (+ رمز التحقق الثنائي إن كان مفعّلاً)، "نسيت كلمة المرور" |
| الرئيسية | تحية، **ما ينتظر ردّك** (موافقة / طلب تعديل بضغطة)، تقدّم الأعمال، مسؤول الحساب، آخر فاتورة |
| أعمالي | كل الملفات المسلّمة (فيديو / صور / مستندات) مع فلتر بسيط |
| تفاصيل ملف | تشغيل الفيديو مباشرة، الموافقة أو طلب التعديل، التعليقات، تنزيل |
| الفواتير | الحالة (مدفوعة / مفتوحة / متأخرة) وفتح PDF |
| حسابي | الاشتراك + توقيع العقد، اللغة، الإشعارات، الخصوصية، تسجيل الخروج، طلب حذف الحساب |
| الإشعارات | كل الإشعارات، تعليم كمقروء، فتح الملف المرتبط |

- العميل **لا يرى ملفات RAW أبداً** — السيرفر يرسل له فقط الملفات المسلّمة (`/api/client-app/home`).
- لا توجد محادثة بين العميل والموظف — إشعارات فقط (كما في السيستم).
- الدخول بـ Google / Apple غير موجود في هذه النسخة (بريد + كلمة مرور فقط).

## الملفات المهمة

```
composeApp/src/commonMain/   ← كل الشاشات والمنطق (مشترك بين أندرويد وآيفون)
  i18n/Strings.kt            ← نصوص اللغات الثلاث
  data/Api.kt                ← الاتصال بالسيرفر (BASE_URL)
  ui/                        ← الشاشات والألوان
  composeResources/drawable  ← الشعار والأيقونة
composeApp/src/androidMain/  ← أندرويد: الأيقونة، الإشعارات، مشغّل الفيديو
composeApp/src/iosMain/      ← آيفون: مشغّل الفيديو، الإشعارات
iosApp/                      ← مشروع Xcode
```

الإصدارات: Kotlin 2.1.0 · Compose Multiplatform 1.7.3 · AGP 8.7.3 · Gradle 8.10.2 · minSdk 26 · iOS 15+.
