# KEC R2024 Student Academic & Continuous Evaluation System

A menu-driven Java 11+ JDBC application using SQLite. The program creates its database (`kec_academic.db`) in the current working directory on first launch.

## Requirements

- JDK 11 or later
- Apache Maven 3.8+ (optional if using `run.bat`)

Maven downloads the SQLite JDBC driver and its logging dependency. The Windows launcher can also compile and run with a JDK alone; if Maven is missing, it downloads these dependencies on first launch, so an internet connection is needed for that first run.

## Run

Open a terminal in the extracted project directory:

```text
mvn clean compile exec:java
```

On Windows, run `run.bat`. It uses Maven when available, or falls back to `javac` and downloads the SQLite driver when Maven is not installed.

An alternate JDBC URL may be configured with the `KEC_DB_URL` environment variable. The default is `jdbc:sqlite:kec_academic.db`.

The schema is in `kec_schema.sql`; application startup applies it automatically. Database tables are created if absent, so existing records are retained on subsequent launches.

## Menu workflow

The top-level menu follows the assignment PDF:

1. **Student Management** — register a student or update an existing student's details/current semester. Roll numbers are unique; do not register an existing student again.
2. **Course Catalog** — add a course with code, title, credits, and type.
3. **Course Enrollment & Attendance Tracking** — use the submenu to enroll, record/update classes held and attended, or record the medical-condonation decision.
4. **Enter Marks** — enter the roll number, course code, and semester, then choose one assessment phase:
   - **Phase 1:** enter CIA Test 1 and CIA Test 2 as two separate numeric inputs.
   - **Phase 2:** enter one assignment/quiz/lab evaluation mark.
   - **Phase 3:** record an ESE score or mark the student absent.
5. **Student Semester Mark Sheet** — generate the course-level results and SGPA.
6. **Attendance Defaulter Report** — list enrollments below 65% with `SA`.
7. **Department-wise Class Performance & Result Summary** — show pass/fail counts, pass percentage, incomplete students, and average SGPA.

Enter one menu choice at each prompt and press Enter. For Phase 1, for example, type `1`, Enter, then type the CIA1 mark, Enter, then the CIA2 mark, Enter. Do not enter both marks together as `45/48`.

For enrollment, attendance, marks, and condonation, enter the exact registered roll number, course code, and semester. For option 3, open the relevant submenu and enroll the student in the course before entering attendance, marks, or condonation. Condonation applies only to attendance from 65% to below 75%; students below 65% are barred from ESE.

Updating a student's current semester does not change existing course enrollments or assessment history. A duplicate course enrollment is rejected rather than overwritten.

### Example data-entry sequence

Create:

- Student: `24ITR001`, `Aarav Kumar`, `IT`, semester `3`
- Course: `24ITR31`, `Data Structures`, `4` credits, `THEORY`
- Enrollment: `24ITR001`, `24ITR31`, semester `3`
- Attendance: `40` held, `34` attended (85%)
- CIA1: `80`, CIA2: `84`, COURSEWORK: `90`
- ESE: `76`

The internal score is the arithmetic mean of CIA1, CIA2, and coursework. This is then weighted using the course-type CIA/ESE percentages. The example total is `(80 + 84 + 90) / 3 * 0.40 + 76 * 0.60 = 78.53`; the grade is `A`, with 8 grade points.

## Evaluation behavior

- **THEORY:** 40% CIA, 60% ESE; ESE minimum 45/100; overall minimum 50.
- **PRACTICAL:** 60% CIA, 40% ESE; ESE minimum 50/100; overall minimum 50.
- **EMBEDDED:** 50% CIA, 50% ESE; ESE minimum 45/100 (22.5/50); overall minimum 50.
- Attendance below 65% results in `SA` and blocks ESE entry. Attendance from 65% to below 75% requires recorded medical-condonation approval to enter ESE. At 75% or above, ESE entry is allowed.
- An absent ESE receives `AB`; a failed ESE threshold or overall score receives `RA`.
- Passing score grades: `O` 91–100, `A+` 81–<91, `A` 71–<81, `B+` 61–<71, and `B` 50–<61. Grade points are 10, 9, 8, 7, and 6 respectively. `RA`, `SA`, and `AB` are 0.
- The CIA score is the mean of the three raw CIA components. The request specifies their input range and course weightage but does not state a separate CIA aggregation formula; this implementation uses an arithmetic mean and exposes that assumption here.
- SGPA is credit-weighted. A marksheet displays SGPA as `N/A` until every enrolled course has a completed result. The department summary reports pass/fail and average SGPA for completed students, and separately counts incomplete records.

## Reports and screenshot evidence

The console reports include the student's CIA/ESE marks, weighted total, grade/status, SGPA and equivalent percentage; students below 65% attendance are listed with `SA`; and department summaries include pass/fail/incomplete counts and average SGPA.

No screenshots are included because they must be captured from a real application run. To produce submission evidence, run the application and capture your terminal after:

1. Adding a student and a course.
2. Enrolling the student and entering CIA1, CIA2, coursework, and ESE in separate menu operations.
3. Generating the student's marksheet showing the calculated weighted score and SGPA.

## Project files

- `kec_schema.sql` — SQLite schema with checks, primary keys, and foreign keys.
- `src/main/java/edu/kec/academic/DBConnection.java` — JDBC connection and schema initialization.
- `src/main/java/edu/kec/academic/StudentDAO.java` — student and course inserts.
- `src/main/java/edu/kec/academic/AcademicDAO.java` — enrollment, attendance, marks, evaluation, and reports.
- `src/main/java/edu/kec/academic/MainApp.java` — menu-driven interface.
# JAVA-ASSIGNMENT
