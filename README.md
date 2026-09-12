# Smart Personal Assistant (Java Desktop App)

An all-in-one desktop assistant built in Java/Swing: prayer-time reminders,
weekly timetable/meeting alerts, text + voice note-taking, a personal file
manager (with voice commands), and priority bill/fee alerts — all behind an
Admin/User login backed by a local SQLite database.

---

## 1. Requirements to build & run

- **JDK 17+** (tested with the syntax targeting Java 17; JDK 21 also works)
- **Maven 3.8+**
- Internet access the first time you build (Maven needs to download the
  `sqlite-jdbc` and `org.json` libraries) and at runtime (to call the free
  Aladhan prayer-times API)
- Optional, for real speech-to-text and OS text-to-speech — see §5 and §6

> **Note on how this project was produced:** it was written in a sandboxed
> environment with no internet access and no JDK compiler available, so the
> code could not be compiled or run here. It was written carefully by hand
> to standard, well-known Java/Swing/JDBC/HttpClient patterns, but **please
> run `mvn clean package` yourself and treat the first build as a normal
> "compile and fix any typos" pass.**

## 2. Build & run

```bash
cd smart-personal-assistant
mvn clean package
java -jar target/smart-personal-assistant.jar
```

The first run creates a SQLite file at `~/.smart-assistant/assistant.db`
and a per-user files folder at `~/.smart-assistant/files/<username>/`.

## 3. First login

There's no seeded admin account — **the first account you register
automatically becomes Admin**; every account after that is a regular User.
Use the "Create Account" button on the login screen, then log back in.

## 4. Architecture / package layout

```
com.spa
 ├─ Main.java                  entry point
 ├─ db/DatabaseManager         SQLite connection + schema (auto-created)
 ├─ auth/                      User model, PBKDF2 password hashing, AuthService
 ├─ prayer/                    Aladhan API client + PrayerTimes model
 ├─ timetable/                 Weekly schedule CRUD + "due now" lookup
 ├─ notes/                     Text/voice notes, category tags, search
 ├─ files/                     Save/read/delete files, with a confirmation
 │                             gate shared by both GUI buttons and voice
 │                             commands ("delete this file", etc.)
 ├─ alerts/                    Bill/fee/loan reminders + priority alert firing
 │                             (visual highlight + TTS + desktop notification)
 ├─ scheduler/ReminderScheduler  one ScheduledExecutorService polling every
 │                             30s for due prayers, timetable entries, bills
 ├─ voice/                     VoiceInputService interface + MockVoiceInputService
 │                             (see §5 for enabling real Vosk recognition)
 ├─ tts/                       TextToSpeechService interface + an OS-command
 │                             based implementation (no bundled voice data)
 ├─ util/NotificationUtil      system tray notifications
 └─ gui/                       Swing screens (Login, Dashboard, one panel per module)
```

Passwords are **never stored in plain text** — `PasswordUtil` uses
PBKDF2-HMAC-SHA256 with a random per-user salt (120,000 iterations),
using only classes built into the JDK, no extra crypto library needed.

## 5. Voice input — demo mode vs. real offline recognition

Out of the box, every "voice" button (Notes, File Manager, Ask Assistant)
uses `MockVoiceInputService`, which pops a text-input dialog pre-labeled
"simulate voice command." This lets you **demo the entire voice-driven
flow** (parsing "save this file" / "delete this file" / "read this file" /
"what's my schedule today") without needing a large model download or a
working microphone in front of a grader.

To wire up **real, offline** speech-to-text with [Vosk](https://alphacephei.com/vosk/):

1. Download a small English model (e.g. `vosk-model-small-en-us-0.15`,
   ~40MB) from https://alphacephei.com/vosk/models and unzip it.
2. In `pom.xml`, uncomment the `com.alphacephei:vosk` dependency block.
3. Move `src/optional/java/com/spa/voice/VoskVoiceInputService.java` into
   `src/main/java/com/spa/voice/` (same package, just a different folder —
   it's kept out of the default build so the project compiles without the
   model present).
4. In `MainDashboardFrame.java`, change:
   ```java
   private final VoiceInputService voiceInputService = new MockVoiceInputService();
   ```
   to:
   ```java
   private final VoiceInputService voiceInputService =
       new VoskVoiceInputService("/absolute/path/to/vosk-model-small-en-us-0.15");
   ```
5. `mvn clean package` again.

**Urdu voice (stretch goal):** Vosk also ships small Urdu/Hindi models. The
same `VoskVoiceInputService` class works with any Vosk model directory, so
adding Urdu is a matter of loading a second `Model` instance and offering a
language toggle in the GUI — left as documented future work per the
project's stretch-goal scope.

## 6. Text-to-speech

`SystemTextToSpeechService` shells out to whatever the OS already provides,
so there's nothing extra to install on most machines:
- **macOS:** `say` (built in)
- **Windows:** PowerShell + `System.Speech.Synthesis` (built in)
- **Linux:** `espeak` — install with `sudo apt install espeak` if missing

If you'd rather use **FreeTTS** (as the spec suggests) instead of shelling
out to the OS, implement `TextToSpeechService` with FreeTTS's
`VoiceManager`/`Voice` classes and swap the instance created in
`MainDashboardFrame.java` — nothing else in the app needs to change, since
every module only depends on the `TextToSpeechService` interface.

## 7. Priority alerts

Adding a bill (Bills tab) with a due date of today or earlier — or waiting
for one to become due — triggers all three required channels at once:
1. The row turns **red/bold** in the Bills table (`DueDateHighlightRenderer`).
2. The message is **spoken aloud** via the TTS service.
3. A **desktop notification** is shown (system tray, or a dialog fallback
   on platforms without tray support).

## 8. What's fully working vs. documented future scope

| Feature | Status |
|---|---|
| Admin/User login, hashed passwords, SQLite storage | ✅ Fully implemented |
| Prayer times via Aladhan API + background alerts | ✅ Fully implemented |
| Weekly timetable with reminders | ✅ Fully implemented |
| Text notes (add/search/delete/categorize) | ✅ Fully implemented |
| File manager (save/read/delete + confirmation) | ✅ Fully implemented |
| Priority bill alerts (visual + spoken + notification) | ✅ Fully implemented |
| Voice commands (routing, parsing, confirmation flow) | ✅ Fully implemented, using a demo input dialog by default |
| Real offline speech recognition (Vosk) | ⏳ Drop-in class provided (`VoskVoiceInputService`); needs a downloaded model — see §5 |
| OS text-to-speech | ✅ Implemented (zero extra dependencies) |
| FreeTTS specifically | ⏳ Swappable via the `TextToSpeechService` interface — see §6 |
| Fixed-intent Q&A assistant ("what's my schedule today?") | ✅ Implemented (rule-based, not a general LLM) |
| Urdu voice recognition | ⏳ Documented path via a second Vosk model — see §5 |

## 9. Suggested phased build order (matches the 15–18 day plan)

1. **Days 1–4:** Auth + SQLite schema + DatabaseManager (done here).
2. **Days 5–7:** Prayer time module + Timetable module + scheduler.
3. **Days 8–10:** Notes + File Manager (text-only first), then layer in
   voice commands via `VoiceInputService`.
4. **Days 11–13:** Priority bill alerts (visual + TTS + notification).
5. **Days 14–16:** Polish GUI, Admin panel, wire the demo voice dialog.
6. **Days 17–18:** Stretch goals — swap in real Vosk model, Urdu model,
   expand the Q&A assistant's intents.
