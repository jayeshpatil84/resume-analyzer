# AI Resume Analyzer

AI Resume Analyzer is a Spring Boot application that analyzes a resume against a job description and provides useful feedback about the candidate's profile.

The application accepts a PDF resume and a job description, then generates a detailed analysis including **job description match score, skill gaps, ATS compatibility, spelling errors, and improvement recommendations**.

---

## ✨ Features

- **Job Description Match Score**  
  Compares the skills found in the resume with the skills required in the job description.

- **Skill Gap Analysis**  
  Displays matched skills, missing skills, and additional skills found in the resume.

- **ATS Compatibility Check**  
  Performs a multi-point review of the resume, including contact information, section headings, dates, keywords, action verbs, and measurable achievements.

- **Spell Checker**  
  Detects spelling and language errors in the resume using LanguageTool.

- **Smart Recommendations**  
  Provides suggestions based on the resume score, missing skills, and detected issues.

- **Analysis History**  
  Previous resume analyses are stored in MySQL and can be viewed from the history page.

- **Dual Skill Extraction Mode**  
  The application can use the built-in skill keyword dataset by default or an optional OpenNLP NER model for more advanced skill extraction.

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2 |
| Web Interface | Thymeleaf, HTML, CSS, JavaScript |
| Database | MySQL 8+ |
| ORM | Spring Data JPA / Hibernate |
| PDF Processing | Apache PDFBox 3.0.1 |
| NLP | Apache OpenNLP 2.3.1 |
| Spell Checking | LanguageTool 6.3 |
| Build Tool | Maven 3.8+ |

---

## 📁 Project Structure

```text
resume-analyzer/
│
├── src/
│   └── main/
│       ├── java/com/resumeai/
│       │   ├── ResumeAnalyzerApplication.java
│       │   │
│       │   ├── controller/
│       │   │   ├── ResumeAnalyzerController.java
│       │   │   └── WebController.java
│       │   │
│       │   ├── service/
│       │   │   ├── AnalyzerService.java
│       │   │   ├── PdfExtractorService.java
│       │   │   ├── NlpService.java
│       │   │   ├── SkillDatasetLoader.java
│       │   │   ├── ScoringService.java
│       │   │   ├── AtsService.java
│       │   │   └── SpellCheckService.java
│       │   │
│       │   ├── trainer/
│       │   │   └── ModelTrainer.java
│       │   │
│       │   ├── model/
│       │   │   └── AnalysisResult.java
│       │   │
│       │   ├── dto/
│       │   │   ├── AnalysisResponse.java
│       │   │   ├── AtsResult.java
│       │   │   └── SpellError.java
│       │   │
│       │   └── repository/
│       │       └── AnalysisResultRepository.java
│       │
│       └── resources/
│           ├── application.properties
│           │
│           ├── dataset/
│           │   └── skills.txt
│           │
│           ├── opennlp-models/
│           │   ├── training-data.txt
│           │   └── PUT_MODEL_HERE.txt
│           │
│           └── templates/
│               ├── index.html
│               └── history.html
│
├── pom.xml
└── README.md
```

---

# ⚙️ Project Setup

Follow the steps below to run the project on your local machine.

## 1. Prerequisites

Install the following software before starting:

| Software | Required Version | Check Installation |
|---|---|---|
| Java JDK | 17 or higher | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| MySQL | 8.0+ | `mysql --version` |
| Git | Latest recommended | `git --version` |

---

## 2. Clone the Repository

Open Command Prompt, PowerShell, or a terminal and run:

```bash
git clone https://github.com/your-username/resume-analyzer.git
```

Move into the project directory:

```bash
cd resume-analyzer
```

Replace the repository URL with your actual GitHub repository URL.

---

## 3. Create the MySQL Database

Start MySQL and log in:

```bash
mysql -u root -p
```

Create the database:

```sql
CREATE DATABASE IF NOT EXISTS resume_analyzer;
```

