PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS students (
    roll_no TEXT PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    department TEXT NOT NULL CHECK (length(trim(department)) > 0),
    current_semester INTEGER NOT NULL CHECK (current_semester BETWEEN 1 AND 8)
);

CREATE TABLE IF NOT EXISTS courses (
    course_code TEXT PRIMARY KEY,
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    credits INTEGER NOT NULL CHECK (credits BETWEEN 1 AND 30),
    course_type TEXT NOT NULL CHECK (course_type IN ('THEORY', 'PRACTICAL', 'EMBEDDED'))
);

CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    roll_no TEXT NOT NULL REFERENCES students(roll_no) ON DELETE RESTRICT,
    course_code TEXT NOT NULL REFERENCES courses(course_code) ON DELETE RESTRICT,
    semester INTEGER NOT NULL CHECK (semester BETWEEN 1 AND 8),
    classes_held INTEGER NOT NULL DEFAULT 0 CHECK (classes_held >= 0),
    classes_attended INTEGER NOT NULL DEFAULT 0 CHECK (classes_attended >= 0 AND classes_attended <= classes_held),
    condonation_approved INTEGER NOT NULL DEFAULT 0 CHECK (condonation_approved IN (0, 1)),
    UNIQUE (roll_no, course_code, semester)
);

CREATE TABLE IF NOT EXISTS assessment_marks (
    enrollment_id INTEGER PRIMARY KEY REFERENCES enrollments(enrollment_id) ON DELETE CASCADE,
    cia_test1 REAL CHECK (cia_test1 BETWEEN 0 AND 100),
    cia_test2 REAL CHECK (cia_test2 BETWEEN 0 AND 100),
    coursework_score REAL CHECK (coursework_score BETWEEN 0 AND 100),
    ese_mark REAL CHECK (ese_mark BETWEEN 0 AND 100),
    ese_absent INTEGER NOT NULL DEFAULT 0 CHECK (ese_absent IN (0, 1)),
    CHECK (ese_absent = 0 OR ese_mark IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_enrollments_semester ON enrollments(semester);
CREATE INDEX IF NOT EXISTS idx_students_department ON students(department);
