# MindQuest: Project Specification Protocol

## 1. Overview

**MindQuest** is a text-based trivia RPG built in Java, designed to apply and demonstrate fundamental Object-Oriented Programming (OOP) concepts through an interactive and modular structure. The player answers multiple-choice questions drawn from knowledge categories — **Computer Science**, **Philosophy**, and **Artificial Intelligence (to be expanded in future iterationsany )** — in a turn-based combat format. Each correct answer deals damage to an enemy, while wrong answers reduce the player’s HP. The session ends when the player either clears all questions or their HP drops to zero.

This document serves as the **technical project specification** for development purposes. It defines the structure, flow, and system behavior in a clear, implementation-oriented manner, ensuring minimal refactoring as the project scales.

---

## 2. Core Objectives

- Create a **console-based trivia RPG** using Java OOP concepts.
- Emphasize **Encapsulation**, **Inheritance**, **Polymorphism**, and **Abstraction**.
- Design for **clarity and modularity** — separate question data from game logic.
- Keep the architecture open for future integration (e.g., file I/O, APIs, GUI).

---

## 3. Game Structure & Flow

### Final Clarifications (Post-Planning Decisions)

- **Unified Main Loop:** The game operates under a single `while(true)` loop handling both menu navigation and gameplay. Transitions between views (menu, round, results) clear the console for smooth flow.
- **Session Start:** A "session" refers to the entire duration the Java program is running. Players can type `exit game` (case-sensitive) to quit, followed by a confirmation prompt. When the user kills the terminal, the session ends, and all in-memory data is reset.
- **Per-Round Reset:** At the start of each new round (a single playthrough of a chosen topic and difficulty set), the player's HP, hint count, and the set of used question IDs for that round will be reset.
- **Global Points:** The `global points` are the *only* state that persists and accumulates *across multiple rounds within the same session*. These points are only increased (never decreased) by successfully completing rounds.
- **Menu Navigation:** One continuous control loop manages main menu and question flow. Each screen transition triggers a console clear to simulate navigation.
- **Final Chance Behavior:** When HP hits 0, a message displays loss and triggers a countdown timer before launching one random Hard question. If playing a single topic, the question will be from that topic. In Mixed Mode, it will be a random Hard question from any available topic. Correct answer restores +30 HP (max HP = 100); otherwise, Game Over follows.
- **HP & Difficulty Relationship:** Difficulty affects HP deduction amounts, not fundamental gameplay complexity. Future iterations may expand this mechanic.
- **Game Over Flow:** After losing or finishing a session, the game pauses with a "Press Enter to continue…" prompt before returning to the main menu.
- **Replay Logic:** Replay continues with the same topic and difficulty for convenience. Returning to the main menu fully resets the session state.

---

## 4. System Components (MVC-Aligned)

### 4.1 Model Layer

Handles **data** and **game state**.

- **Player Class:** HP, score, hint count, statistics.
- **Enemy Class:** Represents opponent (visual metaphor for quiz challenge).
- **Question Class:** Base class for all questions.
  - Fields: `questionText`, `choices[]`, `correctAnswer`, `difficulty`, `topic`, `id`.
  - Subclasses: `EasyQuestion`, `MediumQuestion`, `HardQuestion` (inherit and override).
- **QuestionBank:** Holds categorized lists of questions (modifiable for scalability).
- **GameState:** Tracks current session status — active question index, HP, score, and question history.

### 4.2 View Layer

Handles **console output** and user-facing presentation.

- Displays formatted text (menus, feedback, results).
- Clears console between screens for immersion.
- May later integrate with ASCII art or colored text.

### 4.3 Controller Layer

Handles **game logic and input flow.**

- **GameController:** Manages transitions between menus, question flow, and end states.
- **InputHandler:** Validates and sanitizes user input.
- **SessionManager:** Initializes and resets new sessions.

---

### 4.4 Question System Decisions (Finalized)

**Question Bank Structure**

- We will use **separate lists for each topic × difficulty** (Option B). Example: `csEasy`, `csMedium`, `csHard`, `philosophyEasy`, etc. This keeps loading and retrieval by topic/difficulty straightforward.

**Question Count & Scaling**

- Initial content plan: **5 questions per difficulty per topic** (Easy, Medium, Hard) → **15 questions per topic**. Topics to seed first: **Computer Science** and **Philosophy**. The system will be designed so adding AI questions is trivial.

**Unique Identification**

- Every `Question` will have a unique ID string in the format: `TOPIC_DIFFICULTY_###` (e.g., `CS_EASY_001`). The `###` portion will be a sequential counter padded with leading zeros, unique per topic and difficulty combination. This ID is used for tracking which questions were already shown in a session and for referencing questions later.

