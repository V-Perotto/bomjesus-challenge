package br.com.bomjesus.academic.service;

import br.com.bomjesus.academic.model.Person;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Ordem alfabética do português (BR-08): ignora maiúsculas/minúsculas e acentos e desempata
 * pelo identificador.
 */
final class PersonOrdering {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private PersonOrdering() {
    }

    /** Cria um comparador novo a cada chamada, pois {@link Collator} não é thread-safe. */
    static <T extends Person> Comparator<T> byNameThenId() {
        Collator collator = Collator.getInstance(PT_BR);
        collator.setStrength(Collator.PRIMARY);
        return Comparator.<T, String>comparing(Person::getName, collator).thenComparing(Person::getId);
    }
}
