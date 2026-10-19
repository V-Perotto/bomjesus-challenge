package br.com.bomjesus.academic.mapper;

import br.com.bomjesus.academic.dto.AddressRequest;
import br.com.bomjesus.academic.dto.AddressResponse;
import br.com.bomjesus.academic.model.Address;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Converte endereços. O vínculo com a pessoa é feito só por {@code Person#replaceAddresses}.
 */
@Mapper(config = CentralMapperConfig.class)
public interface AddressMapper {

    Address toEntity(AddressRequest request);

    List<Address> toEntities(List<AddressRequest> requests);

    AddressResponse toResponse(Address address);
}