**Answer Representation**

- Each `Question` will store its `choices` as an `ArrayList<String>` and a `correctIndex` (int) pointing to the correct choice. Using a list makes it easy to shuffle choices and remove incorrect options for hints.

**Randomization Strategy**

- **Question order** is shuffled per session (we pick 3 questions per difficulty, 9 total). For **Mixed Mode**, the session pulls an equal number of questions from each selected topic: e.g., if Mixed Mode has 9 slots and three topics are enabled, pull 3 questions per topic across difficulty distribution.
- **Answer choices** are shuffled at display time inside the `Question` instance. After shuffling choices, the `correctIndex` is updated accordingly to keep the correct mapping.

**Question Loading API**

- `QuestionBank` exposes methods like `loadDefaultQuestions()` and `getQuestionsByTopicAndDifficulty(topic, difficulty)` so that the data source can later be swapped with a file or API loader without changing controllers.

**Hint Compatibility**

- `Question` will expose a helper method `removeIncorrectOptions(int removeCount)` which returns a reduced list of choices for display (used by the 50-50 hint mechanic). This keeps hint logic encapsulated in the model.

**Session-level Tracking**

- `GameState` or `SessionManager` will maintain a `Set<String>` of question IDs already used in the current session to prevent repeats. Questions selected for the session are sampled from the shuffled lists and then locked for the session.

---

## 5. Core Mechanics

### 5.1 Combat and Scoring Mechanics (Finalized)

**HP Penalties and Difficulty Scaling**

- Incorrect answers penalize HP based on difficulty:
  - Easy → −25 HP
  - Medium → −15 HP
  - Hard → −10 HP
- This reflects the idea that missing an easy question is a heavier mistake.

**Enemy and Round Representation**

- Each **round** represents a battle with an abstract enemy (the topic itself). The enemy has no mechanical HP; its defeat is symbolic—clearing all questions in the round means victory.
- Future expansions may treat enemies as entities with attack patterns or attributes, but for now, this remains aesthetic only.

**Damage and Scoring Formula**

- Player damage taken = penalty per difficulty as above.
- Correct answers add to the score using a baseline multiplier:
  - Easy = +10 points
  - Medium = +20 points
  - Hard = +30 points
- System is designed to allow future **damage multipliers** or bonus modifiers for special mechanics (e.g., power-ups, streaks) with minimal changes.

**Streak Bonus (Deferred Feature)**

- Combo or streak scoring is not implemented for the prototype but can be added later. Design remains open for integrating consecutive-answer tracking.

**HP–Score Relationship**

- End-of-round score includes a **Perfect HP Bonus** proportional to remaining HP:
  - Final Score = Base Score + (HP × 0.5)
  - Encourages accurate and consistent answering across the session.

**Final Chance Reward**

- If the player succeeds in the Final Chance question, they regain **+30 HP** (up to max 100). No extra score bonus is given.

**Round Completion**

- A round is cleared when all assigned questions are answered or the player loses all HP. Winning the round displays a victory summary; losing transitions to the Final Chance state.

---

## 5.2 Hint System & Lifelines (Finalized)

**Hint Availability**

- Each player starts with **2 hints per session**. Hints reset when starting a new session (topic run). Total hints persist only within the active session.

**Hint Activation**

- A command link labeled **“Type HINT to use your hint”** appears beneath every question display.
- Upon typing `HINT`, the system asks for confirmation (“Would you like to use your HINT? (Y/N)”). If confirmed, a hint is consumed.

**Hint Scope & Effect**

- Each question always has four answer choices (one correct, three incorrect). A used hint eliminates **two random incorrect options**, leaving the correct answer and one incorrect.

**Display Behavior**

- After applying a hint, the console **reprints the question** with the remaining valid options visible. Eliminated options are no longer selectable.

**Hint Limit Enforcement**

- If the player attempts to use a hint after all have been spent, the system displays “No hints remaining!” and reprompts for the answer normally.

**Final Chance Interaction**

- Hints are **disabled** during the Final Chance state. Any attempt to type `HINT` displays a message: “Hints unavailable during Final Chance.” The question immediately reprints afterward.

**UI Feedback**

- Each question display includes a concise HUD line showing current stats, e.g.:

  ```
  HP: 85 | Hints: 1 | Score: 120
  ```

  This line updates dynamically after every action to reflect player status.

---

## 6. Data Handling

- **Storage:** Hardcoded question data in a dedicated file (e.g., `QuestionBank.java`).
- **Extensibility:** Future updates can load questions from a text or JSON file.
- **Optional Integration:** AI-generated question expansion (e.g., Gemini API) through modular controller injection.

