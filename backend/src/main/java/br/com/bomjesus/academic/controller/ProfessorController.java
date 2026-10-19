package br.com.bomjesus.academic.controller;

import br.com.bomjesus.academic.dto.ProfessorRequest;
import br.com.bomjesus.academic.dto.ProfessorResponse;
import br.com.bomjesus.academic.model.Status;
import br.com.bomjesus.academic.service.ProfessorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/professors")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    @GetMapping
    public ResponseEntity<List<ProfessorResponse>> findAll(@RequestParam(required = false) Status status) {
        return ResponseEntity.ok(professorService.findAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessorResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(professorService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProfessorResponse> create(@Valid @RequestBody ProfessorRequest request) {
        ProfessorResponse created = professorService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessorResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody ProfessorRequest request) {
        return ResponseEntity.ok(professorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        professorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
