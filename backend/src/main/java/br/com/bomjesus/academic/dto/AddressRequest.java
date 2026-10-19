package br.com.bomjesus.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "{validation.required}")
        @Size(max = 150, message = "{validation.maxSize}")
        String street,

        @NotBlank(message = "{validation.required}")
        @Size(max = 100, message = "{validation.maxSize}")
        String city,

        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 100, message = "{validation.size}")
        String state,

        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^[A-Za-z0-9 -]{3,20}$", message = "{validation.zipCode}")
        String zipCode,

        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 100, message = "{validation.size}")
        String country) {
}
