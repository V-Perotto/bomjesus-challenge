package br.com.bomjesus.academic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "professor")
public class Professor extends Person {

    @Column(nullable = false, precision = 9, scale = 2)
    private BigDecimal salary;

    public Professor() {
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }
}
