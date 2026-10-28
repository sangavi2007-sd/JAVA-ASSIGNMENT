package edu.kec.academic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StudentDAO {
    public void addStudent(String rollNo, String name, String department, int semester) throws SQLException {
        validateText(rollNo, "Roll number");
        validateText(name, "Student name");
        validateText(department, "Department");
        validateSemester(semester);
        if (studentExists(rollNo)) {
            throw new IllegalArgumentException("Student " + rollNo.trim().toUpperCase()
                    + " is already registered. Use option 11 to update the existing student.");
        }
        String sql = "INSERT INTO students (roll_no, name, department, current_semester) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rollNo.trim().toUpperCase());
            statement.setString(2, name.trim());
            statement.setString(3, department.trim().toUpperCase());
            statement.setInt(4, semester);
            statement.executeUpdate();
        }
    }

    public void updateStudent(String rollNo, String name, String department, int semester) throws SQLException {
        validateText(rollNo, "Roll number");
        validateText(name, "Student name");
        validateText(department, "Department");
        validateSemester(semester);
        String sql = "UPDATE students SET name = ?, department = ?, current_semester = ? WHERE roll_no = ?";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            statement.setString(2, department.trim().toUpperCase());
            statement.setInt(3, semester);
            statement.setString(4, rollNo.trim().toUpperCase());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Student " + rollNo.trim().toUpperCase()
                        + " was not found. Add the student first using option 1.");
            }
        }
    }

    public void addCourse(String courseCode, String title, int credits, String courseType) throws SQLException {
        validateText(courseCode, "Course code");
        validateText(title, "Course title");
        if (credits < 1 || credits > 30) {
            throw new IllegalArgumentException("Credits must be between 1 and 30.");
        }
        String normalizedType = courseType.trim().toUpperCase();
        if (!normalizedType.equals("THEORY") && !normalizedType.equals("PRACTICAL")
                && !normalizedType.equals("EMBEDDED")) {
            throw new IllegalArgumentException("Course type must be THEORY, PRACTICAL, or EMBEDDED.");
        }
        if (courseExists(courseCode)) {
            throw new IllegalArgumentException("Course " + courseCode.trim().toUpperCase()
                    + " already exists. The existing course was not changed.");
        }
        String sql = "INSERT INTO courses (course_code, title, credits, course_type) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseCode.trim().toUpperCase());
            statement.setString(2, title.trim());
            statement.setInt(3, credits);
            statement.setString(4, normalizedType);
            statement.executeUpdate();
        }
    }

    private boolean studentExists(String rollNo) throws SQLException {
        return exists("SELECT 1 FROM students WHERE roll_no = ?", rollNo.trim().toUpperCase());
    }

    private boolean courseExists(String courseCode) throws SQLException {
        return exists("SELECT 1 FROM courses WHERE course_code = ?", courseCode.trim().toUpperCase());
    }

    private boolean exists(String sql, String key) throws SQLException {
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void validateText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be empty.");
        }
    }

    private static void validateSemester(int semester) {
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
    }
}
