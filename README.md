# 🪧 AndolonDesk

> **A protest & movement management desktop application built with JavaFX**

AndolonDesk (আন্দোলন-ডেস্ক) is a feature-rich desktop application designed to organize, coordinate, and manage protest movements. Built on JavaFX with a local SQLite database, it provides a secure and interactive platform for movement organizers.

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Running the Application](#-running-the-application)
- [Project Structure](#-project-structure)
- [Dependencies](#-dependencies)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

- 🔐 **Secure Authentication** — BCrypt-hashed password login system
- 🗺️ **Map Integration** — Interactive maps via MapJFX for tracking movement locations
- 🗃️ **Local Database** — Persistent data storage using SQLite (no internet required)
- 🎨 **Styled UI** — Custom CSS-themed JavaFX interface with HTML-rendered views
- 📋 **Protest Management** — Create, update, and manage protests/movements
- 🌐 **Embedded Web View** — In-app web rendering via JavaFX WebView

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| UI Framework | JavaFX 21 (FXML + Controls + WebView) |
| Database | SQLite (via `sqlite-jdbc`) |
| Map | MapJFX 3.1.0 |
| Security | BCrypt (`at.favre.lib`) |
| Build Tool | Apache Maven |

---

## ✅ Prerequisites

Before running this project, make sure you have the following installed:

- **Java JDK 17+** — [Download here](https://adoptium.net/)
- **Apache Maven 3.6+** — [Download here](https://maven.apache.org/download.cgi)
- Git

Verify your setup:
```bash
java -version
mvn -version
```

---

## 📦 Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Coder-85/AndolonDesk.git
   cd AndolonDesk
   ```

2. **Install dependencies:**
   ```bash
   mvn install
   ```

---

## ▶️ Running the Application

Use the JavaFX Maven plugin to launch the app:

```bash
mvn clean javafx:run
```

The main entry point is `org.amjonota.App`.

---

## 📁 Project Structure

```
AndolonDesk/
├── src/
│   └── main/
│       ├── java/
│       │   └── org/amjonota/        # Java source files
│       └── resources/               # FXML layouts, CSS stylesheets, HTML views
├── pom.xml                          # Maven build configuration
└── .gitignore
```

---

## 📚 Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `org.xerial:sqlite-jdbc` | 3.47.1.0 | Local SQLite database |
| `org.openjfx:javafx-controls` | 21 | UI controls |
| `org.openjfx:javafx-fxml` | 21 | FXML layout support |
| `org.openjfx:javafx-web` | 21 | Embedded web view |
| `at.favre.lib:bcrypt` | 0.10.2 | Password hashing |
| `com.sothawo:mapjfx` | 3.1.0 | Interactive map rendering |

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a new branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a Pull Request

---

## 📄 License

This project is open source. Please add a `LICENSE` file to the repository to specify terms of use.

---

> Made with ❤️ by [Coder-85](https://github.com/Coder-85) and [sifat5532](https://github.com/sifat5532)
