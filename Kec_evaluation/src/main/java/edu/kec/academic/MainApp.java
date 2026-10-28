package edu.kec.academic;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class MainApp {
    private final Scanner scanner = new Scanner(System.in);
    private final StudentDAO studentDAO = new StudentDAO();
    private final AcademicDAO academicDAO = new AcademicDAO();

    public static void main(String[] args) {
        try {
            DBConnection.initialize();
            new MainApp().run();
        } catch (SQLException | IOException exception) {
            System.err.println("Could not initialize the academic database: " + exception.getMessage());
            System.exit(1);
        }
    }

    private void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Select an option: ");
            try {
                switch (choice) {
                    case "1":
                        studentManagementMenu();
                        break;
                    case "2":
                        addCourse();
                        break;
                    case "3":
                        enrollmentAttendanceMenu();
                        break;
                    case "4":
                        recordMarksByPhase();
                        break;
                    case "5":
                        academicDAO.printMarkSheet(promptRequired("Roll number: "),
                                readIntInRange("Semester (1-8): ", 1, 8));
                        break;
                    case "6":
                        academicDAO.printAttendanceDefaulters(readIntInRange("Semester (1-8): ", 1, 8));
                        break;
                    case "7":
                        academicDAO.printDepartmentSummary(readIntInRange("Semester (1-8): ", 1, 8));
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Choose one of the listed options.");
                }
            } catch (SQLException | IllegalArgumentException exception) {
                System.err.println("Operation failed: " + exception.getMessage());
            }
        }
        System.out.println("Academic system closed.");
    }

    private void printMenu() {
        System.out.println("\n=== KEC R2024 Student Academic & Continuous Evaluation ===");
        System.out.println("1. Student Management");
        System.out.println("2. Course Catalog");
        System.out.println("3. Course Enrollment & Attendance Tracking");
        System.out.println("4. Enter Marks (Phase 1: CIA, Phase 2: Assignment, Phase 3: ESE)");
        System.out.println("5. Student Semester Mark Sheet");
        System.out.println("6. Attendance Defaulter Report");
        System.out.println("7. Department-wise Class Performance & Result Summary");
        System.out.println("0. Exit");
    }

    private void studentManagementMenu() throws SQLException {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- Student Management ---");
            System.out.println("1. Register student");
            System.out.println("2. Update existing student");
            System.out.println("0. Back to main menu");
            switch (readIntInRange("Select an option: ", 0, 2)) {
                case 1:
                    addStudent();
                    break;
                case 2:
                    updateStudent();
                    break;
                case 0:
                    inMenu = false;
                    break;
                default:
                    throw new IllegalStateException("Unexpected student menu option.");
            }
        }
    }

    private void enrollmentAttendanceMenu() throws SQLException {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- Course Enrollment & Attendance ---");
            System.out.println("1. Enroll student in a course");
            System.out.println("2. Record/update attendance");
            System.out.println("3. Record medical condonation decision");
            System.out.println("0. Back to main menu");
            switch (readIntInRange("Select an option: ", 0, 3)) {
                case 1:
                    enrollStudent();
                    break;
                case 2:
                    recordAttendance();
                    break;
                case 3:
                    approveCondonation();
                    break;
                case 0:
                    inMenu = false;
                    break;
                default:
                    throw new IllegalStateException("Unexpected enrollment menu option.");
            }
        }
    }

    private void addStudent() throws SQLException {
        String rollNo = promptRequired("Roll number: ");
        String name = promptRequired("Student name: ");
        String department = promptRequired("Department: ");
        int semester = readIntInRange("Current semester (1-8): ", 1, 8);
        studentDAO.addStudent(rollNo, name, department, semester);
        System.out.println("Student added.");
    }

    private void updateStudent() throws SQLException {
        String rollNo = promptRequired("Existing roll number: ");
        String name = promptRequired("Student name: ");
        String department = promptRequired("Department: ");
        int semester = readIntInRange("Current semester (1-8): ", 1, 8);
        studentDAO.updateStudent(rollNo, name, department, semester);
        System.out.println("Student details updated. Existing course enrollments and marks were not changed.");
    }

    private void addCourse() throws SQLException {
        String code = promptRequired("Course code: ");
        String title = promptRequired("Course title: ");
        int credits = readIntInRange("Credits (1-30): ", 1, 30);
        String type = readCourseType();
        studentDAO.addCourse(code, title, credits, type);
        System.out.println("Course added.");
    }

    private void enrollStudent() throws SQLException {
        academicDAO.enroll(promptRequired("Roll number: "), promptRequired("Course code: "),
                readIntInRange("Semester (1-8): ", 1, 8));
        System.out.println("Student enrolled.");
    }

    private void recordAttendance() throws SQLException {
        String rollNo = promptRequired("Roll number: ");
        String courseCode = promptRequired("Course code: ");
        int semester = readIntInRange("Semester (1-8): ", 1, 8);
        int held = readIntInRange("Total classes held: ", 0, Integer.MAX_VALUE);
        int attended = readIntInRange("Classes attended (0-" + held + "): ", 0, held);
        academicDAO.recordAttendance(rollNo, courseCode, semester, held, attended);
        System.out.println("Attendance recorded.");
    }

    private void approveCondonation() throws SQLException {
        System.out.println("The student must already be enrolled in this course for the chosen semester.");
        System.out.println("Use the student's exact registered roll number (for example, 25ITR125).");
        String rollNo = promptRequired("Roll number: ");
        String courseCode = promptRequired("Course code: ");
        int semester = readIntInRange("Semester (1-8): ", 1, 8);
        boolean approved = readYesNo("Medical condonation approved? (yes/no): ");
        academicDAO.approveCondonation(rollNo, courseCode, semester, approved);
        System.out.println("Condonation decision recorded.");
    }

    private void recordMarksByPhase() throws SQLException {
        String rollNo = promptRequired("Roll number: ");
        String courseCode = promptRequired("Course code: ");
        int semester = readIntInRange("Semester (1-8): ", 1, 8);
        System.out.println("Select assessment phase:");
        System.out.println("1. Phase 1 - CIA Test 1 and CIA Test 2");
        System.out.println("2. Phase 2 - enter assignment / quiz / lab evaluation mark");
        System.out.println("3. Phase 3 - enter ESE mark or record absence");
        System.out.println("At the phase prompt, enter only 1, 2, or 3; marks are entered at the next prompts.");
        int phase = readIntInRange("Select phase (1, 2, or 3): ", 1, 3);
        if (phase == 1) {
            double cia1 = readDoubleInRange("CIA Test 1 mark (0-100): ", 0, 100);
            double cia2 = readDoubleInRange("CIA Test 2 mark (0-100): ", 0, 100);
            academicDAO.recordCiaMarks(rollNo, courseCode, semester, cia1, cia2, null);
            System.out.println("Phase 1 CIA Test 1 and Test 2 marks recorded.");
        } else if (phase == 2) {
            double coursework = readDoubleInRange("Assignment / quiz / lab evaluation mark (0-100): ", 0, 100);
            academicDAO.recordCiaMark(rollNo, courseCode, semester, "COURSEWORK", coursework);
            System.out.println("Phase 2 coursework mark recorded.");
        } else {
            recordEseMark(rollNo, courseCode, semester);
        }
    }

    private void recordEseMark(String rollNo, String courseCode, int semester) throws SQLException {
        boolean absent = readYesNo("Absent from ESE? (yes/no): ");
        Double mark = absent ? null : readDoubleInRange("ESE mark (0-100): ", 0, 100);
        academicDAO.recordEseMark(rollNo, courseCode, semester, mark, absent);
        System.out.println(absent ? "ESE absence recorded." : "ESE mark recorded.");
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    private String promptRequired(String label) {
        while (true) {
            String value = prompt(label);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This value cannot be empty.");
        }
    }

    private int readInt(String label) {
        while (true) {
            try {
                return Integer.parseInt(prompt(label));
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private int readIntInRange(String label, int minimum, int maximum) {
        while (true) {
            int value = readInt(label);
            if (value >= minimum && value <= maximum) {
                return value;
            }
            System.out.printf("Enter a value from %d to %d.%n", minimum, maximum);
        }
    }

    private double readDoubleInRange(String label, double minimum, double maximum) {
        while (true) {
            try {
                double value = Double.parseDouble(prompt(label));
                if (Double.isFinite(value) && value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a numeric mark.");
                continue;
            }
            System.out.printf("Enter a value from %.0f to %.0f.%n", minimum, maximum);
        }
    }

    private boolean readYesNo(String label) {
        while (true) {
            String answer = prompt(label);
            if ("yes".equalsIgnoreCase(answer) || "y".equalsIgnoreCase(answer)) {
                return true;
            }
            if ("no".equalsIgnoreCase(answer) || "n".equalsIgnoreCase(answer)) {
                return false;
            }
            System.out.println("Enter yes or no.");
        }
    }

    private String readCourseType() {
        while (true) {
            String type = prompt("Course type (THEORY/PRACTICAL/EMBEDDED): ").toUpperCase();
            if (type.equals("THEORY") || type.equals("PRACTICAL") || type.equals("EMBEDDED")) {
                return type;
            }
            System.out.println("Enter THEORY, PRACTICAL, or EMBEDDED.");
        }
    }

}
