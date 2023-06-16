package io.surisoft.capi.lb.controller;

import io.surisoft.capi.lb.utils.HttpUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestHttpUtils {

    @Autowired
    HttpUtils httpUtils;

    @Test
    void testHttpConnectionTimeout() {
        String expected1 = "localhost?param=key&connectTimeout=100";
        String expected2 = "localhost?connectTimeout=100";
        Assertions.assertEquals(httpUtils.setHttpConnectTimeout("localhost?param=key", 100), expected1);
        Assertions.assertEquals(httpUtils.setHttpConnectTimeout("localhost", 100), expected2);
    }

    @Test
    void testSocketTimeout() {
        String expected1 = "localhost?param=key&socketTimeout=100";
        String expected2 = "localhost?socketTimeout=100";
        Assertions.assertEquals(httpUtils.setHttpSocketTimeout("localhost?param=key", 100), expected1);
        Assertions.assertEquals(httpUtils.setHttpSocketTimeout("localhost", 100), expected2);
    }

    @Test
    void testIngressEndpoint() {
        String expected1 = "localhost?param=key&customHostHeader=ingress.domain";
        String expected2 = "localhost?customHostHeader=ingress.domain";
        Assertions.assertEquals(httpUtils.setIngressEndpoint("localhost?param=key", "ingress.domain"), expected1);
        Assertions.assertEquals(httpUtils.setIngressEndpoint("localhost", "ingress.domain"), expected2);
    }

    @Test
    void testCapiContext() {
        String expected = "capi";
        Assertions.assertEquals(httpUtils.getCapiContext("capi/*"), expected);
    }

    @Test
    void testAll() {
        String expected = "localhost?param=key&customHostHeader=ingress.domain&socketTimeout=100&connectTimeout=100";
        String endpoint = httpUtils.setIngressEndpoint("localhost?param=key", "ingress.domain");
        endpoint = httpUtils.setHttpSocketTimeout(endpoint, 100);
        endpoint = httpUtils.setHttpConnectTimeout(endpoint, 100);
        Assertions.assertEquals(endpoint, expected);
    }
}