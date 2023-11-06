package io.surisoft.capi.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StickySessionTest {

    private StickySession stickySessionUnderTest;

    @BeforeEach
    void setUp() throws Exception {
        stickySessionUnderTest = new StickySession();
    }

    @Test
    void testIdGetterAndSetter() {
        final String id = "id";
        stickySessionUnderTest.setId(id);
        assertThat(stickySessionUnderTest.getId()).isEqualTo(id);
    }

    @Test
    void testParamNameGetterAndSetter() {
        final String paramName = "paramName";
        stickySessionUnderTest.setParamName(paramName);
        assertThat(stickySessionUnderTest.getParamName()).isEqualTo(paramName);
    }

    @Test
    void testParamValueGetterAndSetter() {
        final String paramValue = "paramValue";
        stickySessionUnderTest.setParamValue(paramValue);
        assertThat(stickySessionUnderTest.getParamValue()).isEqualTo(paramValue);
    }

    @Test
    void testNodeIndexGetterAndSetter() {
        final int nodeIndex = 0;
        stickySessionUnderTest.setNodeIndex(nodeIndex);
        assertThat(stickySessionUnderTest.getNodeIndex()).isEqualTo(nodeIndex);
    }
}
