# MindQuest

MindQuest is a text-based trivia RPG built in Java. This project demonstrates fundamental Object-Oriented Programming (OOP) concepts through an interactive and modular structure.

## Project Specification
For detailed technical specifications, please refer to [.github/projest_specification_MindQuest.md](.github/projest_specification_MindQuest.md).

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 11 or higher

### Building and Running
1. Clone the repository:
   ```bash
   git clone <repository_url>
   cd MindQuest
   ```
2. Compile the Java files:
   ```bash
   javac src/*.java src/model/*.java src/controller/*.java src/view/*.java
   ```
3. Run the application:
   ```bash
   java src.Main
   ```

## Folder Structure
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
│   ├── GameConfig.java
│   └── Main.java
└── README.md
```

## Development Phases
1. **Phase 1:** Core gameplay logic (questions, HP, scoring, replay).
2. **Phase 2:** Difficulty and hint mechanics.
3. **Phase 3:** Session tracking and modularization.
4. **Phase 4:** Console cleanup, visual polish, and documentation.

## Contributing
Contributions are welcome! Please refer to the project specification for guidelines.
