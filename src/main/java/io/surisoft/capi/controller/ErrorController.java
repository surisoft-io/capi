package io.surisoft.capi.controller;

import io.surisoft.capi.schema.CapiRestError;
import io.surisoft.capi.utils.Constants;
import io.vavr.collection.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
public class ErrorController {
    @GetMapping(path = Constants.CAPI_INTERNAL_REST_ERROR_PATH + "/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CapiRestError> get(HttpServletRequest request) {
        return buildResponse(request);
    }

    @PostMapping(path = Constants.CAPI_INTERNAL_REST_ERROR_PATH + "/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CapiRestError> post(HttpServletRequest request) {
        return buildResponse(request);
    }

    @PutMapping(path = Constants.CAPI_INTERNAL_REST_ERROR_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CapiRestError> put(HttpServletRequest request) {
        return buildResponse(request);
    }

    @DeleteMapping(path = Constants.CAPI_INTERNAL_REST_ERROR_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CapiRestError> delete(HttpServletRequest request) {
        return buildResponse(request);
    }

    private ResponseEntity<CapiRestError> buildResponse(HttpServletRequest request) {

        CapiRestError capiRestError = new CapiRestError();

        String errorMessage =  request.getHeader(Constants.REASON_MESSAGE_HEADER);

        if(request.getHeader(Constants.ROUTE_ID_HEADER) != null) {
            capiRestError.setRouteID(request.getHeader(Constants.ROUTE_ID_HEADER));
        }

        if(request.getHeader(Constants.CAPI_URI_IN_ERROR) != null) {
          capiRestError.setHttpUri(request.getHeader(Constants.CAPI_URI_IN_ERROR));
        }

        if(Boolean.parseBoolean(request.getHeader(Constants.ERROR_API_SHOW_TRACE_ID))) {
            capiRestError.setTraceID(request.getHeader(Constants.TRACE_ID_HEADER));
        }

        capiRestError.setErrorMessage(Objects.requireNonNullElse(errorMessage, "There was an exception connecting to the requested service, please try again later on."));

        if(request.getHeader(Constants.TRACE_ID_HEADER) != null) {
            capiRestError.setTraceID(request.getHeader(Constants.TRACE_ID_HEADER));
        }

        if(request.getHeader(Constants.REASON_CODE_HEADER) != null) {
            int returnedCode = Integer.parseInt(request.getHeader(Constants.REASON_CODE_HEADER));
            capiRestError.setErrorCode(returnedCode);
        } else {
            capiRestError.setErrorCode(HttpStatus.BAD_GATEWAY.value());
        }
        return new ResponseEntity<>(capiRestError, HttpStatus.valueOf(capiRestError.getErrorCode()));
    }
}