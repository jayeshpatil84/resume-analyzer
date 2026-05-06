# 📄 AI Resume Analyzer v2

An AI-powered resume analysis tool built with **Spring Boot + Java 17**. Upload a PDF resume, paste a job description, and instantly get a full breakdown — JD skill match score, ATS compatibility report, skill gap analysis, recommendations, and spell check.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![Maven](https://img.shields.io/badge/Maven-3.8+-red?logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## ✨ Features

- **JD Match Score** — Jaccard similarity between resume skills and the job description
- **Skill Gap Analysis** — Matched skills, missing skills, and extra skills shown side by side
- **ATS Compatibility Check** — 9-point audit covering contact info, section headers, dates, keywords, action verbs, metrics, and more
- **Spell Checker** — LanguageTool-powered with a tech term whitelist (AWS, Docker, Kubernetes, etc. are never flagged)
- **Smart Recommendations** — Tailored advice based on your score and missing skills
- **History Dashboard** — All past analyses saved to MySQL, viewable at `/history`
- **Dual NLP Mode** — Keyword dataset matching by default (zero setup), or optional OpenNLP NER model for higher accuracy

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2, Java 17 |
| Web UI | Thymeleaf, HTML / CSS / JS |
| Database | MySQL 8+ via Spring Data JPA |
| PDF Parsing | Apache PDFBox 3.0.1 |
| NLP / NER | Apache OpenNLP 2.3.1 |
| Spell Check | LanguageTool 6.3 |
| Build Tool | Maven 3.8+ |

---

## 📁 Project Structure

```
resume-analyzer/
├── src/
│   └── main/
│       ├── java/com/resumeai/
│       │   ├── ResumeAnalyzerApplication.java
│       │   ├── controller/
│       │   │   ├── ResumeAnalyzerController.java    # REST API — /api/analyze, /api/history
│       │   │   └── WebController.java               # Page routes — /, /history
│       │   ├── service/
│       │   │   ├── AnalyzerService.java             # Full pipeline orchestrator
│       │   │   ├── PdfExtractorService.java         # PDF → text via PDFBox 3.x
│       │   │   ├── NlpService.java                  # Skill extraction (NER or dataset)
│       │   │   ├── SkillDatasetLoader.java          # Loads skills.txt into memory
│       │   │   ├── ScoringService.java              # Jaccard match score + recommendations
│       │   │   ├── AtsService.java                  # 9-point ATS compatibility check
│       │   │   └── SpellCheckService.java           # Spell checking via LanguageTool
│       │   ├── trainer/
│       │   │   └── ModelTrainer.java                # OpenNLP NER model trainer (optional)
│       │   ├── model/
│       │   │   └── AnalysisResult.java              # JPA entity → MySQL table
│       │   ├── dto/
│       │   │   ├── AnalysisResponse.java
│       │   │   ├── AtsResult.java
│       │   │   └── SpellError.java
│       │   └── repository/
│       │       └── AnalysisResultRepository.java
│       └── resources/
│           ├── application.properties
│           ├── dataset/
│           │   └── skills.txt                       # 500+ skill keywords
│           ├── opennlp-models/
│           │   ├── training-data.txt                # Annotated NER training sentences
│           │   └── PUT_MODEL_HERE.txt               # Drop en-ner-skills.bin here after training
│           └── templates/
│               ├── index.html                       # Main analyzer UI
│               └── history.html                     # Analysis history page
├── pom.xml
└── README.md
```

---

## ⚙️ Prerequisites

| Tool | Minimum Version | Check Command |
|---|---|---|
| Java JDK | 17 | `java -version` |
| Maven | 3.8 | `mvn -version` |
| MySQL | 8.0 | `mysql --version` |

---

## 🚀 Setup & Installation

### 1. Clone the repository

```bash
git clone https://github.com/your-username/resume-analyzer.git
cd resume-analyzer
```

### 2. Create the MySQL database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS resume_analyzer;
EXIT;
```

> Spring Boot will auto-create the `analysis_results` table on first startup via `spring.jpa.hibernate.ddl-auto=update`. You only need to create the database itself.

### 3. Configure database credentials

Open `src/main/resources/application.properties` and update the following:

```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD_HERE
```

> **Tip:** For local development, you can also create `src/main/resources/application-local.properties` with your overrides — it is already in `.gitignore` and will never be committed.

### 4. Build the project

```bash
mvn clean install -DskipTests
```

> ⏳ First build may take **3–5 minutes** — Maven downloads LanguageTool's language models (~150 MB).

### 5. Run the application

```bash
mvn spring-boot:run
```

Or run the compiled JAR directly:

```bash
java -jar target/resume-analyzer-2.0.0.jar
```

### 6. Verify startup

On successful startup you will see:

```
Loaded 500+ skills from dataset: dataset/skills.txt
en-ner-skills.bin not found → using dataset keyword matching (fallback mode)
LanguageTool spell checker initialized
Tomcat started on port(s): 8080 (http)
```

### 7. Open the app

| Page | URL |
|---|---|
| Resume Analyzer | http://localhost:8080 |
| Analysis History | http://localhost:8080/history |
| API Health Check | http://localhost:8080/api/health |

---

## 📡 API Reference

### `POST /api/analyze`

Analyzes a PDF resume against a job description.

| Field | Type | Required | Description |
|---|---|---|---|
| `resume` | File (`.pdf`) | ✅ | PDF resume, max 10 MB |
| `jobDescription` | String | ✅ | Full job description text |
| `jobTitle` | String | ❌ | Defaults to `"Not Specified"` |

**Example (cURL):**

```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "resume=@/path/to/resume.pdf" \
  -F "jobDescription=We need a Java backend engineer with Spring Boot and Docker..." \
  -F "jobTitle=Backend Engineer"
```

### `GET /api/history`

Returns all past analyses sorted newest first.

### `GET /api/health`

Returns:
```json
{ "status": "UP", "service": "AI Resume Analyzer v2" }
```

---

## 🤖 Optional — Train the OpenNLP NER Model (Higher Accuracy)

By default, the app uses keyword matching from `dataset/skills.txt`. You can optionally train a custom NER model for higher accuracy skill extraction.

### Step 1 — Add the exec plugin to `pom.xml`

Inside the `<plugins>` block in `pom.xml`, add:
  
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    
</plugin>
```

### Step 2 — Train the model

```bash
mvn compile exec:java \
  -Dexec.mainClass="com.resumeai.trainer.ModelTrainer" \
  -Dexec.args="src/main/resources/opennlp-models/training-data.txt src/main/resources/opennlp-models/en-ner-skills.bin"
```

Training takes 1–5 minutes. The model is saved to `src/main/resources/opennlp-models/en-ner-skills.bin`.

> **Note:** `*.bin` files are in `.gitignore` — the trained model will not be committed.

### Step 3 — Restart the application

```bash
mvn spring-boot:run
```

On startup you should now see:

```
OpenNLP NER model loaded — AI mode active
```

### Training Data Format

Each line in `training-data.txt` is one annotated sentence using OpenNLP's span format:

```
Developed <START:skill> Spring Boot <END> microservices deployed to <START:skill> AWS <END>.
Expert in <START:skill> Python <END> and <START:skill> Machine Learning <END> with 3 years experience.
Managed <START:skill> Kubernetes <END> clusters on <START:skill> AWS EKS <END>.
```

**Tips for better accuracy:**
- Aim for at least 1,000 diverse sentences
- Cover all resume sections: Experience, Projects, Skills, Summary
- Include all skill categories: languages, frameworks, cloud, databases, tools
- Mirror real job descriptions from LinkedIn, Indeed, Naukri
- Entity type must always be `skill` (lowercase)

---

## 🔧 Troubleshooting

| Error | Likely Cause | Fix |
|---|---|---|
| `Communications link failure` | MySQL not running | `sudo systemctl start mysql` (Linux) / `brew services start mysql` (macOS) |
| `Access denied for user 'root'@'localhost'` | Wrong DB password | Update `spring.datasource.password` in `application.properties` |
| `Port 8080 already in use` | Another app on port 8080 | Add `server.port=9090` to `application.properties` |
| `UnsupportedClassVersionError` | Java version too old | Run `java -version` — must be 17 or higher |
| LanguageTool slow on first start | Loading language model (~150MB) | Normal — takes ~10 seconds on first run, instant after |
| `ClassNotFoundException: ModelTrainer` | exec plugin not in `pom.xml` | Add `exec-maven-plugin` (see NER section above) |
| `createDatabaseIfNotExist` ignored | MySQL user lacks CREATE privilege | Grant privileges or manually create the database |

---

## ⚙️ Configuration Reference

All settings live in `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# MySQL connection
spring.datasource.url=jdbc:mysql://localhost:3306/resume_analyzer?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=yourpassword

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Skills dataset path (relative to resources/)
app.dataset.skills-path=dataset/skills.txt
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push the branch: `git push origin feature/your-feature`
5. Open a Pull Request

Please make sure your code compiles (`mvn clean install`) before submitting.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 🙏 Acknowledgements

- [Apache PDFBox](https://pdfbox.apache.org/) — PDF text extraction
- [Apache OpenNLP](https://opennlp.apache.org/) — NER model training and inference
- [LanguageTool](https://languagetool.org/) — Open-source spell and grammar checking
- [Spring Boot](https://spring.io/projects/spring-boot) — Application framework
- [Project Lombok](https://projectlombok.org/) — Boilerplate reduction
