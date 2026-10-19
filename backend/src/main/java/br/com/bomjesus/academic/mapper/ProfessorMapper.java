package br.com.bomjesus.academic.mapper;

import br.com.bomjesus.academic.dto.ProfessorRequest;
import br.com.bomjesus.academic.dto.ProfessorResponse;
import br.com.bomjesus.academic.model.Professor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Os endereços nunca são mapeados aqui: o Service os liga ao professor via {@code replaceAddresses}.
 */
@Mapper(config = CentralMapperConfig.class, uses = AddressMapper.class)
public interface ProfessorMapper {

    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "status", defaultValue = "ACTIVE")
    Professor toEntity(ProfessorRequest request);

    /** {@code status} nulo mantém o status atual (BR-07); os demais campos são sobrescritos. */
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "status", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(ProfessorRequest request, @MappingTarget Professor professor);

    ProfessorResponse toResponse(Professor professor);
}
