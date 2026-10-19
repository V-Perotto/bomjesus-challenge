package br.com.bomjesus.academic.config;

import br.com.bomjesus.academic.service.ProfessorService;
import br.com.bomjesus.academic.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NoDemoDataTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ProfessorService professorService;

    @Test
    @DisplayName("Sem o perfil demo, o banco inicia vazio (FR-027)")
    void shouldStartWithEmptyDatabase() {
        assertThat(studentService.findAll(null)).isEmpty();
        assertThat(professorService.findAll(null)).isEmpty();
    }
}
