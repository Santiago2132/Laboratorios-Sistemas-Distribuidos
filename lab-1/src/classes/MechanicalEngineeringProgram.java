package classes;

import interfaces.AcademicProgram;
import java.util.ArrayList;
import java.util.List;

public class MechanicalEngineeringProgram implements AcademicProgram {
    private String programName;
    private int durationYears;
    private String department;
    private List<String> courses;
    private List<String> enrolledStudents;

    // Constructor
    public MechanicalEngineeringProgram(String programName, int durationYears, String department) {
        this.programName = programName;
        this.durationYears = durationYears;
        this.department = department;
        this.courses = new ArrayList<>();
        this.enrolledStudents = new ArrayList<>();
    }

    @Override
    public void describe_program() {
        System.out.println("Program: " + programName + ", Department: " + department + ", Duration: " + durationYears + " years");
    }

    @Override
    public void add_course(String courseName) {
        courses.add(courseName);
        System.out.println("Added course: " + courseName + " to " + programName);
    }

    @Override
    public void remove_course(String courseName) {
        if (courses.remove(courseName)) {
            System.out.println("Removed course: " + courseName + " from " + programName);
        } else {
            System.out.println("Course: " + courseName + " not found in " + programName);
        }
    }

    @Override
    public void list_courses() {
        System.out.println("Courses in " + programName + ":");
        for (String course : courses) {
            System.out.println("- " + course);
        }
    }

    @Override
    public void enroll_student(String studentName) {
        enrolledStudents.add(studentName);
        System.out.println(studentName + " enrolled in " + programName);
    }

    @Override
    public void graduate_student(String studentName) {
        if (enrolledStudents.remove(studentName)) {
            System.out.println(studentName + " graduated from " + programName);
        } else {
            System.out.println(studentName + " is not enrolled in " + programName);
        }
    }

    @Override
    public void calculate_program_duration() {
        System.out.println("The " + programName + " program duration is " + durationYears + " years.");
    }

    @Override
    public void provide_program_accreditation() {
        System.out.println(programName + " is accredited by the Engineering Accreditation Commission.");
    }

    // Additional method 1: Conduct lab experiment
    public void conduct_lab_experiment(String experimentName) {
        System.out.println(programName + " is conducting a lab experiment: " + experimentName);
    }

    // Additional method 2: Design project
    public void assign_design_project(String projectName) {
        System.out.println(programName + " assigned a design project: " + projectName);
    }
}