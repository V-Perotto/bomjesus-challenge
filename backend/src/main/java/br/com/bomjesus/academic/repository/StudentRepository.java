package br.com.bomjesus.academic.repository;

import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.model.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * As listagens carregam os endereços na mesma consulta (sem N+1) e não ordenam: a ordenação
 * alfabética do português (BR-08) é feita no Service.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = "addresses")
    @Query("select s from Student s")
    List<Student> findAllWithAddresses();

    @EntityGraph(attributePaths = "addresses")
    @Query("select s from Student s where s.status = :status")
    List<Student> findAllWithAddressesByStatus(@Param("status") Status status);

    @EntityGraph(attributePaths = "addresses")
    Optional<Student> findWithAddressesById(Long id);

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByStudentNumberAndIdNot(String studentNumber, Long id);
}
