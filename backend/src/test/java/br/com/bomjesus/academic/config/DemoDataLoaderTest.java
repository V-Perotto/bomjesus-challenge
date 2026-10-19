package br.com.bomjesus.academic.config;

import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.service.ProfessorService;
import br.com.bomjesus.academic.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
class DemoDataLoaderTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ProfessorService professorService;

    @Test
    @DisplayName("Perfil demo carrega 3 alunos (1 inativo) e 2 professores, um aluno com 2 endereços")
    void shouldLoadDemoData() {
        assertThat(studentService.findAll(null)).hasSize(3);
        assertThat(studentService.findAll(Status.DISABLE)).hasSize(1);
        assertThat(professorService.findAll(null)).hasSize(2);
        assertThat(studentService.findAll(null))
                .anySatisfy(student -> assertThat(student.addresses()).hasSize(2));
    }
}