Then exit MySQL:

```sql
EXIT;
```

You only need to create the database. The application will create the required table automatically when it starts because Hibernate is configured to update the database schema.

---

## 4. Configure Database Credentials

Open:

```text
src/main/resources/application.properties
```

Find the MySQL configuration and update it with your MySQL username and password.

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/resume_analyzer?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

Replace:

```text
YOUR_MYSQL_PASSWORD
```

with your actual MySQL password.

### Important

Do not upload your real database password to GitHub.

For local development, you can keep sensitive configuration in a local properties file that is excluded from Git.

---

## 5. Build the Project

From the project root directory, run:

```bash
mvn clean install
```

To skip tests during the build:

```bash
mvn clean install -DskipTests
```

Maven will download the required dependencies automatically.

The first build may take longer because libraries such as LanguageTool and other NLP dependencies need to be downloaded.

---

## 6. Start the Application

Run:

```bash
mvn spring-boot:run
```

The application should start on:

```text
http://localhost:8080
```

You can also run the generated JAR file:

```bash
java -jar target/resume-analyzer-2.0.0.jar
```

---

## 7. Verify the Application

After starting the application, open the following URLs in your browser:

| Page | URL |
|---|---|
| Resume Analyzer | http://localhost:8080 |
| Analysis History | http://localhost:8080/history |
| API Health | http://localhost:8080/api/health |

The health endpoint should return a response similar to:

```json
{
  "status": "UP",
  "service": "AI Resume Analyzer v2"
}
```

---

# ▶️ How to Use the Application

Once the application is running:

### Step 1 — Open the Analyzer

Go to:

```text
http://localhost:8080
```

### Step 2 — Upload Your Resume

Upload your resume in **PDF format**.

Maximum supported file size:

```text
10 MB
```

### Step 3 — Enter the Job Description

Paste the complete job description into the job description field.

You can also provide the job title.

### Step 4 — Analyze the Resume

Click the analyze button.

The application will:

1. Extract text from the PDF.
2. Identify skills from the resume.
3. Analyze the job description.
4. Calculate the skill match score.
5. Identify missing and matched skills.
6. Perform the ATS compatibility check.
7. Check spelling and language issues.
8. Generate recommendations.
9. Save the result in MySQL.

### Step 5 — View Previous Analyses

Open:

```text
http://localhost:8080/history
```

This page displays previously saved analyses.

---

# 📊 Analysis Process

The application follows this general workflow:

```text
Resume PDF
     │
     ▼
PDF Text Extraction
     │
     ▼
Skill Extraction
     │
     ▼
Job Description Analysis
     │
     ├── Skill Matching
     ├── Skill Gap Analysis
     ├── ATS Check
     └── Spell Check
     │
     ▼
Score + Recommendations
     │
     ▼
Save Result in MySQL
```

---

# 🧠 Skill Extraction

The application supports two approaches for identifying skills.

## Default Mode — Dataset Matching

By default, the application loads skills from:

```text
src/main/resources/dataset/skills.txt
```

The dataset contains a large collection of technology and professional skill keywords.

This mode requires no additional model setup.

---

## Optional Mode — OpenNLP NER

For more advanced skill extraction, an OpenNLP Named Entity Recognition model can be trained.

The model files are located in:

```text
src/main/resources/opennlp-models/
```

The training data is stored in:

```text
training-data.txt
```

The generated model is:

```text
en-ner-skills.bin
```

The application can use this model instead of the default dataset-based matching.

---

# 🤖 Train the OpenNLP Model

Training the OpenNLP model is optional.

The application works without it.

## Step 1 — Add the Maven Exec Plugin

Add the following plugin inside the `<plugins>` section of `pom.xml`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
</plugin>
```

## Step 2 — Compile the Project

```bash
mvn compile
```

## Step 3 — Train the Model

Run:

```bash
mvn exec:java \
  -Dexec.mainClass="com.resumeai.trainer.ModelTrainer" \
  -Dexec.args="src/main/resources/opennlp-models/training-data.txt src/main/resources/opennlp-models/en-ner-skills.bin"
