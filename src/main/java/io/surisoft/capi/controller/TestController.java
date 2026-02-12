package io.surisoft.capi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.kafka.CapiInstance;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.SubscriptionGroup;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/test")
//@ConditionalOnProperty(value = "capi.kafka.enabled", havingValue = "true")
public class TestController {

    //@Autowired
    //RestTemplate restTemplate;

    private SubscriptionGroup consulKeyValueToList(ObjectMapper mapper, String encodedValue) throws JsonProcessingException {
        String decodedValue = new String(Base64.getDecoder().decode(encodedValue));
        return mapper.readValue(decodedValue, SubscriptionGroup.class);
    }

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @Autowired(required = false)
    private CapiInstance capiInstance;

    @Autowired
    private Cache<String, Service> serviceCache;

    @Autowired(required = false)
    private Cache<String, List<String>> consulKvStoreCache;



    @PostMapping("/cors")
    public String addHeaders(@RequestBody String headers) {
        List<String> headerList = Arrays.asList(headers.split(",", -1));
        if(!headerList.isEmpty()) {
            consulKvStoreCache.put("capi-cors-headers", headerList);
        }
        return "OK";
    }

}
