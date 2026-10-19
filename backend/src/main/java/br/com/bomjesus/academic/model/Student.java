package br.com.bomjesus.academic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "student",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_student_number", columnNames = "student_number"))
public class Student extends Person {

    @Column(name = "student_number", nullable = false, length = 20)
    private String studentNumber;

    @Column(length = 500)
    private String photo;

    public Student() {
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
