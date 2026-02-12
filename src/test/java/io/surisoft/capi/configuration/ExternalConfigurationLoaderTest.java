package io.surisoft.capi.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalConfigurationLoaderTest {

    private static final String CONFIG_PROPERTY = "capi-configuration";

    @Mock
    private ApplicationEnvironmentPreparedEvent event;
    @Mock
    private ConfigurableEnvironment environment;

    private final ExternalConfigurationLoader loaderUnderTest = new ExternalConfigurationLoader();

    @AfterEach
    void tearDown() {
        System.clearProperty(CONFIG_PROPERTY);
    }

    @Test
    void nullSystemProperty_returnsEarlyNoException() {
        System.clearProperty(CONFIG_PROPERTY);
        when(event.getEnvironment()).thenReturn(environment);

        assertThatCode(() -> loaderUnderTest.onApplicationEvent(event))
                .doesNotThrowAnyException();

        verify(environment, never()).getPropertySources();
    }

    @Test
    void blankSystemProperty_returnsEarly() {
        System.setProperty(CONFIG_PROPERTY, "   ");
        when(event.getEnvironment()).thenReturn(environment);

        assertThatCode(() -> loaderUnderTest.onApplicationEvent(event))
                .doesNotThrowAnyException();

        verify(environment, never()).getPropertySources();
    }

    @Test
    void nonExistentFilePath_returnsEarly() {
        System.setProperty(CONFIG_PROPERTY, "/nonexistent/path/config.yml");
        when(event.getEnvironment()).thenReturn(environment);

        assertThatCode(() -> loaderUnderTest.onApplicationEvent(event))
                .doesNotThrowAnyException();

        verify(environment, never()).getPropertySources();
    }
}
