package br.com.bomjesus.academic.dto;

import br.com.bomjesus.academic.model.Status;

import java.util.List;

public record StudentResponse(
        Long id,
        String name,
        String phoneNumber,
        String emailAddress,
        Status status,
        String studentNumber,
        String photo,
        List<AddressResponse> addresses) {
}
