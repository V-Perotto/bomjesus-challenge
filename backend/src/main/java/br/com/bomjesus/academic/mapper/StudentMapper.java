package br.com.bomjesus.academic.mapper;

import br.com.bomjesus.academic.dto.StudentRequest;
import br.com.bomjesus.academic.dto.StudentResponse;
import br.com.bomjesus.academic.model.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Os endereços nunca são mapeados aqui: o Service os liga ao aluno via {@code replaceAddresses}.
 */
@Mapper(config = CentralMapperConfig.class, uses = AddressMapper.class)
public interface StudentMapper {

    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "status", defaultValue = "ACTIVE")
    Student toEntity(StudentRequest request);

    /** {@code status} nulo mantém o status atual (BR-07); os demais campos são sobrescritos. */
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "status", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(StudentRequest request, @MappingTarget Student student);

    StudentResponse toResponse(Student student);
}
