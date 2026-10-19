package br.com.bomjesus.academic.dto;

import br.com.bomjesus.academic.model.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StudentRequest(
        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 100, message = "{validation.size}")
        String name,

        @NotBlank(message = "{validation.required}")
        @Size(max = 20, message = "{validation.maxSize}")
        @Pattern(regexp = "^(?=(?:\\D*\\d){8,15}\\D*$)\\+?[0-9 ()-]+$", message = "{validation.phone}")
        String phoneNumber,

        @NotBlank(message = "{validation.required}")
        @Email(regexp = ".+@.+\\..+", message = "{validation.email}")
        @Size(max = 150, message = "{validation.maxSize}")
        String emailAddress,

        Status status,

        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^[A-Za-z0-9-]{1,20}$", message = "{validation.studentNumber}")
        String studentNumber,

        @Size(max = 500, message = "{validation.maxSize}")
        @Pattern(regexp = "^https?://\\S+$", message = "{validation.photo}")
        String photo,

        @NotEmpty(message = "{validation.addresses}")
        List<@Valid AddressRequest> addresses) {
}
