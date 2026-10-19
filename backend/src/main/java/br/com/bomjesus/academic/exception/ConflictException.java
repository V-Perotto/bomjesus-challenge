package br.com.bomjesus.academic.exception;

/**
 * Violação de unicidade de um campo (ex.: e-mail ou matrícula já cadastrados).
 */
public class ConflictException extends RuntimeException {

    private final String field;
    private final String fieldMessage;

    public ConflictException(String field, String fieldMessage, String detail) {
        super(detail);
        this.field = field;
        this.fieldMessage = fieldMessage;
    }

    public String getField() {
        return field;
    }

    public String getFieldMessage() {
        return fieldMessage;
    }
}
