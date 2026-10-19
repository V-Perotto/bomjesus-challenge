package br.com.bomjesus.academic.repository;

import br.com.bomjesus.academic.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

    boolean existsByEmailAddress(String emailAddress);

    boolean existsByEmailAddressAndIdNot(String emailAddress, Long id);
}
