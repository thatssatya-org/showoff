package com.samsepiol.portfolio.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BeszelPairingRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsOnlyOneNonBlankTokenField() throws Exception {
        var request = BeszelPairingRequest.from(objectMapper.readTree("{\"token\":\"beszel-secret\"}"));

        assertThat(request.getToken()).isEqualTo("beszel-secret");
    }

    @Test
    void rejectsCallerControlledProviderFields() throws Exception {
        assertThatThrownBy(() -> BeszelPairingRequest.from(
                objectMapper.readTree("{\"token\":\"x\",\"baseUrl\":\"https://untrusted.invalid\"}")))
                .isInstanceOf(BeszelPairingRequestException.class);
    }
}
