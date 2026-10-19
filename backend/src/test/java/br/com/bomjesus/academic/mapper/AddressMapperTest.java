package br.com.bomjesus.academic.mapper;

import br.com.bomjesus.academic.dto.AddressRequest;
import br.com.bomjesus.academic.dto.AddressResponse;
import br.com.bomjesus.academic.model.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.bomjesus.academic.support.AddressTestData.address;
import static br.com.bomjesus.academic.support.AddressTestData.addressRequest;
import static org.assertj.core.api.Assertions.assertThat;

class AddressMapperTest {

    private final AddressMapper mapper = new AddressMapperImpl();

    @Test
    @DisplayName("toEntity copia os campos sem preencher id nem pessoa")
    void shouldMapRequestToEntityWithoutIdAndPerson() {
        AddressRequest request = addressRequest();

        Address entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getPerson()).isNull();
        assertThat(entity.getStreet()).isEqualTo(request.street());
        assertThat(entity.getCity()).isEqualTo(request.city());
        assertThat(entity.getState()).isEqualTo(request.state());
        assertThat(entity.getZipCode()).isEqualTo(request.zipCode());
        assertThat(entity.getCountry()).isEqualTo(request.country());
    }

    @Test
    @DisplayName("toEntities preserva a ordem enviada")
    void shouldPreserveOrderWhenMappingList() {
        List<Address> entities = mapper.toEntities(List.of(addressRequest("Rua A"), addressRequest("Rua B")));

        assertThat(entities).extracting(Address::getStreet).containsExactly("Rua A", "Rua B");
    }

    @Test
    @DisplayName("toResponse copia os 5 campos")
    void shouldMapEntityToResponse() {
        AddressResponse response = mapper.toResponse(address("Rua C"));

        assertThat(response).isEqualTo(new AddressResponse("Rua C", "Curitiba", "PR", "80020-310", "Brasil"));
    }
}
