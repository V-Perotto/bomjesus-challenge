package br.com.bomjesus.academic.mapper;

import br.com.bomjesus.academic.dto.ProfessorResponse;
import br.com.bomjesus.academic.model.Professor;
import br.com.bomjesus.academic.model.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static br.com.bomjesus.academic.support.AddressTestData.addressRequest;
import static br.com.bomjesus.academic.support.ProfessorTestData.professor;
import static br.com.bomjesus.academic.support.ProfessorTestData.professorRequest;
import static org.assertj.core.api.Assertions.assertThat;

class ProfessorMapperTest {

    private final ProfessorMapper mapper = new ProfessorMapperImpl(new AddressMapperImpl());

    @Test
    @DisplayName("toEntity com status nulo cria o professor como ACTIVE, sem endereços")
    void shouldDefaultStatusAndSkipAddressesOnCreate() {
        Professor professor = mapper.toEntity(professorRequest());

        assertThat(professor.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(professor.getSalary()).isEqualByComparingTo("8500");
        assertThat(professor.getAddresses()).isEmpty();
    }

    @Test
    @DisplayName("updateEntity com status nulo mantém o status e não altera id nem endereços")
    void shouldKeepStatusIdAndAddressesOnUpdate() {
        Professor professor = professor(4L);
        professor.setStatus(Status.DISABLE);

        mapper.updateEntity(professorRequest("novo@escola.com", null, new BigDecimal("9000"),
                List.of(addressRequest("Rua Nova"))), professor);

        assertThat(professor.getStatus()).isEqualTo(Status.DISABLE);
        assertThat(professor.getSalary()).isEqualByComparingTo("9000");
        assertThat(professor.getId()).isEqualTo(4L);
        assertThat(professor.getAddresses()).hasSize(1).first().extracting("street")
                .isEqualTo("Rua XV de Novembro, 100");
    }

    @Test
    @DisplayName("toResponse mapeia salário e endereços")
    void shouldMapEntityToResponse() {
        ProfessorResponse response = mapper.toResponse(professor(2L, "Marcos"));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("Marcos");
        assertThat(response.salary()).isEqualByComparingTo("8500.00");
        assertThat(response.addresses()).hasSize(1);
    }
}
