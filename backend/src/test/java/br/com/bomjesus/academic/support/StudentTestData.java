package br.com.bomjesus.academic.support;

import br.com.bomjesus.academic.dto.AddressRequest;
import br.com.bomjesus.academic.dto.StudentRequest;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.model.Student;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static br.com.bomjesus.academic.support.AddressTestData.DEFAULT_STREET;
import static br.com.bomjesus.academic.support.AddressTestData.address;
import static br.com.bomjesus.academic.support.AddressTestData.addressRequest;

public final class StudentTestData {

    public static final String EMAIL = "ana@escola.com";
    public static final String STUDENT_NUMBER = "2026001";
    public static final String PHOTO = "https://i.pravatar.cc/150?img=5";

    private StudentTestData() {
    }

    public static StudentRequest studentRequest() {
        return studentRequest(EMAIL, STUDENT_NUMBER);
    }

    public static StudentRequest studentRequest(String email, String studentNumber) {
        return studentRequest(email, studentNumber, null, List.of(addressRequest()));
    }

    public static StudentRequest studentRequest(String email, String studentNumber, Status status,
                                                List<AddressRequest> addresses) {
        return new StudentRequest("Ana Souza", "+55 (41) 99999-0001", email, status, studentNumber, PHOTO,
                addresses);
    }

    /** Aluno ainda não persistido (sem id), com um endereço ligado. */
    public static Student newStudent(String name, String email, String studentNumber) {
        Student student = new Student();
        student.setName(name);
        student.setPhoneNumber("+55 (41) 99999-0001");
        student.setEmailAddress(email);
        student.setStatus(Status.ACTIVE);
        student.setStudentNumber(studentNumber);
        student.setPhoto(PHOTO);
        student.replaceAddresses(List.of(address(DEFAULT_STREET)));
        return student;
    }

    /** Aluno "persistido" para testes com mocks: o id é atribuído por reflexão. */
    public static Student student(Long id) {
        return student(id, "Ana Souza");
    }

    public static Student student(Long id, String name) {
        Student student = newStudent(name, "aluno" + id + "@escola.com", "MAT-" + id);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