### 6.1.  Console & Input Management (Implementation Notes)

### **1. Console Clearing Strategy**

To keep the console readable between screens and transitions:

* **Baseline Approach (for development):**

  Use `System.out.println("\n".repeat(50));` to simulate a cleared console.

  Works across all environments with minimal setup.
* **Future-Proof Approach (for polish phase):**

  Implement a small helper in a separate class, `ConsoleUtils.clearScreen()`, to perform an actual system clear. This will be implemented from the start.

  ```java
  public class ConsoleUtils {
      public static void clearScreen() {
          try {
              final String os = System.getProperty("os.name");
              if (os.contains("Windows")) {
                  new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
              } else {
                  Runtime.getRuntime().exec("clear");
              }
          } catch (final Exception e) {
              System.out.println("\n".repeat(50)); // fallback
          }
      }
  }
  ```

  This utility will detect the operating system (Windows, Linux, macOS) and execute the appropriate terminal command (`cls` or `clear`). A fallback to printing 50 newlines will be used if the OS cannot be detected or the command fails.

---

### **2. Input Handling**

* Maintain **one global `Scanner` instance** shared across the game (e.g., via `InputHandler` or `GameController`), to prevent input buffering issues.
* All player input prompts run inside **validation loops** until valid input is received.
* On invalid input:
  * Display a specific error message (e.g., “Invalid choice.”, “Input out of range.”).
  * Pause briefly with `Thread.sleep(1000);` so the player can read the message.
  * The game will then return to the Main Menu.
* Wrap all input in `try / catch (InputMismatchException e)` to prevent crashes and handle non-numeric input gracefully.

---

### **3. Timing and Pauses**

* Use `Thread.sleep(milliseconds)` for short immersive pauses, such as:
  * “Loading next question…”
  * “Invalid input, try again…”
* Avoid full countdown timers for now; they require multi-threaded, non-blocking input handling which adds unnecessary complexity for a console prototype.
* Timed mechanics may be revisited once a GUI version is developed.

---

### **4. Error & Debug Handling**

* Non-fatal exceptions log an error message and safely return to the Main Menu.
* Unexpected EOF or stream closures trigger a graceful shutdown with a short notice.
* Introduce a **DEBUG flag** in a dedicated `GameConfig.java` class to toggle developer output:
  * When `GameConfig.DEBUG` is `true` → Print internal events (selected question ID, correct answer index, etc.) to facilitate testing.
  * When `GameConfig.DEBUG` is `false` → Suppress diagnostic messages for a clean player experience.

---

### **5. Future Enhancements**

* Consider adding colored console output for aesthetic polish once the core gameplay loop stabilizes.
* If IDE-specific console issues occur, encapsulate all text printing in a `GameUI` class so display tweaks remain isolated from game logic.
* Optional: add a Debug Mode menu option to toggle runtime debugging without recompilation.

---

## 7. Implementation Plan

### 7.1 Folder Structure

```
MindQuest/
├── src/
│   ├── model/
│   │   ├── Player.java
│   │   ├── Enemy.java
│   │   ├── Question.java
│   │   ├── EasyQuestion.java
│   │   ├── MediumQuestion.java
│   │   ├── HardQuestion.java
│   │   └── QuestionBank.java
│   ├── controller/
│   │   ├── GameController.java
│   │   ├── InputHandler.java
│   │   └── SessionManager.java
│   ├── view/
│   │   └── ConsoleUI.java
│   └── Main.java
└── README.md
```

### 7.2 Development Phases

1. **Phase 1:** Core gameplay logic (questions, HP, scoring, replay).
2. **Phase 2:** Difficulty and hint mechanics.
3. **Phase 3:** Session tracking and modularization.
4. **Phase 4:** Console cleanup, visual polish, and documentation.

---

## 8. OOP Principles in Practice

| Principle               | Application                                                                                                     |
| ----------------------- | --------------------------------------------------------------------------------------------------------------- |
| **Encapsulation** | Private fields with getters/setters in Player, Question, and GameState classes.                                 |
| **Inheritance**   | Specialized question types (`EasyQuestion`, `MediumQuestion`, `HardQuestion`) extend base Question class. |
| **Polymorphism**  | Method overriding for scoring and damage calculation based on difficulty.                                       |
| **Abstraction**   | Abstract base classes/interfaces define shared behaviors for extensibility.                                     |

---

## 9. Future-Proofing Notes

- Question bank structured as a **swappable module**, allowing later integration with APIs or external files.
- Controller designed to accommodate **state transitions** cleanly (no hardcoded logic chains).
- Architecture enables **UI replacement** (e.g., JavaFX or web-based view) with minimal refactoring.
