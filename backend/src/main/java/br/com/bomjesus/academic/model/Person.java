package br.com.bomjesus.academic.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Superclasse abstrata de {@link Student} e {@link Professor}, persistida com herança JOINED.
 */
@Entity
@Table(name = "person",
        uniqueConstraints = @UniqueConstraint(name = "uk_person_email_address", columnNames = "email_address"))
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email_address", nullable = false, length = 150)
    private String emailAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Address> addresses = new ArrayList<>();

    protected Person() {
    }

    /**
     * Substitui todos os endereços da pessoa. A lista gerenciada pelo Hibernate é limpa e
     * reaproveitada (nunca reatribuída), para que a remoção de órfãos funcione.
     */
    public void replaceAddresses(Collection<Address> newAddresses) {
        addresses.clear();
        newAddresses.forEach(this::addAddress);
    }

    private void addAddress(Address address) {
        address.setPerson(this);
        addresses.add(address);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }
}
