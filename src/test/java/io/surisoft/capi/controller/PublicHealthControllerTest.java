package io.surisoft.capi.controller;

import io.surisoft.capi.service.ConsulNodeDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class PublicHealthControllerTest {

    private final PublicHealthController controllerUnderTest = new PublicHealthController();

    @Test
    void consulConnected_returns200OK() {
        try (MockedStatic<ConsulNodeDiscovery> mocked = mockStatic(ConsulNodeDiscovery.class)) {
            mocked.when(ConsulNodeDiscovery::isConnectedToConsul).thenReturn(true);

            ResponseEntity<String> response = controllerUnderTest.amIHealthy();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void consulNotConnected_returns503ServiceUnavailable() {
        try (MockedStatic<ConsulNodeDiscovery> mocked = mockStatic(ConsulNodeDiscovery.class)) {
            mocked.when(ConsulNodeDiscovery::isConnectedToConsul).thenReturn(false);

            ResponseEntity<String> response = controllerUnderTest.amIHealthy();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
