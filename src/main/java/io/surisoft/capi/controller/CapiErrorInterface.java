package io.surisoft.capi.controller;

import io.surisoft.capi.utils.Constants;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class CapiErrorInterface implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CapiErrorInterface.class);

    @GetMapping(path = "/error",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> handleGet(HttpServletRequest request) {
        return handleTheError(request);
    }

    @PostMapping(path = "/error",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> handlePost(HttpServletRequest request) {
        return handleTheError(request);
    }

    @PutMapping(path = "/error",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> handlePut(HttpServletRequest request) {
        return handleTheError(request);
    }

    @DeleteMapping(path = "/error",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> handleDelete(HttpServletRequest request) {
        return handleTheError(request);
    }

    @PatchMapping(path = "/error",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> handlePatch(HttpServletRequest request) {
        return handleTheError(request);
    }

    private ResponseEntity<String> handleTheError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            log.trace("Handling error: {}", statusCode);
            JsonObject jsonObject = new JsonObject();
            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                jsonObject.put(Constants.ERROR_MESSAGE, "The requested route was not found, please try again later on.");
                jsonObject.put(Constants.ERROR_CODE, statusCode);
                return new ResponseEntity<>(jsonObject.toJson(), HttpStatus.NOT_FOUND);
            } else {
                jsonObject.put(Constants.ERROR_MESSAGE, "The requested route is not available, please try again later on.");
                jsonObject.put(Constants.ERROR_CODE, HttpStatus.UNAUTHORIZED.value());
                request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.UNAUTHORIZED.value());
                return new ResponseEntity<>(jsonObject.toJson(), HttpStatus.UNAUTHORIZED);
            }
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(Constants.ERROR_MESSAGE, "The requested route is not available, please try again later on.");
        jsonObject.put(Constants.ERROR_CODE, HttpStatus.UNAUTHORIZED.value());
        return new ResponseEntity<>(jsonObject.toJson(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}