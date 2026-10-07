# 📐 AR Measure — Camera দিয়ে যেকোনো কিছু Measure করো

## 🇧🇩 বাংলায় বিবরণ

**AR Measure** একটা Android app যেটা দিয়ে তুমি তোমার phone এর camera ব্যবহার করে real-world objects এর **length, breadth (দৈর্ঘ্য, প্রস্থ)** মাপতে পারবে — **real-time** এ!

### App কী কী করে:
- 📸 **Camera দিয়ে AR Measurement** — Screen এ দুই জায়গায় tap করলে two points এর মধ্যে accurate distance দেখাবে
- 🔲 **Real-time Edge Detection** — Object এর edges green color এ highlight হবে (OpenCV Canny Edge Detection)
- 📏 **cm / inch switch** — দুটো unit-ই support করে
- 📋 **Measurement History** — আগের সব measurement save হয়, পরে দেখা যায়
- 🎯 **Plane Detection** — ARCore দিয়ে floor, table, wall detect করে accurate measurement দেয়
- 🌙 **Dark Theme UI** — সুন্দর dark theme design

---

## 🇬🇧 English Description

**AR Measure** is an Android app that uses your phone's camera and ARCore to measure real-world objects in real-time with edge detection.

### Features:
- **AR Measurement**: Tap two points on any detected surface to get accurate distance
- **Real-time Edge Detection**: OpenCV Canny edge detection highlights object boundaries in green
- **Unit Toggle**: Switch between cm and inches
- **Measurement History**: All measurements are saved and can be viewed later
- **Depth API**: Uses ARCore depth API (when available) for better accuracy
- **Dark Theme**: Modern dark UI design

---

## 🛠️ Setup — কিভাবে চালাবে (Step by Step)

### তোমার যা লাগবে:
1. **Android Studio** (latest version) — [Download করো এখান থেকে](https://developer.android.com/studio)
2. **ARCore supported phone** — [তোমার phone support করে কিনা দেখো](https://developers.google.com/ar/devices)
3. **USB Cable** — phone কে computer-এ connect করতে
4. **Android 7.0+** (API 24+) phone

### Step 1: Android Studio Install করো
1. [developer.android.com/studio](https://developer.android.com/studio) থেকে download করো
2. Install করো (Next, Next, Finish)
3. প্রথমবার open করলে কিছু SDK download হবে — একটু wait করো

### Step 2: Project Open করো
1. Android Studio open করো
2. **File → Open** click করো
3. এই `ARMeasure` folder টা select করো
4. **OK** click করো
5. Gradle sync হবে — কিছু dependency download হবে (প্রথমবার একটু সময় লাগবে)

### Step 3: Phone Ready করো
1. Phone-এ **Settings → About Phone** যাও
2. **Build Number** এ 7 বার tap করো — "Developer mode enabled" দেখাবে
3. **Settings → Developer Options** যাও
4. **USB Debugging** ON করো
5. USB cable দিয়ে phone কে computer-এ connect করো
6. Phone-এ "Allow USB Debugging?" আসলে **Allow** দাও

### Step 4: App Run করো
1. Android Studio তে উপরে তোমার phone এর নাম দেখাবে
2. **Run ▶** button (সবুজ play button) click করো
3. Build হবে, তারপর phone-এ app install হবে
4. App open হবে!

### Step 5: App ব্যবহার করো
1. **Start Measuring** button চাপো
2. Camera permission দাও
3. Phone টা table/floor এর দিকে ধরে একটু আস্তে আস্তে move করো
4. "Surface detected!" দেখলে ready ✅
5. **প্রথম point-এ tap করো** (যেখান থেকে মাপবে)
6. **দ্বিতীয় point-এ tap করো** (যেখানে শেষ)
7. Screen-এ measurement দেখাবে! 🎉

### Edge Detection:
- নিচে **Edge: OFF** button চাপলে **Edge: ON** হবে
- Object এর edges green-এ highlight হবে

---

## 📁 Project Structure

```
ARMeasure/
├── app/
│   ├── build.gradle.kts          ← App dependencies ও settings
│   ├── proguard-rules.pro        ← Release build rules
│   └── src/main/
│       ├── AndroidManifest.xml   ← App permissions ও activities
│       ├── java/com/armeasure/app/
│       │   ├── MainActivity.kt       ← Home screen
│       │   ├── MeasureActivity.kt     ← AR Measurement screen (main logic)
│       │   ├── AnchorNode.kt          ← AR anchor point ও line drawing
│       │   ├── MeasurementData.kt     ← Data model ও storage
│       │   └── HistoryActivity.kt     ← Measurement history screen
│       └── res/
│           ├── layout/               ← UI layouts (XML)
│           ├── drawable/             ← Shapes ও backgrounds
│           └── values/               ← Colors, strings, themes
├── build.gradle.kts              ← Root build file
├── settings.gradle.kts           ← Project settings
├── gradle.properties             ← Gradle config
└── README.md                     ← এই file!
```

---

## 🔧 Technologies Used

| Technology | Purpose |
|---|---|
| **Kotlin** | Android app-এর programming language |
| **ARCore** | Google-এর AR SDK — real-world surface ও depth detect করে |
| **SceneView** | ARCore-এর উপর easier AR rendering library |
| **OpenCV** | Computer vision library — edge detection এর জন্য |
| **Material Design** | Modern Android UI components |
| **ViewBinding** | Type-safe layout access |

---

## ⚠️ Important Notes

1. **ARCore supported phone লাগবে** — পুরানো/সস্তা phone-এ কাজ নাও করতে পারে
2. **ভালো lighting দরকার** — অন্ধকারে surface detect ভালো হয় না
3. **Flat surface-এ best কাজ করে** — table, floor, wall
4. **Measurement accuracy** — ±1-2cm variation হতে পারে, এটা normal (professional tools এর মতো exact না)
5. **Edge detection** সব object-এর জন্য equally ভালো কাজ নাও করতে পারে — contrasting colors-এ best কাজ করে

---

## 🚀 Future Improvements (পরে add করা যাবে)

- 📸 Screenshot save with measurement overlay
- 📐 Area (ক্ষেত্রফল) calculation — 4 points select করে
- 🎨 Custom colors for measurement lines
- 📊 Export measurements to CSV/Excel
- 🔊 Voice readout of measurements
- 📱 Widget for quick access
- 🌐 Multiple language support

---

## 📄 License

This project is free to use. Made with ❤️ for learning.

---

*Built with ARCore + OpenCV + Kotlin for Android*
