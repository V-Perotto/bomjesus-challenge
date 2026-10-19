package br.com.bomjesus.academic.service;

import br.com.bomjesus.academic.dto.ProfessorRequest;
import br.com.bomjesus.academic.dto.ProfessorResponse;
import br.com.bomjesus.academic.exception.ConflictException;
import br.com.bomjesus.academic.exception.ResourceNotFoundException;
import br.com.bomjesus.academic.mapper.AddressMapper;
import br.com.bomjesus.academic.mapper.AddressMapperImpl;
import br.com.bomjesus.academic.mapper.ProfessorMapperImpl;
import br.com.bomjesus.academic.model.Address;
import br.com.bomjesus.academic.model.Professor;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static br.com.bomjesus.academic.support.AddressTestData.addressRequest;
import static br.com.bomjesus.academic.support.ProfessorTestData.professor;
import static br.com.bomjesus.academic.support.ProfessorTestData.professorRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras BR-01, BR-03, BR-04 e BR-06 a BR-10 do data-model aplicadas a professores.
 */
@ExtendWith(MockitoExtension.class)
class ProfessorServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private EmailUniquenessGuard emailUniquenessGuard;

    private ProfessorService service;

    @BeforeEach
    void setUp() {
        AddressMapper addressMapper = new AddressMapperImpl();
        service = new ProfessorService(professorRepository, new ProfessorMapperImpl(addressMapper), addressMapper,
                emailUniquenessGuard);
    }

    @Test
    @DisplayName("findAll ordena pelo português e filtra por status")
    void shouldSortAndFilter() {
        when(professorRepository.findAllWithAddresses())
                .thenReturn(List.of(professor(1L, "Óscar"), professor(2L, "marcos"), professor(3L, "Helena")));
        when(professorRepository.findAllWithAddressesByStatus(Status.DISABLE)).thenReturn(List.of());

        assertThat(service.findAll(null)).extracting(ProfessorResponse::name)
                .containsExactly("Helena", "marcos", "Óscar");
        assertThat(service.findAll(Status.DISABLE)).isEmpty();
    }

    @Test
    @DisplayName("findById: encontrado retorna o professor; inexistente lança 404")
    void shouldFindByIdOrThrowNotFound() {
        when(professorRepository.findWithAddressesById(1L)).thenReturn(Optional.of(professor(1L)));
        when(professorRepository.findWithAddressesById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(1L).id()).isEqualTo(1L);
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Professor com id 99 não encontrado.");
    }

    @Test
    @DisplayName("create normaliza e-mail, aplica ACTIVE, salário com 2 casas e liga os endereços")
    void shouldCreateProfessorWithNormalizedValues() {
        when(professorRepository.save(any(Professor.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        ProfessorResponse response = service.create(professorRequest("Helena@Escola.COM"));

        ArgumentCaptor<Professor> saved = ArgumentCaptor.forClass(Professor.class);
        verify(professorRepository).save(saved.capture());
        verify(emailUniquenessGuard).assertAvailable("helena@escola.com", null);
        assertThat(saved.getValue().getEmailAddress()).isEqualTo("helena@escola.com");
        assertThat(saved.getValue().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getValue().getAddresses()).extracting(Address::getPerson).containsOnly(saved.getValue());
        assertThat(response.salary()).isEqualTo(new BigDecimal("8500.00"));
        assertThat(response.salary().toPlainString()).isEqualTo("8500.00");
    }

    @Test
    @DisplayName("create com e-mail já usado (ex.: por um aluno) lança 409 e não grava")
    void shouldNotCreateWhenEmailIsTaken() {
        doThrow(new ConflictException("emailAddress", "e-mail já cadastrado", "duplicado"))
                .when(emailUniquenessGuard).assertAvailable("ana@escola.com", null);

        assertThatThrownBy(() -> service.create(professorRequest("ana@escola.com")))
                .isInstanceOf(ConflictException.class);
        verify(professorRepository, never()).save(any());
    }

    @Test
    @DisplayName("update substitui endereços, mantém o status omitido e normaliza o salário")
    void shouldUpdateProfessor() {
        Professor existing = professor(1L);
        existing.setStatus(Status.DISABLE);
        when(professorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(professorRepository.save(existing)).thenReturn(existing);
        ProfessorRequest request = professorRequest("helena@escola.com", null, new BigDecimal("9200.5"),
                List.of(addressRequest("Rua Nova")));

        ProfessorResponse response = service.update(1L, request);

        assertThat(response.status()).isEqualTo(Status.DISABLE);
        assertThat(response.salary().toPlainString()).isEqualTo("9200.50");
        assertThat(response.addresses()).extracting("street").containsExactly("Rua Nova");
        verify(emailUniquenessGuard).assertAvailable("helena@escola.com", 1L);
    }

    @Test
    @DisplayName("update checa o e-mail antes de alterar a entidade gerenciada")
    void shouldCheckEmailBeforeChangingTheEntity() {
        Professor existing = professor(1L);
        String originalEmail = existing.getEmailAddress();
        when(professorRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new ConflictException("emailAddress", "e-mail já cadastrado", "duplicado"))
                .when(emailUniquenessGuard).assertAvailable("ana@escola.com", 1L);

        assertThatThrownBy(() -> service.update(1L, professorRequest("ana@escola.com")))
                .isInstanceOf(ConflictException.class);
        assertThat(existing.getEmailAddress()).isEqualTo(originalEmail);
        verify(professorRepository, never()).save(any());
    }

    @Test
    @DisplayName("update de id inexistente lança 404")
    void shouldThrowNotFoundOnUpdate() {
        when(professorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, professorRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(emailUniquenessGuard, never()).assertAvailable(anyString(), anyLong());
    }

    @Test
    @DisplayName("delete remove o professor; inexistente lança 404")
    void shouldDeleteOrThrowNotFound() {
        Professor existing = professor(1L);
        when(professorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(professorRepository.findById(99L)).thenReturn(Optional.empty());

        service.delete(1L);

        verify(professorRepository).delete(existing);
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private static Professor withId(Professor professor) {
        ReflectionTestUtils.setField(professor, "id", 1L);
        return professor;
    }
}
