package br.com.bomjesus.academic.support;

import br.com.bomjesus.academic.dto.AddressRequest;
import br.com.bomjesus.academic.dto.ProfessorRequest;
import br.com.bomjesus.academic.model.Professor;
import br.com.bomjesus.academic.model.Status;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static br.com.bomjesus.academic.support.AddressTestData.DEFAULT_STREET;
import static br.com.bomjesus.academic.support.AddressTestData.address;
import static br.com.bomjesus.academic.support.AddressTestData.addressRequest;

public final class ProfessorTestData {

    public static final String EMAIL = "helena@escola.com";

    private ProfessorTestData() {
    }

    public static ProfessorRequest professorRequest() {
        return professorRequest(EMAIL);
    }

    public static ProfessorRequest professorRequest(String email) {
        return professorRequest(email, null, new BigDecimal("8500"), List.of(addressRequest()));
    }

    public static ProfessorRequest professorRequest(String email, Status status, BigDecimal salary,
                                                    List<AddressRequest> addresses) {
        return new ProfessorRequest("Helena Rocha", "+55 41 3333-0001", email, status, salary, addresses);
    }

    /** Professor ainda não persistido (sem id), com um endereço ligado. */
    public static Professor newProfessor(String name, String email) {
        Professor professor = new Professor();
        professor.setName(name);
        professor.setPhoneNumber("+55 41 3333-0001");
        professor.setEmailAddress(email);
        professor.setStatus(Status.ACTIVE);
        professor.setSalary(new BigDecimal("8500.00"));
        professor.replaceAddresses(List.of(address(DEFAULT_STREET)));
        return professor;
    }

    /** Professor "persistido" para testes com mocks: o id é atribuído por reflexão. */
    public static Professor professor(Long id) {
        return professor(id, "Helena Rocha");
    }

    public static Professor professor(Long id, String name) {
        Professor professor = newProfessor(name, "professor" + id + "@escola.com");
        ReflectionTestUtils.setField(professor, "id", id);
        return professor;
    }
}
