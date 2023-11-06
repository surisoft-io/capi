package io.surisoft.capi.schema;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ServiceTest {

    @Mock
    private Set<Mapping> mockMappingList;
    @Mock
    private ServiceMeta mockServiceMeta;
    @Mock
    private OpenAPI mockOpenAPI;

    private Service serviceUnderTest;

    @BeforeEach
    void setUp() throws Exception {
        serviceUnderTest = new Service();
        serviceUnderTest.setMappingList(mockMappingList);
        serviceUnderTest.setServiceMeta(mockServiceMeta);
        serviceUnderTest.setOpenAPI(mockOpenAPI);
    }

    @Test
    void testNameGetterAndSetter() {
        final String name = "name";
        serviceUnderTest.setName(name);
        assertThat(serviceUnderTest.getName()).isEqualTo(name);
    }

    @Test
    void testContextGetterAndSetter() {
        final String context = "context";
        serviceUnderTest.setContext(context);
        assertThat(serviceUnderTest.getContext()).isEqualTo(context);
    }

    @Test
    void testGetMappingList() {
        assertThat(serviceUnderTest.getMappingList()).isEqualTo(mockMappingList);
    }

    @Test
    void testGetServiceMeta() {
        assertThat(serviceUnderTest.getServiceMeta()).isEqualTo(mockServiceMeta);
    }

    @Test
    void testRoundRobinEnabledGetterAndSetter() {
        final boolean roundRobinEnabled = false;
        serviceUnderTest.setRoundRobinEnabled(roundRobinEnabled);
        assertThat(serviceUnderTest.isRoundRobinEnabled()).isFalse();
    }

    @Test
    void testMatchOnUriPrefixGetterAndSetter() {
        final boolean matchOnUriPrefix = false;
        serviceUnderTest.setMatchOnUriPrefix(matchOnUriPrefix);
        assertThat(serviceUnderTest.isMatchOnUriPrefix()).isFalse();
    }

    @Test
    void testForwardPrefixGetterAndSetter() {
        final boolean forwardPrefix = false;
        serviceUnderTest.setForwardPrefix(forwardPrefix);
        assertThat(serviceUnderTest.isForwardPrefix()).isFalse();
    }

    @Test
    void testIdGetterAndSetter() {
        final String id = "id";
        serviceUnderTest.setId(id);
        assertThat(serviceUnderTest.getId()).isEqualTo(id);
    }

    @Test
    void testFailOverEnabledGetterAndSetter() {
        final boolean failOverEnabled = false;
        serviceUnderTest.setFailOverEnabled(failOverEnabled);
        assertThat(serviceUnderTest.isFailOverEnabled()).isFalse();
    }

    @Test
    void testRegisteredByGetterAndSetter() {
        final String registeredBy = "registeredBy";
        serviceUnderTest.setRegisteredBy(registeredBy);
        assertThat(serviceUnderTest.getRegisteredBy()).isEqualTo(registeredBy);
    }

    @Test
    void testGetOpenAPI() {
        assertThat(serviceUnderTest.getOpenAPI()).isEqualTo(mockOpenAPI);
    }
}
