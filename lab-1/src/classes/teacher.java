package classes;

import interfaces.person;

public class teacher implements person {
    private String name;
    private String gender;
    private String country;
    private int age;
    private String birthday;

    // Constructor
    public teacher(String name, String gender, String country, int age, String birthday) {
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
        System.out.println("Hi, my name is " + name + ", I am from " + country + " and I am a teacher.");
    }

    @Override
    public void is_adult() {
        System.out.println("Is adult: " + (age >= 18));
    }

    @Override
    public void years_until_retirement() {
        System.out.println("Years until retirement: " + (65 - age));
    }

    @Override
    public void greet_in_native_language() {
        System.out.println("Hello, students!");
    }

    // Additional method 1: Teach a subject
    public void teach_subject(String subject) {
        System.out.println(name + " is teaching " + subject + " today.");
    }

    // Additional method 2: Grade assignment
    public void grade_assignment(String studentName, int score) {
        System.out.println(name + " graded " + studentName + "'s assignment with a score of " + score + ".");
    }
}