package br.com.bomjesus.academic.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Configuração comum dos mappers: beans Spring injetados por construtor e build quebrado se
 * algum campo de destino ficar sem mapeamento.
 */
@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CentralMapperConfig {
}
