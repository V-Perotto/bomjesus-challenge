package br.com.bomjesus.academic.repository;

import br.com.bomjesus.academic.model.Professor;
import br.com.bomjesus.academic.model.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static br.com.bomjesus.academic.support.AddressTestData.address;
import static br.com.bomjesus.academic.support.ProfessorTestData.newProfessor;
import static br.com.bomjesus.academic.support.StudentTestData.newStudent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ProfessorRepositoryTest {

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Professor salvo gera linhas em person e professor, com o salário em escala 2")
    void shouldPersistProfessorIntoPersonAndProfessorTables() {
        Professor professor = newProfessor("Helena", "helena@escola.com");
        professor.setSalary(new BigDecimal("8500"));
        Long id = professorRepository.saveAndFlush(professor).getId();
        entityManager.clear();

        assertThat(countRows("person", id)).isEqualTo(1);
        assertThat(countRows("professor", id)).isEqualTo(1);
        assertThat(professorRepository.findById(id).orElseThrow().getSalary()).isEqualByComparingTo("8500.00")
                .extracting(BigDecimal::scale).isEqualTo(2);
    }

    @Test
    @DisplayName("E-mail de um professor igual ao de um aluno viola uk_person_email_address")
    void shouldRejectEmailAlreadyUsedByStudent() {
        studentRepository.saveAndFlush(newStudent("Ana", "ana@escola.com", "2026001"));

        assertThatThrownBy(() -> professorRepository.saveAndFlush(newProfessor("Helena", "ana@escola.com")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .message().containsIgnoringCase("uk_person_email_address");
    }

    @Test
    @DisplayName("ProfessorRepository não enxerga alunos: id de aluno retorna vazio")
    void shouldNotFindStudentThroughProfessorRepository() {
        Long studentId = studentRepository.saveAndFlush(newStudent("Ana", "ana@escola.com", "2026001")).getId();
        entityManager.clear();

        assertThat(professorRepository.findById(studentId)).isEmpty();
        assertThat(professorRepository.findWithAddressesById(studentId)).isEmpty();
    }

    @Test
    @DisplayName("Listagens trazem os endereços e filtram por status")
    void shouldFilterByStatus() {
        professorRepository.save(newProfessor("Helena", "helena@escola.com"));
        Professor inactive = newProfessor("Marcos", "marcos@escola.com");
        inactive.setStatus(Status.DISABLE);
        professorRepository.saveAndFlush(inactive);
        entityManager.clear();

        List<Professor> all = professorRepository.findAllWithAddresses();

        assertThat(all).hasSize(2).allSatisfy(professor -> assertThat(entityManager.getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(professor, "addresses")).isTrue());
        assertThat(professorRepository.findAllWithAddressesByStatus(Status.DISABLE))
                .extracting(Professor::getName).containsExactly("Marcos");
    }

    @Test
    @DisplayName("Excluir o professor remove seus endereços (cascata)")
    void shouldDeleteAddressesWithProfessor() {
        Professor professor = newProfessor("Helena", "helena@escola.com");
        professor.replaceAddresses(List.of(address("Rua A"), address("Rua B")));
        Long id = professorRepository.saveAndFlush(professor).getId();

        professorRepository.delete(professorRepository.findById(id).orElseThrow());
        professorRepository.flush();

        assertThat(countRows("person", id)).isZero();
        assertThat(((Number) entityManager.createNativeQuery("select count(*) from address where person_id = :id")
                .setParameter("id", id).getSingleResult()).longValue()).isZero();
    }

    private long countRows(String table, Long id) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table + " where id = :id")
                .setParameter("id", id)
                .getSingleResult()).longValue();
    }
}
