package br.com.bomjesus.academic.controller;

import br.com.bomjesus.academic.dto.AddressResponse;
import br.com.bomjesus.academic.dto.ProfessorRequest;
import br.com.bomjesus.academic.dto.ProfessorResponse;
import br.com.bomjesus.academic.exception.ConflictException;
import br.com.bomjesus.academic.exception.ResourceNotFoundException;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.service.ProfessorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
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

@WebMvcTest(ProfessorController.class)
class ProfessorControllerTest {

    private static final String BASE_URL = "/api/professors";

    private static final String VALID_BODY = """
            {
              "name": "Helena Rocha",
              "phoneNumber": "+55 41 3333-0001",
              "emailAddress": "helena@escola.com",
              "salary": 8500.00,
              "addresses": [
                {"street": "Av. Sete de Setembro, 2000", "city": "Curitiba", "state": "PR",
                 "zipCode": "80230-010", "country": "Brasil"}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessorService professorService;

    @Test
    @DisplayName("POST válido → 201 com Location e salário com 2 casas")
    void shouldCreateProfessor() throws Exception {
        when(professorService.create(any())).thenReturn(response(1L));

        postJson(VALID_BODY)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/professors/1")))
                .andExpect(content().string(containsString("\"salary\":8500.00")));
    }

    @ParameterizedTest(name = "salary = {0} → 400")
    @ValueSource(strings = {"0", "-1", "10.123", "1000000.01", "\"abc\""})
    @DisplayName("Salário inválido → 400 no campo salary")
    void shouldRejectInvalidSalary(String salary) throws Exception {
        postJson(VALID_BODY.replace("8500.00", salary))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("salary"));
        verify(professorService, never()).create(any());
    }

    @Test
    @DisplayName("Salário ausente → 400 'é obrigatório'")
    void shouldRequireSalary() throws Exception {
        postJson(VALID_BODY.replace("\"salary\": 8500.00,", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("salary"))
                .andExpect(jsonPath("$.errors[0].message").value("é obrigatório"));
    }

    @Test
    @DisplayName("POST com e-mail inválido e sem endereços → 400 listando os campos")
    void shouldRejectInvalidProfessor() throws Exception {
        String body = VALID_BODY.replace("helena@escola.com", "x")
                .replaceFirst("(?s)\"addresses\": \\[.*]", "\"addresses\": []");

        postJson(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                        "addresses", "emailAddress")));
    }

    @Test
    @DisplayName("POST ignora id e campos desconhecidos no corpo (FR-015)")
    void shouldIgnoreIdAndUnknownFields() throws Exception {
        when(professorService.create(any())).thenReturn(response(1L));

        postJson(VALID_BODY.replace("\"name\"", "\"id\": 99, \"foo\": 1, \"name\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST com e-mail de outra pessoa → 409 no campo emailAddress")
    void shouldReturnConflictWithField() throws Exception {
        when(professorService.create(any())).thenThrow(new ConflictException("emailAddress", "e-mail já cadastrado",
                "Já existe uma pessoa cadastrada com o e-mail helena@escola.com."));

        postJson(VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("emailAddress"));
    }

    @Test
    @DisplayName("Erro inesperado → 500 genérico, sem detalhes internos")
    void shouldHideInternalDetailsOnUnexpectedError() throws Exception {
        when(professorService.create(any())).thenThrow(new IllegalStateException("boom"));

        postJson(VALID_BODY)
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("boom"))));
    }

    @Test
    @DisplayName("GET lista professores, repassa o filtro e rejeita status inválido")
    void shouldListProfessors() throws Exception {
        when(professorService.findAll(isNull())).thenReturn(List.of(response(1L)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Helena Rocha"));
        mockMvc.perform(get(BASE_URL).param("status", "ACTIVE")).andExpect(status().isOk());
        verify(professorService).findAll(Status.ACTIVE);
        mockMvc.perform(get(BASE_URL).param("status", "FOO")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET por id: 200, 404 e 400 (id não numérico)")
    void shouldFindById() throws Exception {
        when(professorService.findById(1L)).thenReturn(response(1L));
        when(professorService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Professor com id 99 não encontrado."));

        mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Professor com id 99 não encontrado."));
        mockMvc.perform(get(BASE_URL + "/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT: 200 atualizado, 404 inexistente")
    void shouldUpdateProfessor() throws Exception {
        when(professorService.update(eq(1L), any(ProfessorRequest.class))).thenReturn(response(1L));
        when(professorService.update(eq(99L), any(ProfessorRequest.class)))
                .thenThrow(new ResourceNotFoundException("Professor com id 99 não encontrado."));

        mockMvc.perform(put(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
        mockMvc.perform(put(BASE_URL + "/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE: 204 sem corpo, 404 inexistente")
    void shouldDeleteProfessor() throws Exception {
        doThrow(new ResourceNotFoundException("Professor com id 99 não encontrado."))
                .when(professorService).delete(99L);

        mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete(BASE_URL + "/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("405, 415 e CORS da origem permitida")
    void shouldHandleMethodMediaTypeAndCors() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/1")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post(BASE_URL).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        mockMvc.perform(options(BASE_URL)
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"));
    }

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static ProfessorResponse response(Long id) {
        return new ProfessorResponse(id, "Helena Rocha", "+55 41 3333-0001", "helena@escola.com", Status.ACTIVE,
                new BigDecimal("8500.00"),
                List.of(new AddressResponse("Av. Sete de Setembro, 2000", "Curitiba", "PR", "80230-010", "Brasil")));
    }
}
