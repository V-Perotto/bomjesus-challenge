package br.com.bomjesus.academic.config;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Normaliza todo texto recebido em JSON (FR-014): remove espaços nas extremidades e trata
 * texto vazio como ausente ({@code null}), para que {@code @NotBlank} e campos opcionais
 * funcionem de forma consistente.
 */
@JacksonComponent
public class TrimmingStringDeserializer extends StdScalarDeserializer<String> {

    public TrimmingStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        String value = _parseString(parser, context, this);
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
