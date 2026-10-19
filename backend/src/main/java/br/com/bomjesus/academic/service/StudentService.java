package br.com.bomjesus.academic.service;

import br.com.bomjesus.academic.dto.StudentRequest;
import br.com.bomjesus.academic.dto.StudentResponse;
import br.com.bomjesus.academic.exception.ConflictException;
import br.com.bomjesus.academic.exception.ResourceNotFoundException;
import br.com.bomjesus.academic.mapper.AddressMapper;
import br.com.bomjesus.academic.mapper.StudentMapper;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.model.Student;
import br.com.bomjesus.academic.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final AddressMapper addressMapper;
    private final EmailUniquenessGuard emailUniquenessGuard;

    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper,
                          AddressMapper addressMapper, EmailUniquenessGuard emailUniquenessGuard) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
        this.addressMapper = addressMapper;
        this.emailUniquenessGuard = emailUniquenessGuard;
    }

    /** @param status filtro opcional; {@code null} lista todos */
    @Transactional(readOnly = true)
    public List<StudentResponse> findAll(Status status) {
        List<Student> students = status == null
                ? studentRepository.findAllWithAddresses()
                : studentRepository.findAllWithAddressesByStatus(status);
        return students.stream()
                .sorted(PersonOrdering.byNameThenId())
                .map(studentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return studentRepository.findWithAddressesById(id)
                .map(studentMapper::toResponse)
                .orElseThrow(() -> notFound(id));
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        String email = normalizeEmail(request.emailAddress());
        String studentNumber = normalizeStudentNumber(request.studentNumber());
        emailUniquenessGuard.assertAvailable(email, null);
        assertStudentNumberAvailable(studentNumber, null);

        Student student = studentMapper.toEntity(request);
        applyNormalizedValues(student, email, studentNumber);
        student.replaceAddresses(addressMapper.toEntities(request.addresses()));
        return studentMapper.toResponse(studentRepository.save(student));
    }

    /**
     * A unicidade é checada antes de alterar a entidade gerenciada: alterá-la antes faria o
     * Hibernate gravar o valor duplicado (auto-flush) e o conflito perderia o campo.
     */
    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id).orElseThrow(() -> notFound(id));
        String email = normalizeEmail(request.emailAddress());
        String studentNumber = normalizeStudentNumber(request.studentNumber());
        emailUniquenessGuard.assertAvailable(email, id);
        assertStudentNumberAvailable(studentNumber, id);

        studentMapper.updateEntity(request, student);
        applyNormalizedValues(student, email, studentNumber);
        student.replaceAddresses(addressMapper.toEntities(request.addresses()));
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id).orElseThrow(() -> notFound(id));
        studentRepository.delete(student);
    }

    private void assertStudentNumberAvailable(String studentNumber, Long currentId) {
        boolean taken = currentId == null
                ? studentRepository.existsByStudentNumber(studentNumber)
                : studentRepository.existsByStudentNumberAndIdNot(studentNumber, currentId);
        if (taken) {
            throw new ConflictException("studentNumber", "matrícula já cadastrada",
                    "Já existe um aluno com a matrícula %s.".formatted(studentNumber));
        }
    }

    private static void applyNormalizedValues(Student student, String email, String studentNumber) {
        student.setEmailAddress(email);
        student.setStudentNumber(studentNumber);
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private static String normalizeStudentNumber(String studentNumber) {
        return studentNumber.toUpperCase(Locale.ROOT);
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Aluno com id %d não encontrado.".formatted(id));
    }
}
