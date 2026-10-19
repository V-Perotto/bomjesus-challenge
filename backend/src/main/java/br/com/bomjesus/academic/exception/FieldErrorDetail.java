package br.com.bomjesus.academic.exception;

/**
 * Erro associado a um campo da requisição (ex.: {@code addresses[0].city}).
 */
public record FieldErrorDetail(String field, String message) {
}
