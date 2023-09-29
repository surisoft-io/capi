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
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class CapiErrorInterface implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CapiErrorInterface.class);

    @GetMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleError(HttpServletRequest request) {

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            log.trace("Handling error: {}", statusCode);

            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put(Constants.ERROR_MESSAGE, "The requested route was not found, please try again later on.");
                jsonObject.put(Constants.ERROR_CODE, statusCode);
                return new ResponseEntity<>(jsonObject.toJson(), HttpStatus.NOT_FOUND);
            } else {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put(Constants.ERROR_MESSAGE, "The requested route is not available, please try again later on.");
                jsonObject.put(Constants.ERROR_CODE, HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(jsonObject.toJson(), HttpStatus.BAD_REQUEST);
            }

       }
       JsonObject jsonObject = new JsonObject();
       jsonObject.put(Constants.ERROR_MESSAGE, "The requested route is not available, please try again later on.");
       jsonObject.put(Constants.ERROR_CODE, HttpStatus.BAD_REQUEST);
       return new ResponseEntity<>(jsonObject.toJson(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}