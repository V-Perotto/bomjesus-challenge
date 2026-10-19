package br.com.bomjesus.academic.service;

import br.com.bomjesus.academic.exception.ConflictException;
import br.com.bomjesus.academic.repository.PersonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailUniquenessGuardTest {

    private static final String EMAIL = "ana@escola.com";

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private EmailUniquenessGuard guard;

    @Test
    @DisplayName("Criação com e-mail livre não lança exceção")
    void shouldAcceptFreeEmailOnCreate() {
        when(personRepository.existsByEmailAddress(EMAIL)).thenReturn(false);

        assertThatCode(() -> guard.assertAvailable(EMAIL, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criação com e-mail já usado lança conflito no campo emailAddress")
    void shouldRejectTakenEmailOnCreate() {
        when(personRepository.existsByEmailAddress(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> guard.assertAvailable(EMAIL, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(EMAIL)
                .extracting("field").isEqualTo("emailAddress");
    }

    @Test
    @DisplayName("Criação nunca consulta 'exists...AndIdNot' com id nulo")
    void shouldNeverUseIdNotQueryOnCreate() {
        when(personRepository.existsByEmailAddress(EMAIL)).thenReturn(false);

        guard.assertAvailable(EMAIL, null);

        verify(personRepository, never()).existsByEmailAddressAndIdNot(anyString(), any());
    }

    @Test
    @DisplayName("Atualização mantendo o próprio e-mail não é conflito")
    void shouldAcceptOwnEmailOnUpdate() {
        when(personRepository.existsByEmailAddressAndIdNot(EMAIL, 1L)).thenReturn(false);

        assertThatCode(() -> guard.assertAvailable(EMAIL, 1L)).doesNotThrowAnyException();
        verify(personRepository, never()).existsByEmailAddress(anyString());
    }

    @Test
    @DisplayName("Atualização com e-mail de outra pessoa lança conflito")
    void shouldRejectEmailOfAnotherPersonOnUpdate() {
        when(personRepository.existsByEmailAddressAndIdNot(EMAIL, 1L)).thenReturn(true);

        assertThatThrownBy(() -> guard.assertAvailable(EMAIL, 1L))
                .isInstanceOf(ConflictException.class);
    }
}
