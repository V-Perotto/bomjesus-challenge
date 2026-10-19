package br.com.bomjesus.academic.service;

import br.com.bomjesus.academic.dto.ProfessorRequest;
import br.com.bomjesus.academic.dto.ProfessorResponse;
import br.com.bomjesus.academic.exception.ResourceNotFoundException;
import br.com.bomjesus.academic.mapper.AddressMapper;
import br.com.bomjesus.academic.mapper.ProfessorMapper;
import br.com.bomjesus.academic.model.Professor;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.repository.ProfessorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
public class ProfessorService {

    private static final int SALARY_SCALE = 2;

    private final ProfessorRepository professorRepository;
    private final ProfessorMapper professorMapper;
    private final AddressMapper addressMapper;
    private final EmailUniquenessGuard emailUniquenessGuard;

    public ProfessorService(ProfessorRepository professorRepository, ProfessorMapper professorMapper,
                            AddressMapper addressMapper, EmailUniquenessGuard emailUniquenessGuard) {
        this.professorRepository = professorRepository;
        this.professorMapper = professorMapper;
        this.addressMapper = addressMapper;
        this.emailUniquenessGuard = emailUniquenessGuard;
    }

    /** @param status filtro opcional; {@code null} lista todos */
    @Transactional(readOnly = true)
    public List<ProfessorResponse> findAll(Status status) {
        List<Professor> professors = status == null
                ? professorRepository.findAllWithAddresses()
                : professorRepository.findAllWithAddressesByStatus(status);
        return professors.stream()
                .sorted(PersonOrdering.byNameThenId())
                .map(professorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfessorResponse findById(Long id) {
        return professorRepository.findWithAddressesById(id)
                .map(professorMapper::toResponse)
                .orElseThrow(() -> notFound(id));
    }

    @Transactional
    public ProfessorResponse create(ProfessorRequest request) {
        String email = normalizeEmail(request.emailAddress());
        emailUniquenessGuard.assertAvailable(email, null);

        Professor professor = professorMapper.toEntity(request);
        applyNormalizedValues(professor, email);
        professor.replaceAddresses(addressMapper.toEntities(request.addresses()));
        return professorMapper.toResponse(professorRepository.save(professor));
    }

    /**
     * A unicidade é checada antes de alterar a entidade gerenciada: alterá-la antes faria o
     * Hibernate gravar o valor duplicado (auto-flush) e o conflito perderia o campo.
     */
    @Transactional
    public ProfessorResponse update(Long id, ProfessorRequest request) {
        Professor professor = professorRepository.findById(id).orElseThrow(() -> notFound(id));
        String email = normalizeEmail(request.emailAddress());
        emailUniquenessGuard.assertAvailable(email, id);

        professorMapper.updateEntity(request, professor);
        applyNormalizedValues(professor, email);
        professor.replaceAddresses(addressMapper.toEntities(request.addresses()));
        return professorMapper.toResponse(professorRepository.save(professor));
    }

    @Transactional
    public void delete(Long id) {
        Professor professor = professorRepository.findById(id).orElseThrow(() -> notFound(id));
        professorRepository.delete(professor);
    }

    /** E-mail em minúsculas (BR-01) e salário sempre com 2 casas decimais (BR-03). */
    private static void applyNormalizedValues(Professor professor, String email) {
        professor.setEmailAddress(email);
        professor.setSalary(normalizeSalary(professor.getSalary()));
    }

    private static BigDecimal normalizeSalary(BigDecimal salary) {
        return salary.setScale(SALARY_SCALE, RoundingMode.UNNECESSARY);
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Professor com id %d não encontrado.".formatted(id));
    }
}
