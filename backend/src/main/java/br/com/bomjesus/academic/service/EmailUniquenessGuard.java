package br.com.bomjesus.academic.service;

import br.com.bomjesus.academic.exception.ConflictException;
import br.com.bomjesus.academic.repository.PersonRepository;
import org.springframework.stereotype.Component;

/**
 * Garante que o e-mail seja único entre todas as pessoas, alunos e professores (BR-04).
 */
@Component
public class EmailUniquenessGuard {

    private final PersonRepository personRepository;

    public EmailUniquenessGuard(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    /**
     * @param normalizedEmail e-mail já em minúsculas
     * @param currentId       id da pessoa sendo atualizada, ou {@code null} na criação
     */
    public void assertAvailable(String normalizedEmail, Long currentId) {
        boolean taken = currentId == null
                ? personRepository.existsByEmailAddress(normalizedEmail)
                : personRepository.existsByEmailAddressAndIdNot(normalizedEmail, currentId);
        if (taken) {
            throw new ConflictException("emailAddress", "e-mail já cadastrado",
                    "Já existe uma pessoa cadastrada com o e-mail %s.".formatted(normalizedEmail));
        }
    }
}
