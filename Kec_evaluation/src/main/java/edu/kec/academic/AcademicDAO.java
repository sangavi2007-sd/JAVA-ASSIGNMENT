package edu.kec.academic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AcademicDAO {
    private static final String ENROLLMENT_SELECT =
            "SELECT e.enrollment_id, e.roll_no, s.name, s.department, e.course_code, c.title, c.credits, " +
            "c.course_type, e.semester, e.classes_held, e.classes_attended, e.condonation_approved, " +
            "m.cia_test1, m.cia_test2, m.coursework_score, m.ese_mark, m.ese_absent " +
            "FROM enrollments e JOIN students s ON s.roll_no = e.roll_no " +
            "JOIN courses c ON c.course_code = e.course_code " +
            "LEFT JOIN assessment_marks m ON m.enrollment_id = e.enrollment_id ";

    public void enroll(String rollNo, String courseCode, int semester) throws SQLException {
        requireText(rollNo, "Roll number");
        requireText(courseCode, "Course code");
        checkSemester(semester);
        if (!recordExists("SELECT 1 FROM students WHERE roll_no = ?", rollNo.trim().toUpperCase())) {
            throw new IllegalArgumentException("Student " + rollNo.trim().toUpperCase()
                    + " was not found. Add the student first using option 1.");
        }
        if (!recordExists("SELECT 1 FROM courses WHERE course_code = ?", courseCode.trim().toUpperCase())) {
            throw new IllegalArgumentException("Course " + courseCode.trim().toUpperCase()
                    + " was not found. Add the course first using option 2.");
        }
        if (enrollmentExists(rollNo, courseCode, semester)) {
            throw new IllegalArgumentException("This student is already enrolled in this course for semester "
                    + semester + ". No duplicate enrollment was created.");
        }
        String sql = "INSERT INTO enrollments (roll_no, course_code, semester) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rollNo.trim().toUpperCase());
            statement.setString(2, courseCode.trim().toUpperCase());
            statement.setInt(3, semester);
            statement.executeUpdate();
        }
    }

    public void recordAttendance(String rollNo, String courseCode, int semester, int held, int attended)
            throws SQLException {
        checkSemester(semester);
        if (held < 0) {
            throw new IllegalArgumentException("Classes held cannot be negative.");
        }
        if (attended < 0 || attended > held) {
            throw new IllegalArgumentException("Attendance must satisfy 0 <= attended <= classes held.");
        }
        String sql = "UPDATE enrollments SET classes_held = ?, classes_attended = ? " +
                "WHERE roll_no = ? AND course_code = ? AND semester = ?";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, held);
            statement.setInt(2, attended);
            statement.setString(3, rollNo.trim().toUpperCase());
            statement.setString(4, courseCode.trim().toUpperCase());
            statement.setInt(5, semester);
            requireUpdated(statement.executeUpdate(), "Enrollment not found.");
        }
    }

    public void approveCondonation(String rollNo, String courseCode, int semester, boolean approved)
            throws SQLException {
        checkSemester(semester);
        Enrollment enrollment = findEnrollment(rollNo, courseCode, semester);
        if (enrollment == null) {
            throw enrollmentNotFound(rollNo, courseCode, semester);
        }
        String sql = "UPDATE enrollments SET condonation_approved = ? WHERE enrollment_id = ?";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, approved ? 1 : 0);
            statement.setInt(2, enrollment.id);
            statement.executeUpdate();
        }
    }

    public void recordCiaMark(String rollNo, String courseCode, int semester, String assessment, double mark)
            throws SQLException {
        checkSemester(semester);
        checkMark(mark);
        String column;
        switch (assessment.trim().toUpperCase()) {
            case "CIA1":
                column = "cia_test1";
                break;
            case "CIA2":
                column = "cia_test2";
                break;
            case "COURSEWORK":
                column = "coursework_score";
                break;
            default:
                throw new IllegalArgumentException("Assessment must be CIA1, CIA2, or COURSEWORK.");
        }
        updateMark(rollNo, courseCode, semester, column, mark);
    }

    public void recordCiaMarks(String rollNo, String courseCode, int semester, double cia1, double cia2,
                               Double coursework) throws SQLException {
        checkSemester(semester);
        checkMark(cia1);
        checkMark(cia2);
        if (coursework != null) {
            checkMark(coursework);
        }
        Enrollment enrollment = findEnrollment(rollNo, courseCode, semester);
        if (enrollment == null) {
            throw enrollmentNotFound(rollNo, courseCode, semester);
        }
        String sql = "INSERT INTO assessment_marks (enrollment_id, cia_test1, cia_test2, coursework_score) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT(enrollment_id) DO UPDATE SET " +
                "cia_test1 = excluded.cia_test1, cia_test2 = excluded.cia_test2, " +
                "coursework_score = COALESCE(excluded.coursework_score, assessment_marks.coursework_score)";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enrollment.id);
            statement.setDouble(2, cia1);
            statement.setDouble(3, cia2);
            if (coursework == null) {
                statement.setNull(4, java.sql.Types.REAL);
            } else {
                statement.setDouble(4, coursework);
            }
            statement.executeUpdate();
        }
    }

    public void recordEseMark(String rollNo, String courseCode, int semester, Double mark, boolean absent)
            throws SQLException {
        checkSemester(semester);
        Enrollment enrollment = findEnrollment(rollNo, courseCode, semester);
        if (enrollment == null) {
            throw enrollmentNotFound(rollNo, courseCode, semester);
        }
        double attendance = enrollment.attendancePercentage();
        if (attendance < 65.0) {
            throw new IllegalArgumentException("ESE entry blocked: attendance is below 65%; student receives SA.");
        }
        if (attendance < 75.0 && !enrollment.condonationApproved) {
            throw new IllegalArgumentException("ESE entry blocked: medical condonation approval is required.");
        }
        if (absent && mark != null) {
            throw new IllegalArgumentException("An absent ESE must not have a numeric mark.");
        }
        if (!absent && mark == null) {
            throw new IllegalArgumentException("Provide an ESE mark or select absent.");
        }
        if (mark != null) {
            checkMark(mark);
        }
        String sql = "INSERT INTO assessment_marks (enrollment_id, ese_mark, ese_absent) " +
                "VALUES (?, ?, ?) ON CONFLICT(enrollment_id) DO UPDATE SET ese_mark = excluded.ese_mark, " +
                "ese_absent = excluded.ese_absent";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enrollment.id);
            if (mark == null) {
                statement.setNull(2, java.sql.Types.REAL);
            } else {
                statement.setDouble(2, mark);
            }
            statement.setInt(3, absent ? 1 : 0);
            statement.executeUpdate();
        }
    }

    public void printMarkSheet(String rollNo, int semester) throws SQLException {
        List<Enrollment> enrollments = findEnrollments(
                ENROLLMENT_SELECT + "WHERE e.roll_no = ? AND e.semester = ? ORDER BY e.course_code",
                rollNo.trim().toUpperCase(), semester);
        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException("No enrollments found for that roll number and semester.");
        }

        double weightedPoints = 0.0;
        int totalCredits = 0;
        boolean allComplete = true;
        System.out.printf("%nKEC AUTONOMOUS - REGULATION 2024%nSTUDENT MARK SHEET: %s | SEMESTER %d%n",
                rollNo.trim().toUpperCase(), semester);
        System.out.println("Course     Title                         CIA      ESE      Total    Grade  Status");
        System.out.println("--------------------------------------------------------------------------------");
        for (Enrollment enrollment : enrollments) {
            Result result = evaluate(enrollment);
            String ciaDisplay = result.ciaMean == null ? "-" : format(result.ciaMean);
            String eseDisplay = enrollment.eseAbsent ? "AB" :
                    enrollment.eseMark == null ? "-" : format(enrollment.eseMark);
            String totalDisplay = result.total == null ? "-" : format(result.total);
            System.out.printf("%-10s %-29.29s %-8s %-8s %-8s %-6s %s%n",
                    enrollment.courseCode, enrollment.title, ciaDisplay, eseDisplay, totalDisplay,
                    result.grade, result.status);
            if (result.gradePoint == null) {
                allComplete = false;
            } else {
                totalCredits += enrollment.credits;
                weightedPoints += enrollment.credits * result.gradePoint;
            }
        }
        if (!allComplete || totalCredits == 0) {
            System.out.println("\nSGPA: N/A (one or more course results are incomplete)");
            System.out.println("Equivalent percentage: N/A");
        } else {
            double sgpa = weightedPoints / totalCredits;
            System.out.printf("%nSGPA: %.2f%nEquivalent percentage: %.2f%%%n", sgpa, sgpa * 10.0);
        }
        System.out.println("CIA and ESE columns show raw marks out of 100; Total is the weighted score.");
    }

    public void printAttendanceDefaulters(int semester) throws SQLException {
        String sql = ENROLLMENT_SELECT + "WHERE e.semester = ? AND e.classes_held > 0 " +
                "ORDER BY s.department, e.roll_no, e.course_code";
        List<Enrollment> enrollments = findEnrollments(sql, semester);
        System.out.printf("%nATTENDANCE DEFAULTERS (<65%%): SEMESTER %d%n", semester);
        System.out.println("Roll No    Student                  Department  Course     Attendance  Grade");
        boolean found = false;
        for (Enrollment enrollment : enrollments) {
            if (enrollment.attendancePercentage() < 65.0) {
                found = true;
                System.out.printf("%-10s %-24.24s %-11s %-10s %8.2f%%   SA%n",
                        enrollment.rollNo, enrollment.studentName, enrollment.department,
                        enrollment.courseCode, enrollment.attendancePercentage());
            }
        }
        if (!found) {
            System.out.println("No attendance defaulters found.");
        }
    }

    public void printDepartmentSummary(int semester) throws SQLException {
        String sql = ENROLLMENT_SELECT + "WHERE e.semester = ? ORDER BY s.department, e.roll_no, e.course_code";
        List<Enrollment> enrollments = findEnrollments(sql, semester);
        Map<String, StudentSummary> byStudent = new LinkedHashMap<>();
        for (Enrollment enrollment : enrollments) {
            String key = enrollment.department + "|" + enrollment.rollNo;
            StudentSummary summary = byStudent.computeIfAbsent(key,
                    ignored -> new StudentSummary(enrollment.department));
            Result result = evaluate(enrollment);
            summary.courseCount++;
            if (result.gradePoint == null) {
                summary.complete = false;
                summary.passed = false;
            } else {
                summary.credits += enrollment.credits;
                summary.weightedPoints += enrollment.credits * result.gradePoint;
                if (result.gradePoint == 0) {
                    summary.passed = false;
                }
            }
        }
        Map<String, DepartmentSummary> departments = new LinkedHashMap<>();
        for (StudentSummary student : byStudent.values()) {
            DepartmentSummary department = departments.computeIfAbsent(student.department,
                    ignored -> new DepartmentSummary());
            if (student.complete && student.courseCount > 0) {
                if (student.passed) {
                    department.pass++;
                } else {
                    department.fail++;
                }
                if (student.credits > 0) {
                    department.sgpaTotal += student.weightedPoints / student.credits;
                    department.sgpaCount++;
                }
            } else {
                department.incomplete++;
            }
        }
        System.out.printf("%nDEPARTMENT RESULT SUMMARY: SEMESTER %d%n", semester);
        System.out.println("Department  Pass  Fail  Pass %   Incomplete  Average SGPA");
        for (Map.Entry<String, DepartmentSummary> entry : departments.entrySet()) {
            DepartmentSummary summary = entry.getValue();
            String average = summary.sgpaCount == 0 ? "N/A" :
                    String.format("%.2f", summary.sgpaTotal / summary.sgpaCount);
            int completedStudents = summary.pass + summary.fail;
            String passPercentage = completedStudents == 0 ? "N/A" :
                    String.format("%.2f%%", summary.pass * 100.0 / completedStudents);
            System.out.printf("%-11s %-5d %-5d %-8s %-11d %s%n",
                    entry.getKey(), summary.pass, summary.fail, passPercentage, summary.incomplete, average);
        }
        if (departments.isEmpty()) {
            System.out.println("No enrollments found.");
        }
    }

    private void updateMark(String rollNo, String courseCode, int semester, String column, double mark)
            throws SQLException {
        Enrollment enrollment = findEnrollment(rollNo, courseCode, semester);
        if (enrollment == null) {
            throw enrollmentNotFound(rollNo, courseCode, semester);
        }
        String sql = "INSERT INTO assessment_marks (enrollment_id, " + column + ") VALUES (?, ?) " +
                "ON CONFLICT(enrollment_id) DO UPDATE SET " + column + " = excluded." + column;
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enrollment.id);
            statement.setDouble(2, mark);
            statement.executeUpdate();
        }
    }

    private Enrollment findEnrollment(String rollNo, String courseCode, int semester) throws SQLException {
        String sql = ENROLLMENT_SELECT +
                "WHERE e.roll_no = ? AND e.course_code = ? AND e.semester = ?";
        List<Enrollment> matches = findEnrollments(sql, rollNo.trim().toUpperCase(),
                courseCode.trim().toUpperCase(), semester);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private boolean enrollmentExists(String rollNo, String courseCode, int semester) throws SQLException {
        String sql = "SELECT 1 FROM enrollments WHERE roll_no = ? AND course_code = ? AND semester = ?";
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rollNo.trim().toUpperCase());
            statement.setString(2, courseCode.trim().toUpperCase());
            statement.setInt(3, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean recordExists(String sql, String key) throws SQLException {
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be empty.");
        }
    }

    private static IllegalArgumentException enrollmentNotFound(String rollNo, String courseCode, int semester) {
        return new IllegalArgumentException("No enrollment found for roll number "
                + rollNo.trim().toUpperCase() + ", course " + courseCode.trim().toUpperCase()
                + ", semester " + semester + ". Check these values and enroll the student using option 3 first.");
    }

    private static void checkSemester(int semester) {
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
    }

    private List<Enrollment> findEnrollments(String sql, Object... parameters) throws SQLException {
        List<Enrollment> enrollments = new ArrayList<>();
        try (Connection connection = DBConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    enrollments.add(readEnrollment(resultSet));
                }
            }
        }
        return enrollments;
    }

    private Enrollment readEnrollment(ResultSet resultSet) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.id = resultSet.getInt("enrollment_id");
        enrollment.rollNo = resultSet.getString("roll_no");
        enrollment.studentName = resultSet.getString("name");
        enrollment.department = resultSet.getString("department");
        enrollment.courseCode = resultSet.getString("course_code");
        enrollment.title = resultSet.getString("title");
        enrollment.credits = resultSet.getInt("credits");
        enrollment.courseType = resultSet.getString("course_type");
        enrollment.semester = resultSet.getInt("semester");
        enrollment.classesHeld = resultSet.getInt("classes_held");
        enrollment.classesAttended = resultSet.getInt("classes_attended");
        enrollment.condonationApproved = resultSet.getInt("condonation_approved") == 1;
        enrollment.cia1 = nullableDouble(resultSet, "cia_test1");
        enrollment.cia2 = nullableDouble(resultSet, "cia_test2");
        enrollment.coursework = nullableDouble(resultSet, "coursework_score");
        enrollment.eseMark = nullableDouble(resultSet, "ese_mark");
        enrollment.eseAbsent = resultSet.getInt("ese_absent") == 1;
        return enrollment;
    }

    private static Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private static void checkMark(double mark) {
        if (mark < 0 || mark > 100 || Double.isNaN(mark) || Double.isInfinite(mark)) {
            throw new IllegalArgumentException("Marks must be between 0 and 100.");
        }
    }

    private static void requireUpdated(int count, String message) {
        if (count == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }

    private Result evaluate(Enrollment enrollment) {
        Result result = new Result();
        if (enrollment.classesHeld > 0 && enrollment.attendancePercentage() < 65.0) {
            result.grade = "SA";
            result.status = "SHORTAGE OF ATTENDANCE";
            result.gradePoint = 0;
            return result;
        }
        if (enrollment.classesHeld > 0 && enrollment.attendancePercentage() < 75.0
                && !enrollment.condonationApproved) {
            result.grade = "PENDING";
            result.status = "CONDONATION REQUIRED";
            return result;
        }
        if (enrollment.cia1 == null || enrollment.cia2 == null || enrollment.coursework == null
                || (!enrollment.eseAbsent && enrollment.eseMark == null)) {
            result.grade = "INCOMPLETE";
            result.status = "MARKS PENDING";
            return result;
        }
        result.ciaMean = (enrollment.cia1 + enrollment.cia2 + enrollment.coursework) / 3.0;
        if (enrollment.eseAbsent) {
            result.grade = "AB";
            result.status = "ABSENT";
            result.gradePoint = 0;
            return result;
        }
        double ciaWeight;
        double eseWeight;
        double eseMinimum;
        switch (enrollment.courseType) {
            case "THEORY":
                ciaWeight = 0.40;
                eseWeight = 0.60;
                eseMinimum = 45.0;
                break;
            case "PRACTICAL":
                ciaWeight = 0.60;
                eseWeight = 0.40;
                eseMinimum = 50.0;
                break;
            case "EMBEDDED":
                ciaWeight = 0.50;
                eseWeight = 0.50;
                eseMinimum = 45.0;
                break;
            default:
                throw new IllegalStateException("Unsupported course type: " + enrollment.courseType);
        }
        result.total = result.ciaMean * ciaWeight + enrollment.eseMark * eseWeight;
        if (enrollment.eseMark < eseMinimum || result.total < 50.0) {
            result.grade = "RA";
            result.status = "FAIL";
            result.gradePoint = 0;
        } else {
            result.grade = gradeFor(result.total);
            result.status = "PASS";
            result.gradePoint = gradePointFor(result.grade);
        }
        return result;
    }

    private static String gradeFor(double total) {
        if (total >= 91) return "O";
        if (total >= 81) return "A+";
        if (total >= 71) return "A";
        if (total >= 61) return "B+";
        return "B";
    }

    private static int gradePointFor(String grade) {
        switch (grade) {
            case "O": return 10;
            case "A+": return 9;
            case "A": return 8;
            case "B+": return 7;
            case "B": return 6;
            default: return 0;
        }
    }

    private static final class Enrollment {
        int id;
        String rollNo;
        String studentName;
        String department;
        String courseCode;
        String title;
        int credits;
        String courseType;
        int semester;
        int classesHeld;
        int classesAttended;
        boolean condonationApproved;
        Double cia1;
        Double cia2;
        Double coursework;
        Double eseMark;
        boolean eseAbsent;

        double attendancePercentage() {
            return classesHeld == 0 ? 0.0 : classesAttended * 100.0 / classesHeld;
        }
    }

    private static final class Result {
        Double ciaMean;
        Double total;
        String grade;
        String status;
        Integer gradePoint;
    }

    private static final class StudentSummary {
        final String department;
        int courseCount;
        int credits;
        double weightedPoints;
        boolean complete = true;
        boolean passed = true;

        StudentSummary(String department) {
            this.department = department;
        }
    }

    private static final class DepartmentSummary {
        int pass;
        int fail;
        int incomplete;
        int sgpaCount;
        double sgpaTotal;
    }
}
