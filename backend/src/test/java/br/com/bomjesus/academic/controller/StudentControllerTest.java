package br.com.bomjesus.academic.controller;

import br.com.bomjesus.academic.dto.AddressResponse;
import br.com.bomjesus.academic.dto.StudentRequest;
import br.com.bomjesus.academic.dto.StudentResponse;
import br.com.bomjesus.academic.exception.ConflictException;
import br.com.bomjesus.academic.exception.ResourceNotFoundException;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    private static final String BASE_URL = "/api/students";
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    private static final String VALID_BODY = """
            {
              "name": "Ana Souza",
              "phoneNumber": "+55 (41) 99999-0001",
              "emailAddress": "ana@escola.com",
              "studentNumber": "2026001",
              "photo": "https://i.pravatar.cc/150?img=5",
              "addresses": [
                {"street": "Rua XV de Novembro, 100", "city": "Curitiba", "state": "PR",
                 "zipCode": "80020-310", "country": "Brasil"}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    @DisplayName("POST válido → 201 com Location e o aluno criado")
    void shouldCreateStudent() throws Exception {
        when(studentService.create(any())).thenReturn(response(1L, "https://i.pravatar.cc/150?img=5"));

        postJson(VALID_BODY)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/students/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.addresses[0].city").value("Curitiba"))
                .andExpect(jsonPath("$.addresses[0].id").doesNotExist());
    }

    @Test
    @DisplayName("POST com vários campos inválidos → 400 listando todos os campos")
    void shouldRejectInvalidStudentListingAllFields() throws Exception {
        String body = """
                {"name": "Ana Souza", "phoneNumber": "+55 41 99999-0001", "emailAddress": "nao-e-email",
                 "studentNumber": "2026001",
                 "addresses": [{"street": "Rua A", "city": "", "state": "PR", "zipCode": "80020-310",
                                "country": "Brasil"}]}
                """;

        postJson(body)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.instance").value(BASE_URL))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[*].field", hasItems("emailAddress", "addresses[0].city")))
                .andExpect(jsonPath("$.errors[?(@.field == 'addresses[0].city')].message").value("é obrigatório"));
        verify(studentService, never()).create(any());
    }

    @Test
    @DisplayName("POST sem endereços → 400 no campo addresses")
    void shouldRejectStudentWithoutAddresses() throws Exception {
        String body = VALID_BODY.replaceFirst("(?s)\"addresses\": \\[.*]", "\"addresses\": []");

        postJson(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("addresses"))
                .andExpect(jsonPath("$.errors[0].message").value("informe ao menos um endereço"));
    }

    @Test
    @DisplayName("POST com nome só de espaços → 400 (texto é normalizado antes da validação)")
    void shouldTreatBlankNameAsMissing() throws Exception {
        postJson(VALID_BODY.replace("\"Ana Souza\"", "\"   \""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @DisplayName("POST com JSON malformado ou corpo vazio → 400")
    void shouldRejectMalformedOrEmptyBody() throws Exception {
        postJson("{\"name\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
        postJson("")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST com status fora do enum → 400 no campo status")
    void shouldRejectUnknownStatusInBody() throws Exception {
        postJson(VALID_BODY.replace("\"studentNumber\"", "\"status\": \"FOO\", \"studentNumber\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("ACTIVE ou DISABLE")));
    }

    @Test
    @DisplayName("POST com photo inválida → 400; sem photo → 201 com photo nula")
    void shouldValidateOptionalPhoto() throws Exception {
        postJson(VALID_BODY.replace("https://i.pravatar.cc/150?img=5", "ftp://x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("photo"));

        when(studentService.create(any())).thenReturn(response(2L, null));
        postJson(VALID_BODY.replace("\"photo\": \"https://i.pravatar.cc/150?img=5\",", ""))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"photo\":null")));
    }

    @Test
    @DisplayName("POST ignora id e campos desconhecidos no corpo (FR-015)")
    void shouldIgnoreIdAndUnknownFields() throws Exception {
        when(studentService.create(any())).thenReturn(response(1L, null));

        postJson(VALID_BODY.replace("\"name\"", "\"id\": 99, \"foo\": 1, \"name\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST com conflito → 409 com o campo em conflito")
    void shouldReturnConflictWithField() throws Exception {
        when(studentService.create(any())).thenThrow(new ConflictException("emailAddress", "e-mail já cadastrado",
                "Já existe uma pessoa cadastrada com o e-mail ana@escola.com."));

        postJson(VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.errors[0].field").value("emailAddress"))
                .andExpect(jsonPath("$.errors[0].message").value("e-mail já cadastrado"));
    }

    @Test
    @DisplayName("Violação de integridade no banco → 409 (gravações simultâneas)")
    void shouldMapDataIntegrityViolationToConflict() throws Exception {
        when(studentService.create(any())).thenThrow(new DataIntegrityViolationException("uk_person_email_address"));

        postJson(VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Os dados conflitam com um registro existente."));
    }

    @Test
    @DisplayName("Erro inesperado → 500 genérico, sem detalhes internos")
    void shouldHideInternalDetailsOnUnexpectedError() throws Exception {
        when(studentService.create(any())).thenThrow(new RuntimeException("boom"));

        postJson(VALID_BODY)
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(content().string(not(containsString("boom"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    @DisplayName("GET lista os alunos e repassa o filtro de status")
    void shouldListStudentsAndForwardStatusFilter() throws Exception {
        when(studentService.findAll(isNull())).thenReturn(List.of(response(1L, null)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        mockMvc.perform(get(BASE_URL).param("status", "DISABLE"))
                .andExpect(status().isOk());
        verify(studentService).findAll(Status.DISABLE);
    }

    @Test
    @DisplayName("Filtro de status: minúsculas → 400; repetido → vale o primeiro valor")
    void shouldValidateStatusFilter() throws Exception {
        mockMvc.perform(get(BASE_URL).param("status", "active"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        mockMvc.perform(get(BASE_URL).param("status", "FOO"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(BASE_URL).param("status", "DISABLE", "ACTIVE"))
                .andExpect(status().isOk());
        verify(studentService).findAll(eq(Status.DISABLE));
        verify(studentService, never()).findAll(Status.ACTIVE);
    }

    @Test
    @DisplayName("GET por id: 200 existente, 404 inexistente, 400 id não numérico")
    void shouldFindById() throws Exception {
        when(studentService.findById(1L)).thenReturn(response(1L, null));
        when(studentService.findById(99L)).thenThrow(new ResourceNotFoundException("Aluno com id 99 não encontrado."));

        mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Aluno com id 99 não encontrado."));
        mockMvc.perform(get(BASE_URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    @DisplayName("PUT: 200 atualizado, 404 inexistente")
    void shouldUpdateStudent() throws Exception {
        when(studentService.update(eq(1L), any(StudentRequest.class))).thenReturn(response(1L, null));
        when(studentService.update(eq(99L), any(StudentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Aluno com id 99 não encontrado."));

        mockMvc.perform(put(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(put(BASE_URL + "/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE: 204 sem corpo, 404 inexistente")
    void shouldDeleteStudent() throws Exception {
        doThrow(new ResourceNotFoundException("Aluno com id 99 não encontrado.")).when(studentService).delete(99L);

        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(delete(BASE_URL + "/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Método não suportado → 405 e tipo de mídia não suportado → 415, em problem+json")
    void shouldRejectUnsupportedMethodAndMediaType() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Método não permitido"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.TEXT_PLAIN).content("texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Tipo de mídia não suportado"));
    }

    @Test
    @DisplayName("Accept sem JSON → 406 sem corpo")
    void shouldRejectNonJsonAccept() throws Exception {
        when(studentService.findAll(isNull())).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL).accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("CORS: pré-checagem de origem permitida → 200; Location exposto")
    void shouldAllowConfiguredOrigin() throws Exception {
        mockMvc.perform(options(BASE_URL + "/1")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));

        when(studentService.findAll(isNull())).thenReturn(List.of());
        mockMvc.perform(get(BASE_URL).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
    }

    @Test
    @DisplayName("CORS: origem não permitida → 403")
    void shouldRejectUnknownOrigin() throws Exception {
        mockMvc.perform(options(BASE_URL + "/1")
                        .header(HttpHeaders.ORIGIN, "http://evil.test")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isForbidden());
    }

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static StudentResponse response(Long id, String photo) {
        return new StudentResponse(id, "Ana Souza", "+55 (41) 99999-0001", "ana@escola.com", Status.ACTIVE,
                "2026001", photo,
                List.of(new AddressResponse("Rua XV de Novembro, 100", "Curitiba", "PR", "80020-310", "Brasil")));
    }
}
