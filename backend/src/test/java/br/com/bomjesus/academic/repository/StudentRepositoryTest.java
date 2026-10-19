package br.com.bomjesus.academic.repository;

import br.com.bomjesus.academic.model.Address;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.model.Student;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static br.com.bomjesus.academic.support.AddressTestData.address;
import static br.com.bomjesus.academic.support.StudentTestData.newStudent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Aluno salvo gera uma linha em person e outra em student com o mesmo id (JOINED)")
    void shouldPersistStudentIntoPersonAndStudentTables() {
        Student saved = studentRepository.saveAndFlush(newStudent("Ana", "ana@escola.com", "2026001"));

        assertThat(countRows("person", saved.getId())).isEqualTo(1);
        assertThat(countRows("student", saved.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("E-mail duplicado viola uk_person_email_address")
    void shouldRejectDuplicatedEmail() {
        studentRepository.saveAndFlush(newStudent("Ana", "ana@escola.com", "2026001"));

        assertThatThrownBy(() -> studentRepository.saveAndFlush(newStudent("Bia", "ana@escola.com", "2026002")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .message().containsIgnoringCase("uk_person_email_address");
    }

    @Test
    @DisplayName("Matrícula duplicada viola uk_student_student_number")
    void shouldRejectDuplicatedStudentNumber() {
        studentRepository.saveAndFlush(newStudent("Ana", "ana@escola.com", "2026001"));

        assertThatThrownBy(() -> studentRepository.saveAndFlush(newStudent("Bia", "bia@escola.com", "2026001")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .message().containsIgnoringCase("uk_student_student_number");
    }

    @Test
    @DisplayName("replaceAddresses remove os endereços que saíram da lista (orphanRemoval)")
    void shouldRemoveOrphanAddressesOnReplace() {
        Student student = newStudent("Ana", "ana@escola.com", "2026001");
        student.replaceAddresses(List.of(address("Rua A"), address("Rua B")));
        Long id = studentRepository.saveAndFlush(student).getId();

        Student managed = studentRepository.findById(id).orElseThrow();
        managed.replaceAddresses(List.of(address("Rua C")));
        studentRepository.saveAndFlush(managed);

        assertThat(countAddresses(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("Excluir o aluno remove todos os seus endereços (cascata)")
    void shouldDeleteAddressesWithStudent() {
        Student student = newStudent("Ana", "ana@escola.com", "2026001");
        student.replaceAddresses(List.of(address("Rua A"), address("Rua B")));
        Long id = studentRepository.saveAndFlush(student).getId();

        studentRepository.delete(studentRepository.findById(id).orElseThrow());
        studentRepository.flush();

        assertThat(countRows("person", id)).isZero();
        assertThat(countAddresses(id)).isZero();
    }

    @Test
    @DisplayName("Endereços voltam na ordem enviada, e endereços idênticos são gravados como enviados")
    void shouldKeepAddressOrderAndDuplicates() {
        Student student = newStudent("Ana", "ana@escola.com", "2026001");
        student.replaceAddresses(List.of(address("Rua B"), address("Rua A"), address("Rua A")));
        Long id = studentRepository.saveAndFlush(student).getId();
        entityManager.clear();

        Student reloaded = studentRepository.findWithAddressesById(id).orElseThrow();

        assertThat(reloaded.getAddresses()).extracting(Address::getStreet).containsExactly("Rua B", "Rua A", "Rua A");
    }

    @Test
    @DisplayName("Listagens trazem os endereços já carregados e filtram por status")
    void shouldLoadAddressesAndFilterByStatus() {
        studentRepository.save(newStudent("Ana", "ana@escola.com", "2026001"));
        Student inactive = newStudent("Bia", "bia@escola.com", "2026002");
        inactive.setStatus(Status.DISABLE);
        studentRepository.saveAndFlush(inactive);
        entityManager.clear();

        List<Student> all = studentRepository.findAllWithAddresses();
        List<Student> disabled = studentRepository.findAllWithAddressesByStatus(Status.DISABLE);

        assertThat(all).hasSize(2);
        assertThat(all).allSatisfy(student -> assertThat(isAddressesLoaded(student)).isTrue());
        assertThat(disabled).extracting(Student::getName).containsExactly("Bia");
    }

    @Test
    @DisplayName("findWithAddressesById carrega os endereços na mesma consulta")
    void shouldLoadAddressesWhenFindingById() {
        Long id = studentRepository.saveAndFlush(newStudent("Ana", "ana@escola.com", "2026001")).getId();
        entityManager.clear();

        Student student = studentRepository.findWithAddressesById(id).orElseThrow();

        assertThat(isAddressesLoaded(student)).isTrue();
        assertThat(student.getAddresses()).hasSize(1);
    }

    private boolean isAddressesLoaded(Student student) {
        return entityManager.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(student, "addresses");
    }

    private long countRows(String table, Long id) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table + " where id = :id")
                .setParameter("id", id)
                .getSingleResult()).longValue();
    }

    private long countAddresses(Long personId) {
        return ((Number) entityManager.createNativeQuery("select count(*) from address where person_id = :id")
                .setParameter("id", personId)
                .getSingleResult()).longValue();
    }
}
