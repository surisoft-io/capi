package io.surisoft.capi.controller;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import io.surisoft.scim.ScimController;
import io.surisoft.scim.request.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.cache2k.Cache;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Optional;

@RestController
@RequestMapping("/scim")
public class ScimEndpoint {

    private final Optional<ScimController> scimController;
    private final HttpUtils httpUtils;
    private final Cache<String, Service> serviceCache;

    public ScimEndpoint(
                        Optional<ScimController> scimController,
                        HttpUtils httpUtils,
                        Cache<String, Service> serviceCache
                          ) {
        this.httpUtils = httpUtils;
        this.serviceCache = serviceCache;
        this.scimController = scimController;
    }

    @GetMapping
    public ResponseEntity<String> ping() {
        System.out.println("PING");
        return new ResponseEntity<>("ok", HttpStatus.OK);
    }

    @PostMapping("/{client}")
    public ResponseEntity<String> createUser(@PathVariable String client, HttpServletRequest httpServletRequest) {
        try {
            if (httpServletRequest.getHeader(Constants.CAPI_GROUP_HEADER) == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing group header");
            }

            String contextPath = httpUtils.contextToRole(httpServletRequest.getHeader(Constants.CAPI_GROUP_HEADER));
            String accessToken = httpUtils.processAuthorizationAccessToken(httpServletRequest);

            Service service = serviceCache.get(contextPath);
            assert service != null;

            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing authorization");
            }

            if (scimController.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No SCIM implementation detected");
            }

            if (!scimController.get().getScimImplementationName().equalsIgnoreCase(client)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Detected SCIM implementation does not match the request");
            }

            Result result = scimController.get().handleRequest(accessToken, httpServletRequest);

            if (result.getStatusCode() != 200) {
                String sanitizedMessage = HtmlUtils.htmlEscape(result.getErrorMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(sanitizedMessage);
            }

            return new ResponseEntity<>(HttpStatus.OK);
       } catch (Exception e) {

        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("result.getErrorMessage()");
    }

}