```

On Windows PowerShell, you can also run the command as a single line:

```powershell
mvn exec:java -Dexec.mainClass="com.resumeai.trainer.ModelTrainer" -Dexec.args="src/main/resources/opennlp-models/training-data.txt src/main/resources/opennlp-models/en-ner-skills.bin"
```

The model will be generated at:

```text
src/main/resources/opennlp-models/en-ner-skills.bin
```

## Step 4 — Restart the Application

```bash
mvn spring-boot:run
```

The application should then detect the trained model during startup.

---

# 📡 API Reference

## Analyze Resume

### `POST /api/analyze`

Analyzes a PDF resume against a job description.

### Request Fields

| Field | Type | Required | Description |
|---|---|---|---|
| `resume` | PDF File | Yes | Resume PDF, maximum 10 MB |
| `jobDescription` | String | Yes | Job description |
| `jobTitle` | String | No | Job title |

### Example

```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "resume=@/path/to/resume.pdf" \
  -F "jobDescription=We need a Java backend engineer with Spring Boot and MySQL..." \
  -F "jobTitle=Backend Engineer"
```

---

## Analysis History

### `GET /api/history`

Returns previously saved resume analyses.

---

## Health Check

### `GET /api/health`

Checks whether the application is running correctly.

Example response:

```json
{
  "status": "UP",
  "service": "AI Resume Analyzer v2"
}
```

---

# ⚙️ Configuration

The main configuration file is:

```text
src/main/resources/application.properties
```

Example:

```properties
# Application port
server.port=8080

# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/resume_analyzer?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=yourpassword

# Hibernate
spring.jpa.hibernate.ddl-auto=update

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Skills dataset
app.dataset.skills-path=dataset/skills.txt
```

Replace the database password with your local MySQL password.

---

# 🔧 Troubleshooting

| Problem | Possible Cause | Solution |
|---|---|---|
| MySQL connection error | MySQL is not running | Start the MySQL service |
| Access denied for MySQL user | Incorrect credentials | Check `spring.datasource.username` and `spring.datasource.password` |
| Port 8080 already in use | Another application is using port 8080 | Change `server.port` |
| Unsupported class version | Incorrect Java version | Install Java 17 or higher |
| Slow first startup | LanguageTool is loading resources | Wait for the initial startup to finish |
| Model not found | NER model has not been trained | Use default dataset mode or train the model |
| Maven command fails | Maven is not installed/configured | Verify with `mvn -version` |

### Change the Application Port

If port `8080` is already being used, change:

```properties
server.port=8080
```

to:

```properties
server.port=9090
```

Then open:

```text
http://localhost:9090
```

---

# 🔐 Security Notes

Keep sensitive configuration such as database passwords outside the public repository.

Never commit:

```text
Database passwords
API keys
Private credentials
Generated secret keys
```

For a public GitHub repository, use environment-specific configuration for sensitive values.

---

# 🤝 Contributing

Contributions and improvements are welcome.

To contribute:

```bash
git checkout -b feature/your-feature
```

Make your changes and commit them:

```bash
git add .
git commit -m "Add your feature"
```

Push the branch:

```bash
git push origin feature/your-feature
```

Then open a Pull Request on GitHub.

Before submitting changes, make sure the project builds successfully:

```bash
mvn clean install
```

---

# 📄 License

This project is licensed under the MIT License.

---

# 🙏 Acknowledgements

This project uses the following open-source technologies:

- Apache PDFBox — PDF text extraction
- Apache OpenNLP — Natural language processing and NER
- LanguageTool — Spell and grammar checking
- Spring Boot — Backend application framework
- Spring Data JPA — Database access
- Hibernate — ORM
- Project Lombok — Boilerplate code reduction