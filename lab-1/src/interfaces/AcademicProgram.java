package interfaces;

public interface AcademicProgram {
    // Methods
    void describe_program();
    void add_course(String courseName);
    void remove_course(String courseName);
    void list_courses();
    void enroll_student(String studentName);
    void graduate_student(String studentName);
    void calculate_program_duration();
    void provide_program_accreditation();
}