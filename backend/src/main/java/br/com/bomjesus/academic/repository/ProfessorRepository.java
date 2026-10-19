package br.com.bomjesus.academic.repository;

import br.com.bomjesus.academic.model.Professor;
import br.com.bomjesus.academic.model.Status;
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
public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    @EntityGraph(attributePaths = "addresses")
    @Query("select p from Professor p")
    List<Professor> findAllWithAddresses();

    @EntityGraph(attributePaths = "addresses")
    @Query("select p from Professor p where p.status = :status")
    List<Professor> findAllWithAddressesByStatus(@Param("status") Status status);

    @EntityGraph(attributePaths = "addresses")
    Optional<Professor> findWithAddressesById(Long id);
}
