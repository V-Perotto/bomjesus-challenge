package br.com.bomjesus.academic.dto;

import br.com.bomjesus.academic.model.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProfessorRequest(
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

        @NotNull(message = "{validation.required}")
        @DecimalMin(value = "0.01", message = "{validation.salary.min}")
        @DecimalMax(value = "1000000.00", message = "{validation.salary.max}")
        @Digits(integer = 7, fraction = 2, message = "{validation.salary.digits}")
        BigDecimal salary,

        @NotEmpty(message = "{validation.addresses}")
        List<@Valid AddressRequest> addresses) {
}
