package br.com.bomjesus.academic.dto;

import br.com.bomjesus.academic.model.Status;

import java.math.BigDecimal;
import java.util.List;

public record ProfessorResponse(
        Long id,
        String name,
        String phoneNumber,
        String emailAddress,
        Status status,
        BigDecimal salary,
        List<AddressResponse> addresses) {
}
