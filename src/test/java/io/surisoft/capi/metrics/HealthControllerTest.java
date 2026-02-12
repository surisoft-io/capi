package io.surisoft.capi.metrics;

import io.surisoft.capi.service.ConsulNodeDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private final HealthController controllerUnderTest = new HealthController();

    @Test
    void consulConnected_returnsHealthUp() {
        try (MockedStatic<ConsulNodeDiscovery> mocked = mockStatic(ConsulNodeDiscovery.class)) {
            mocked.when(ConsulNodeDiscovery::isConnectedToConsul).thenReturn(true);

            Health health = controllerUnderTest.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
        }
    }

    @Test
    void consulNotConnected_returnsHealthDownWithDetail() {
        try (MockedStatic<ConsulNodeDiscovery> mocked = mockStatic(ConsulNodeDiscovery.class)) {
            mocked.when(ConsulNodeDiscovery::isConnectedToConsul).thenReturn(false);

            Health health = controllerUnderTest.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("reason", "Consul not available");
        }
    }
}
