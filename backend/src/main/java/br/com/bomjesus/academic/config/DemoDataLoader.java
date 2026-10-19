package br.com.bomjesus.academic.config;

import br.com.bomjesus.academic.dto.AddressRequest;
import br.com.bomjesus.academic.dto.ProfessorRequest;
import br.com.bomjesus.academic.dto.StudentRequest;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.service.ProfessorService;
import br.com.bomjesus.academic.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados de demonstração (FR-028), carregados só com o perfil {@code demo}
 * ({@code SPRING_PROFILES_ACTIVE=demo}). Passa pelos Services, portanto respeita as validações.
 */
@Component
@Profile("demo")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final StudentService studentService;
    private final ProfessorService professorService;

    public DemoDataLoader(StudentService studentService, ProfessorService professorService) {
        this.studentService = studentService;
        this.professorService = professorService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<StudentRequest> students = List.of(
                new StudentRequest("Ana Souza", "+55 (41) 99999-0001", "ana.souza@escola.com", Status.ACTIVE,
                        "2026001", "https://i.pravatar.cc/150?img=5",
                        List.of(curitiba("Rua XV de Novembro, 100", "80020-310"),
                                curitiba("Rua das Flores, 25", "80010-020"))),
                new StudentRequest("Bruno Martins", "+55 (41) 99999-0002", "bruno.martins@escola.com", Status.ACTIVE,
                        "2026002", "https://i.pravatar.cc/150?img=12",
                        List.of(curitiba("Av. Batel, 1500", "80420-090"))),
                new StudentRequest("Carla Mendes", "+55 (41) 99999-0003", "carla.mendes@escola.com", Status.DISABLE,
                        "2026003", "https://i.pravatar.cc/150?img=32",
                        List.of(new AddressRequest("Rua Joinville, 300", "São José dos Pinhais", "PR", "83005-000",
                                "Brasil"))));
        List<ProfessorRequest> professors = List.of(
                new ProfessorRequest("Helena Rocha", "+55 (41) 3333-0001", "helena.rocha@escola.com", Status.ACTIVE,
                        new BigDecimal("8500.00"), List.of(curitiba("Av. Sete de Setembro, 2000", "80230-010"))),
                new ProfessorRequest("Marcos Pereira", "+55 (41) 3333-0002", "marcos.pereira@escola.com",
                        Status.DISABLE, new BigDecimal("9200.00"),
                        List.of(curitiba("Rua Marechal Deodoro, 500", "80010-010"))));

        students.forEach(studentService::create);
        professors.forEach(professorService::create);
        log.info("Dados de demonstração carregados: {} alunos e {} professores.", students.size(), professors.size());
    }

    private static AddressRequest curitiba(String street, String zipCode) {
        return new AddressRequest(street, "Curitiba", "PR", zipCode, "Brasil");
    }
}
