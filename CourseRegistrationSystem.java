import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class CourseRegistrationSystem {

    private static final String DB_URL = "jdbc:sqlite:courseregistration.db";
    private static Connection conn;

    public static void main(String[] args) {
        // Load the SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC driver not found.");
            return;
        }

        // Initialize the database connection
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.out.println("Error connecting to the database.");
            e.printStackTrace();
            return;
        }

        // Initialize the database
        initializeDatabase();

        Scanner scanner = new Scanner(System.in);
        try {
            while (true) {
                System.out.println("1. List Courses");
                System.out.println("2. Register for a Course");
                System.out.println("3. Drop a Course");
                System.out.println("4. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                scanner.nextLine();  // Consume newline after integer input

                switch (choice) {
                    case 1:
                        listCourses();
                        break;
                    case 2:
                        registerForCourse(scanner);
                        break;
                    case 3:
                        dropCourse(scanner);
                        break;
                    case 4:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("Input error occurred. Exiting...");
        } finally {
            scanner.close();
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void initializeDatabase() {
        try (PreparedStatement createCoursesTable = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS courses (" +
                     "course_code TEXT PRIMARY KEY, " +
                     "title TEXT, " +
                     "description TEXT, " +
                     "capacity INTEGER, " +
                     "schedule TEXT)");
             PreparedStatement createStudentsTable = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS students (" +
                     "student_id TEXT PRIMARY KEY, " +
                     "name TEXT, " +
                     "registered_courses TEXT)")) {

            createCoursesTable.executeUpdate();
            createStudentsTable.executeUpdate();

            // Add sample courses
            addSampleCourses();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void addSampleCourses() {
        String sql = "INSERT INTO courses (course_code, title, description, capacity, schedule) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "IFM02A2");
            pstmt.setString(2, "Introduction to Programming");
            pstmt.setString(3, "Basic programming concepts.");
            pstmt.setInt(4, 30);
            pstmt.setString(5, "MWF 10:00-11:00");
            pstmt.executeUpdate();

            pstmt.setString(1, "IFM03B3");
            pstmt.setString(2, "Data Structures");
            pstmt.setString(3, "Introduction to data structures.");
            pstmt.setInt(4, 25);
            pstmt.setString(5, "TTh 14:00-15:30");
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void listCourses() {
        String sql = "SELECT * FROM courses";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String courseCode = rs.getString("course_code");
                String title = rs.getString("title");
                String description = rs.getString("description");
                int capacity = rs.getInt("capacity");
                String schedule = rs.getString("schedule");

                System.out.println("Course Code: " + courseCode);
                System.out.println("Title: " + title);
                System.out.println("Description: " + description);
                System.out.println("Capacity: " + capacity);
                System.out.println("Schedule: " + schedule);
                System.out.println();
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void registerForCourse(Scanner scanner) {
        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine();
        System.out.print("Enter Course Code: ");
        String courseCode = scanner.nextLine();

        String selectCourse = "SELECT capacity FROM courses WHERE course_code = ?";
        String selectStudent = "SELECT registered_courses FROM students WHERE student_id = ?";
        String updateStudent = "UPDATE students SET registered_courses = ? WHERE student_id = ?";
        String updateCourse = "UPDATE courses SET capacity = capacity - 1 WHERE course_code = ?";

        try (PreparedStatement pstmtSelectCourse = conn.prepareStatement(selectCourse);
             PreparedStatement pstmtSelectStudent = conn.prepareStatement(selectStudent);
             PreparedStatement pstmtUpdateStudent = conn.prepareStatement(updateStudent);
             PreparedStatement pstmtUpdateCourse = conn.prepareStatement(updateCourse)) {

            // Check if the course has available slots
            pstmtSelectCourse.setString(1, courseCode);
            ResultSet rsCourse = pstmtSelectCourse.executeQuery();
            if (rsCourse.next()) {
                int capacity = rsCourse.getInt("capacity");
                if (capacity <= 0) {
                    System.out.println("Course is full.");
                    return;
                }
            } else {
                System.out.println("Course not found.");
                return;
            }

            // Check if the student is already registered
            pstmtSelectStudent.setString(1, studentId);
            ResultSet rsStudent = pstmtSelectStudent.executeQuery();
            if (rsStudent.next()) {
                String registeredCourses = rsStudent.getString("registered_courses");
                if (registeredCourses != null && registeredCourses.contains(courseCode)) {
                    System.out.println("Student is already registered for this course.");
                    return;
                }

                // Register the student for the course
                List<String> courses = new ArrayList<>();
                if (registeredCourses != null && !registeredCourses.isEmpty()) {
                    for (String course : registeredCourses.split(",")) {
                        courses.add(course);
                    }
                }
                courses.add(courseCode);
                pstmtUpdateStudent.setString(1, String.join(",", courses));
                pstmtUpdateStudent.setString(2, studentId);
                pstmtUpdateStudent.executeUpdate();

                // Decrease the course capacity
                pstmtUpdateCourse.setString(1, courseCode);
                pstmtUpdateCourse.executeUpdate();

                System.out.println("Student registered for the course successfully.");
            } else {
                System.out.println("Student not found. Would you like to add a new student? (Y/N)");
                String choice = scanner.nextLine();
                if (choice.equalsIgnoreCase("Y")) {
                    System.out.print("Enter Student Name: ");
                    String studentName = scanner.nextLine();
                    addNewStudent(studentId, studentName);
                    registerForCourse(scanner); // Retry registration after adding the student
                } else {
                    System.out.println("Registration canceled.");
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void addNewStudent(String studentId, String studentName) {
        String sql = "INSERT INTO students (student_id, name, registered_courses) VALUES (?, ?, NULL)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentName);
            pstmt.executeUpdate();

            System.out.println("New student added successfully.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void dropCourse(Scanner scanner) {
        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine();
        System.out.print("Enter Course Code: ");
        String courseCode = scanner.nextLine();

        String selectStudent = "SELECT registered_courses FROM students WHERE student_id = ?";
        String updateStudent = "UPDATE students SET registered_courses = ? WHERE student_id = ?";
        String updateCourse = "UPDATE courses SET capacity = capacity + 1 WHERE course_code = ?";

        try (PreparedStatement pstmtSelectStudent = conn.prepareStatement(selectStudent);
             PreparedStatement pstmtUpdateStudent = conn.prepareStatement(updateStudent);
             PreparedStatement pstmtUpdateCourse = conn.prepareStatement(updateCourse)) {

            // Check if the student is registered
            pstmtSelectStudent.setString(1, studentId);
            ResultSet rsStudent = pstmtSelectStudent.executeQuery();
            if (rsStudent.next()) {
                String registeredCourses = rsStudent.getString("registered_courses");
                if (registeredCourses == null || !registeredCourses.contains(courseCode)) {
                    System.out.println("Student is not registered for this course.");
                    return;
                }

                // Remove the course from the student's registered courses
                List<String> courses = new ArrayList<>();
                for (String course : registeredCourses.split(",")) {
                    if (!course.equals(courseCode)) {
                        courses.add(course);
                    }
                }
                pstmtUpdateStudent.setString(1, String.join(",", courses));
                pstmtUpdateStudent.setString(2, studentId);
                pstmtUpdateStudent.executeUpdate();

                // Increase the course capacity
                pstmtUpdateCourse.setString(1, courseCode);
                pstmtUpdateCourse.executeUpdate();

                System.out.println("Course dropped successfully.");
            } else {
                System.out.println("Student not found.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
