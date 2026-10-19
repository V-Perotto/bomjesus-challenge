package br.com.bomjesus.academic;

import br.com.bomjesus.academic.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo de alunos contra a aplicação real e o banco H2 (isolado por contexto).
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentApiIntegrationTest {

    private static final String BASE_URL = "/api/students";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAll();
    }

    @Test
    @DisplayName("Fluxo completo: criar → buscar → listar → atualizar → filtrar → excluir")
    void shouldRunFullStudentLifecycle() throws Exception {
        long id = createStudent("Ana Souza", "Ana@Escola.com", "ab-2026", addresses("Rua A", "Rua B"));

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("ana@escola.com"))
                .andExpect(jsonPath("$.studentNumber").value("AB-2026"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.addresses", hasSize(2)))
                .andExpect(jsonPath("$.addresses[0].id").doesNotExist());

        mockMvc.perform(get(BASE_URL)).andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(put(BASE_URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ana Souza", "ana@escola.com", "AB-2026", "\"DISABLE\"", addresses("Rua C"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLE"))
                .andExpect(jsonPath("$.addresses[*].street").value(contains("Rua C")));

        mockMvc.perform(get(BASE_URL).param("status", "DISABLE")).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get(BASE_URL).param("status", "ACTIVE")).andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(delete(BASE_URL + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete(BASE_URL + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE_URL + "/" + id)).andExpect(status().isNotFound());
        assertThat(countAddresses(id)).isZero();
    }

    @Test
    @DisplayName("PUT com e-mail ou matrícula de outro aluno → 409 com o campo")
    void shouldRejectUpdateWithDataOfAnotherStudent() throws Exception {
        createStudent("Ana Souza", "ana@escola.com", "2026001", addresses("Rua A"));
        long secondId = createStudent("Bruno Lima", "bruno@escola.com", "2026002", addresses("Rua B"));

        mockMvc.perform(put(BASE_URL + "/" + secondId).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Bruno Lima", "ANA@escola.com", "2026002", null, addresses("Rua B"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("emailAddress"));

        mockMvc.perform(put(BASE_URL + "/" + secondId).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Bruno Lima", "bruno@escola.com", "2026001", null, addresses("Rua B"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("studentNumber"));

        mockMvc.perform(get(BASE_URL + "/" + secondId))
                .andExpect(jsonPath("$.emailAddress").value("bruno@escola.com"));
    }

    @Test
    @DisplayName("Endereços idênticos são aceitos e voltam na ordem enviada")
    void shouldAcceptDuplicatedAddressesInOrder() throws Exception {
        long id = createStudent("Ana Souza", "ana@escola.com", "2026001", addresses("Rua Z", "Rua A", "Rua A"));

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(jsonPath("$.addresses[*].street")
                        .value(contains("Rua Z", "Rua A", "Rua A")));
    }

    @Test
    @DisplayName("Rota inexistente sob /api → 404 em problem+json")
    void shouldReturnProblemDetailForUnknownRoute() throws Exception {
        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    private long createStudent(String name, String email, String studentNumber, String addresses) throws Exception {
        MvcResult result = postStudent(body(name, email, studentNumber, null, addresses))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll("(?s)^\\{\"id\":(\\d+),.*$", "$1"));
    }

    private ResultActions postStudent(String body) throws Exception {
        return mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long countAddresses(long personId) {
        Long count = jdbcTemplate.queryForObject("select count(*) from address where person_id = ?", Long.class,
                personId);
        return count == null ? 0 : count;
    }

    private static String body(String name, String email, String studentNumber, String status, String addresses) {
        return """
                {"name": "%s", "phoneNumber": "+55 (41) 99999-0001", "emailAddress": "%s",
                 "studentNumber": "%s", "status": %s, "addresses": %s}
                """.formatted(name, email, studentNumber, status, addresses);
    }

    private static String addresses(String... streets) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < streets.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("""
                    {"street": "%s", "city": "Curitiba", "state": "PR", "zipCode": "80020-310", "country": "Brasil"}
                    """.formatted(streets[i]).strip());
        }
        return json.append(']').toString();
    }
}
