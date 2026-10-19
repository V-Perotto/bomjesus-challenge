package br.com.bomjesus.academic.service;

import br.com.bomjesus.academic.dto.StudentRequest;
import br.com.bomjesus.academic.dto.StudentResponse;
import br.com.bomjesus.academic.exception.ConflictException;
import br.com.bomjesus.academic.exception.ResourceNotFoundException;
import br.com.bomjesus.academic.mapper.AddressMapper;
import br.com.bomjesus.academic.mapper.AddressMapperImpl;
import br.com.bomjesus.academic.mapper.StudentMapperImpl;
import br.com.bomjesus.academic.model.Address;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.model.Student;
import br.com.bomjesus.academic.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static br.com.bomjesus.academic.support.AddressTestData.addressRequest;
import static br.com.bomjesus.academic.support.StudentTestData.student;
import static br.com.bomjesus.academic.support.StudentTestData.studentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras BR-01, BR-02 e BR-04 a BR-10 do data-model aplicadas a alunos.
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EmailUniquenessGuard emailUniquenessGuard;

    private StudentService service;

    @BeforeEach
    void setUp() {
        AddressMapper addressMapper = new AddressMapperImpl();
        service = new StudentService(studentRepository, new StudentMapperImpl(addressMapper), addressMapper,
                emailUniquenessGuard);
    }

    @Test
    @DisplayName("findAll sem status lista todos; com status usa o filtro")
    void shouldListAllOrFilterByStatus() {
        when(studentRepository.findAllWithAddresses()).thenReturn(List.of(student(1L)));
        when(studentRepository.findAllWithAddressesByStatus(Status.DISABLE)).thenReturn(List.of());

        assertThat(service.findAll(null)).hasSize(1);
        assertThat(service.findAll(Status.DISABLE)).isEmpty();
    }

    @Test
    @DisplayName("findAll ordena pelo português, ignorando caixa e acentos, e desempata pelo id (BR-08)")
    void shouldSortByPortugueseCollationThenId() {
        when(studentRepository.findAllWithAddresses()).thenReturn(List.of(
                student(1L, "bruno"), student(2L, "Álvaro"), student(4L, "Ana"),
                student(5L, "Carla"), student(3L, "Carla")));

        List<StudentResponse> result = service.findAll(null);

        assertThat(result).extracting(StudentResponse::name, StudentResponse::id).containsExactly(
                tuple("Álvaro", 2L),
                tuple("Ana", 4L),
                tuple("bruno", 1L),
                tuple("Carla", 3L),
                tuple("Carla", 5L));
    }

    @Test
    @DisplayName("findById retorna o aluno com os endereços")
    void shouldFindById() {
        when(studentRepository.findWithAddressesById(1L)).thenReturn(Optional.of(student(1L)));

        StudentResponse response = service.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.addresses()).hasSize(1);
    }

    @Test
    @DisplayName("findById de id inexistente lança 404")
    void shouldThrowNotFoundWhenStudentDoesNotExist() {
        when(studentRepository.findWithAddressesById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aluno com id 99 não encontrado.");
    }

    @Test
    @DisplayName("create normaliza e-mail e matrícula, aplica ACTIVE e liga os endereços ao aluno")
    void shouldCreateStudentWithNormalizedValues() {
        when(studentRepository.existsByStudentNumber("AB-2026")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        StudentResponse response = service.create(studentRequest("Ana@Escola.COM", "ab-2026"));

        ArgumentCaptor<Student> saved = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(saved.capture());
        verify(emailUniquenessGuard).assertAvailable("ana@escola.com", null);
        assertThat(saved.getValue().getEmailAddress()).isEqualTo("ana@escola.com");
        assertThat(saved.getValue().getStudentNumber()).isEqualTo("AB-2026");
        assertThat(saved.getValue().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getValue().getAddresses()).extracting(Address::getPerson).containsOnly(saved.getValue());
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("create com e-mail já usado lança 409 e não grava")
    void shouldNotCreateWhenEmailIsTaken() {
        doThrow(new ConflictException("emailAddress", "e-mail já cadastrado", "duplicado"))
                .when(emailUniquenessGuard).assertAvailable("ana@escola.com", null);

        assertThatThrownBy(() -> service.create(studentRequest()))
                .isInstanceOf(ConflictException.class)
                .extracting("field").isEqualTo("emailAddress");
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create com matrícula já usada lança 409 no campo studentNumber")
    void shouldNotCreateWhenStudentNumberIsTaken() {
        when(studentRepository.existsByStudentNumber("2026001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(studentRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um aluno com a matrícula 2026001.")
                .extracting("field").isEqualTo("studentNumber");
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("update substitui os endereços e mantém o status quando ele é omitido")
    void shouldUpdateReplacingAddressesAndKeepingStatus() {
        Student existing = student(1L);
        existing.setStatus(Status.DISABLE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByStudentNumberAndIdNot("2026001", 1L)).thenReturn(false);
        when(studentRepository.save(existing)).thenReturn(existing);
        StudentRequest request = studentRequest("ana@escola.com", "2026001", null,
                List.of(addressRequest("Rua Nova, 1"), addressRequest("Rua Nova, 2")));

        StudentResponse response = service.update(1L, request);

        assertThat(response.status()).isEqualTo(Status.DISABLE);
        assertThat(response.addresses()).extracting("street").containsExactly("Rua Nova, 1", "Rua Nova, 2");
        verify(emailUniquenessGuard).assertAvailable("ana@escola.com", 1L);
    }

    @Test
    @DisplayName("update checa a unicidade antes de alterar a entidade gerenciada")
    void shouldCheckUniquenessBeforeChangingTheEntity() {
        Student existing = student(1L);
        String originalEmail = existing.getEmailAddress();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new ConflictException("emailAddress", "e-mail já cadastrado", "duplicado"))
                .when(emailUniquenessGuard).assertAvailable("outra@escola.com", 1L);

        assertThatThrownBy(() -> service.update(1L, studentRequest("outra@escola.com", "2026001")))
                .isInstanceOf(ConflictException.class);
        assertThat(existing.getEmailAddress()).isEqualTo(originalEmail);
    }

    @Test
    @DisplayName("update com a própria matrícula usa existsByStudentNumberAndIdNot")
    void shouldUseIdNotQueryWhenUpdatingStudentNumber() {
        Student existing = student(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(existing)).thenReturn(existing);

        service.update(1L, studentRequest("ana@escola.com", "MAT-1"));

        verify(studentRepository).existsByStudentNumberAndIdNot("MAT-1", 1L);
        verify(studentRepository, never()).existsByStudentNumber(anyString());
    }

    @Test
    @DisplayName("update com matrícula de outro aluno lança 409")
    void shouldNotUpdateWhenStudentNumberBelongsToAnotherStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(studentRepository.existsByStudentNumberAndIdNot("2026002", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, studentRequest("ana@escola.com", "2026002")))
                .isInstanceOf(ConflictException.class);
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("update de id inexistente lança 404")
    void shouldThrowNotFoundOnUpdateWhenStudentDoesNotExist() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, studentRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(emailUniquenessGuard, never()).assertAvailable(anyString(), anyLong());
    }

    @Test
    @DisplayName("delete remove o aluno existente")
    void shouldDeleteStudent() {
        Student existing = student(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(studentRepository).delete(existing);
    }

    @Test
    @DisplayName("delete de id inexistente lança 404 e não remove nada")
    void shouldThrowNotFoundOnDeleteWhenStudentDoesNotExist() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(studentRepository, never()).delete(any());
    }

    private static Student withId(Student student) {
        ReflectionTestUtils.setField(student, "id", 1L);
        return student;
    }
}
