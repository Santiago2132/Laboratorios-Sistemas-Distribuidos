package classes;

import interfaces.person;
import java.time.LocalDate;

public class student implements person {
    private String name;
    private String gender;
    private String country;
    private int age;
    private LocalDate birthday;

    // Constructor
    public student(String name, String gender, String country, int age, LocalDate birthday) {
        this.name = name;
        this.gender = gender;
        this.country = country;
        this.age = age;
        this.birthday = birthday;
    }

    @Override
    public void said_gender() {
        System.out.println("Gender: " + gender);
    }

    @Override
    public void said_country() {
        System.out.println("My country is " + country);
    }

    @Override
    public void said_name() {
        System.out.println("My name is " + name);
    }

    @Override
    public void celebrate_birthday() {
        System.out.println("IT'S MY BIRTHDAY: " + birthday);
    }

    @Override
    public void introduce_yourself() {
        System.out.println("Hi, my name is " + name + ", I am from " + country);
    }

    @Override
    public void is_adult() {
        System.out.println("Is adult: " + (age >= 18));
    }

    @Override
    public void years_until_retirement() {
        System.out.println("Years until retirement: " + (80 - age));
    }

    @Override
    public void greet_in_native_language() {
        System.out.println("HEY BRO");
    }

    public void perform_role_specific_task(String task) {
        System.out.println(name + " is performing task: " + task + " as a student.");
    }

    public void provide_feedback(String target, String feedback) {
        System.out.println(name + " provides feedback to " + target + ": " + feedback);
    }

    public void criticize_teacher() {
        System.out.println("I don't like this class but it is mandatory");
    }
}