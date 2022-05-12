package io.surisoft.capi.lb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.lb.schema.AliasInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
      locations = "classpath:test-application.properties"
)
class TestCertificateController {

    private static final String DEFAULT_ALIAS;
    private static final String CACERT_NAME;

    static {
        DEFAULT_ALIAS = "default";
        CACERT_NAME = "cacerts";
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void initialize() {
        createEmptyCacerts();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetAll() throws Exception {
        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/manager/certificate")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isOk())
                        .andReturn();

        List<AliasInfo> aliasInfoList = objectMapper.readValue(getResult.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, AliasInfo.class));
        System.out.println(getResult.getResponse().getContentAsString());
        Assertions.assertTrue(aliasInfoList.size() > 0);
    }

    @Test
    void testCertificateUpload() throws Exception {
        File capiUnitTestCer = createTestCertificate();
        Assertions.assertNotNull(capiUnitTestCer);

        FileInputStream inputStream = new FileInputStream(capiUnitTestCer);
        MvcResult postResult  = mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/manager/certificate/capi-unit-test").file("file", inputStream.readAllBytes()))
                .andExpect(status().isOk())
                .andReturn();
        AliasInfo aliasInfo = objectMapper.readValue(postResult.getResponse().getContentAsString(), AliasInfo.class);
        System.out.println(postResult.getResponse().getContentAsString());
        Assertions.assertEquals("capi-unit-test", aliasInfo.getAlias());
    }

    private File createTestCertificate() throws Exception {

        X509Certificate x509Certificate = createCertificate("capi-unit-test");
        File capiUnitTestCer = new File("capi-unit-test.cer");
        try(FileOutputStream fileOutputStream = new FileOutputStream(capiUnitTestCer)) {
            fileOutputStream.write(x509Certificate.getEncoded());
            fileOutputStream.flush();
        }
        return capiUnitTestCer;
    }

    private X509Certificate createCertificate(String name) throws Exception {
        BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();


        X500Name owner = new X500Name("CN=" + name);
        X509v3CertificateBuilder x509v3CertificateBuilder = new JcaX509v3CertificateBuilder(
                owner, new BigInteger(64, new SecureRandom()), new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24), new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365*10)), owner, keyPair.getPublic());

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(privateKey);
        X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);
        X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider(bouncyCastleProvider).getCertificate(x509CertificateHolder);
        x509Certificate.verify(keyPair.getPublic());

        return x509Certificate;
    }

    private void createEmptyCacerts() {
        String storePassword = "changeit";
        String storeName = null;
        try {
            String executionPath = ResourceUtils.getFile("classpath:").getAbsolutePath();
            storeName = executionPath + "/" + CACERT_NAME;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Assertions.assertNotNull(storeName);

        String storeType = "JKS";
        try (FileOutputStream fileOutputStream = new FileOutputStream(storeName)) {
            KeyStore keystore = KeyStore.getInstance(storeType);
            keystore.load(null, storePassword.toCharArray());
            keystore.setCertificateEntry(DEFAULT_ALIAS, createCertificate(DEFAULT_ALIAS));
            keystore.store(fileOutputStream, storePassword.toCharArray());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}