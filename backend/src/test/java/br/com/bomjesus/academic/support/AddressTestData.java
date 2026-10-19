package br.com.bomjesus.academic.support;

import br.com.bomjesus.academic.dto.AddressRequest;
import br.com.bomjesus.academic.model.Address;

public final class AddressTestData {

    public static final String DEFAULT_STREET = "Rua XV de Novembro, 100";

    private AddressTestData() {
    }

    public static AddressRequest addressRequest() {
        return addressRequest(DEFAULT_STREET);
    }

    public static AddressRequest addressRequest(String street) {
        return new AddressRequest(street, "Curitiba", "PR", "80020-310", "Brasil");
    }

    public static Address address(String street) {
        Address address = new Address();
        address.setStreet(street);
        address.setCity("Curitiba");
        address.setState("PR");
        address.setZipCode("80020-310");
        address.setCountry("Brasil");
        return address;
    }
}
