package br.com.bomjesus.academic.mapper;

import br.com.bomjesus.academic.dto.AddressResponse;
import br.com.bomjesus.academic.dto.StudentRequest;
import br.com.bomjesus.academic.dto.StudentResponse;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.bomjesus.academic.support.AddressTestData.addressRequest;
import static br.com.bomjesus.academic.support.StudentTestData.student;
import static br.com.bomjesus.academic.support.StudentTestData.studentRequest;
import static org.assertj.core.api.Assertions.assertThat;

class StudentMapperTest {

    private final StudentMapper mapper = new StudentMapperImpl(new AddressMapperImpl());

    @Test
    @DisplayName("toEntity com status nulo cria o aluno como ACTIVE")
    void shouldDefaultStatusToActiveOnCreate() {
        Student student = mapper.toEntity(studentRequest());

        assertThat(student.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(student.getName()).isEqualTo("Ana Souza");
        assertThat(student.getStudentNumber()).isEqualTo("2026001");
    }

    @Test
    @DisplayName("toEntity não preenche a lista de endereços (ligação feita por replaceAddresses)")
    void shouldNotMapAddressesOnCreate() {
        Student student = mapper.toEntity(studentRequest());

        assertThat(student.getAddresses()).isEmpty();
        assertThat(student.getId()).isNull();
    }

    @Test
    @DisplayName("updateEntity com status nulo mantém o status atual")
    void shouldKeepCurrentStatusWhenUpdateHasNoStatus() {
        Student student = student(1L);
        student.setStatus(Status.DISABLE);

        mapper.updateEntity(studentRequest(), student);

        assertThat(student.getStatus()).isEqualTo(Status.DISABLE);
    }

    @Test
    @DisplayName("updateEntity com photo nula remove a foto")
    void shouldClearPhotoWhenUpdateHasNoPhoto() {
        Student student = student(1L);
        StudentRequest withoutPhoto = new StudentRequest("Ana Souza", "+55 41 99999-0001", "ana@escola.com",
                Status.ACTIVE, "2026001", null, List.of(addressRequest()));

        mapper.updateEntity(withoutPhoto, student);

        assertThat(student.getPhoto()).isNull();
    }

    @Test
    @DisplayName("updateEntity não altera o id nem os endereços")
    void shouldNotTouchIdNorAddressesOnUpdate() {
        Student student = student(7L);

        mapper.updateEntity(studentRequest(), student);

        assertThat(student.getId()).isEqualTo(7L);
        assertThat(student.getAddresses()).hasSize(1);
    }

    @Test
    @DisplayName("toResponse mapeia todos os campos, inclusive os endereços")
    void shouldMapEntityToResponse() {
        StudentResponse response = mapper.toResponse(student(3L, "Bruno"));

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("Bruno");
        assertThat(response.status()).isEqualTo(Status.ACTIVE);
        assertThat(response.addresses()).containsExactly(
                new AddressResponse("Rua XV de Novembro, 100", "Curitiba", "PR", "80020-310", "Brasil"));
    }
}
