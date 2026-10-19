package br.com.bomjesus.academic;

import br.com.bomjesus.academic.repository.ProfessorRepository;
import br.com.bomjesus.academic.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo de professores, isolamento entre tipos e e-mail único entre todas as pessoas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProfessorApiIntegrationTest {

    private static final String PROFESSORS = "/api/professors";
    private static final String STUDENTS = "/api/students";
    private static final String ADDRESSES = """
            [{"street": "Av. Sete de Setembro, 2000", "city": "Curitiba", "state": "PR",
              "zipCode": "80230-010", "country": "Brasil"}]""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAll();
        professorRepository.deleteAll();
    }

    @Test
    @DisplayName("Fluxo completo: criar (salário com 2 casas) → atualizar → excluir")
    void shouldRunFullProfessorLifecycle() throws Exception {
        long id = create(PROFESSORS, professorBody("Helena@Escola.com", "8500"));

        mockMvc.perform(get(PROFESSORS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("helena@escola.com"))
                .andExpect(content().string(containsString("\"salary\":8500.00")));

        mockMvc.perform(put(PROFESSORS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(professorBody("helena@escola.com", "9200.5").replace("\"name\"",
                                "\"status\": \"DISABLE\", \"name\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLE"))
                .andExpect(content().string(containsString("\"salary\":9200.50")));

        mockMvc.perform(delete(PROFESSORS + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(PROFESSORS + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Id de aluno em /api/professors → 404 em GET, PUT e DELETE")
    void shouldIsolateResourcesByType() throws Exception {
        long studentId = create(STUDENTS, studentBody("ana@escola.com"));

        mockMvc.perform(get(PROFESSORS + "/" + studentId)).andExpect(status().isNotFound());
        mockMvc.perform(put(PROFESSORS + "/" + studentId).contentType(MediaType.APPLICATION_JSON)
                        .content(professorBody("x@escola.com", "1000")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(PROFESSORS + "/" + studentId)).andExpect(status().isNotFound());
        mockMvc.perform(get(STUDENTS + "/" + studentId)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("E-mail é único entre alunos e professores (criação e atualização)")
    void shouldEnforceEmailUniquenessAcrossPeople() throws Exception {
        create(STUDENTS, studentBody("ana@escola.com"));
        long professorId = create(PROFESSORS, professorBody("helena@escola.com", "8500"));

        mockMvc.perform(post(PROFESSORS).contentType(MediaType.APPLICATION_JSON)
                        .content(professorBody("ANA@escola.com", "8500")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("emailAddress"));

        mockMvc.perform(put(PROFESSORS + "/" + professorId).contentType(MediaType.APPLICATION_JSON)
                        .content(professorBody("ana@escola.com", "8500")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("emailAddress"));
    }

    private long create(String url, String body) throws Exception {
        String json = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll("(?s)^\\{\"id\":(\\d+),.*$", "$1"));
    }

    private static String professorBody(String email, String salary) {
        return """
                {"name": "Helena Rocha", "phoneNumber": "+55 41 3333-0001", "emailAddress": "%s",
                 "salary": %s, "addresses": %s}
                """.formatted(email, salary, ADDRESSES);
    }

    private static String studentBody(String email) {
        return """
                {"name": "Ana Souza", "phoneNumber": "+55 41 99999-0001", "emailAddress": "%s",
                 "studentNumber": "2026001", "addresses": %s}
                """.formatted(email, ADDRESSES);
    }
}
