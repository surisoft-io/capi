package io.surisoft.capi.lb.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.lb.schema.OIDCClient;
import okhttp3.*;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "oidc.provider", name = "enabled", havingValue = "true")
public class OIDCClientManager {

    private static final Logger log = LoggerFactory.getLogger(OIDCClientManager.class);
    @Autowired
    private OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${oidc.provider.clientId}")
    private String clientId;
    @Value("${oidc.provider.clientSecret}")
    private String clientSecret;
    @Value("${oidc.provider.host}")
    private String oidcProviderHost;

    private String getAccessToken() throws IOException, OIDCException {
        RequestBody requestBody = new FormBody.Builder()
                .add(OIDCConstants.CLIENT_ID, clientId)
                .add(OIDCConstants.CLIENT_SECRET, clientSecret)
                .add(OIDCConstants.GRANT_TYPE, OIDCConstants.CLIENT_CREDENTIALS_GRANT_TYPE)
                .build();
        Request accessTokenRequest = new Request.Builder()
                .url(oidcProviderHost + OIDCConstants.TOKEN_URI)
                .post(requestBody)
                .build();
        try (Response clientRegistrationResponse = httpClient.newCall(accessTokenRequest).execute()) {
            if (clientRegistrationResponse.isSuccessful()) {
                JsonNode accessTokenResponse = objectMapper.readTree(Objects.requireNonNull(clientRegistrationResponse.body()).string());
                return accessTokenResponse.get(OIDCConstants.ACCESS_TOKEN_ATTRIBUTE).asText();
            } else {
                throw new OIDCException("Error getting access token from OIDC Provider");
            }
        }
    }

    public OIDCClient registerClient(String name) throws IOException, OIDCException {
        log.info("Requesting client with name {}", name);
        String accessToken = getAccessToken();
        log.info(accessToken);
        try {
            JsonObject clientRegistrationJson = new JsonObject();
            clientRegistrationJson.put(OIDCConstants.CLIENT_NAME, name);

            //Register the client to get ID and Secret
            Request clientRegistrationRequest = new Request.Builder()
                    .addHeader(OIDCConstants.AUTHORIZATION_HEADER, OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                    .url(oidcProviderHost + OIDCConstants.CLIENT_REGISTRATION_URI)
                    .post(okhttp3.RequestBody.create(clientRegistrationJson.toJson(), OIDCConstants.JSON_TYPE))
                    .build();
            try (Response clientRegistrationResponse = httpClient.newCall(clientRegistrationRequest).execute()) {
                if (clientRegistrationResponse.isSuccessful()) {
                    OIDCClient oidcClient = objectMapper.readValue(Objects.requireNonNull(clientRegistrationResponse.body()).string(), OIDCClient.class);
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.put(OIDCConstants.SERVICE_ACCOUNT_ENABLE, true);
                    Request enableServiceAccountRequest = new Request.Builder()
                            .addHeader(OIDCConstants.AUTHORIZATION_HEADER,OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                            .url(oidcProviderHost + OIDCConstants.CLIENTS_URI + "/" +oidcClient.getClientId())
                            .put(RequestBody.create(jsonObject.toJson(), OIDCConstants.JSON_TYPE))
                            .build();
                    try (Response enableServiceAccountResponse = httpClient.newCall(enableServiceAccountRequest).execute()) {
                        if (enableServiceAccountResponse.isSuccessful()) {
                            return oidcClient;
                        } else {
                            throw new OIDCException("Error creating client for name: " + name);
                        }
                    }
                } else {
                    throw new OIDCException("Error creating client for name: " + name);
                }
            }
        } catch (IOException e) {
            throw new OIDCException(e.getMessage());
        }
    }

    private String createApiIdRole(String apiId) throws IOException, OIDCException {
        String accessToken = getAccessToken();

        JsonObject apiRole = new JsonObject();
        apiRole.put(OIDCConstants.NAME_ATTRIBUTE, apiId);

        Request createApiRoleRequest = new Request.Builder()
                .addHeader(OIDCConstants.AUTHORIZATION_HEADER, OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oidcProviderHost + OIDCConstants.ROLES_URI)
                .post(RequestBody.create(apiRole.toJson(), OIDCConstants.JSON_TYPE))
                .build();
        try (Response createApiRoleResponse = httpClient.newCall(createApiRoleRequest).execute()) {
            if (createApiRoleResponse.isSuccessful()) {
                return getApiIdRole(apiId, false);
            } else {
                throw new OIDCException("Problem creating role for Api ID: " + apiId);
            }
        }
    }

    private String getApiIdRole(String apiId, boolean findOnly) throws IOException, OIDCException {
        String accessToken = getAccessToken();
        Request getApiRoleRequest = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .url("http://localhost:8080/admin/realms/master/roles/" + apiId)
                .get()
                .build();
        try (Response getApiRoleResponse = httpClient.newCall(getApiRoleRequest).execute()) {
            if (getApiRoleResponse.isSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(Objects.requireNonNull(getApiRoleResponse.body()).string());
                if (jsonNode.get("id") != null) {
                    return jsonNode.get("id").asText();
                } else {
                    throw new OIDCException("Problem finding role for Api ID: " + apiId);
                }
            } else {
                if (!findOnly) {
                    throw new OIDCException("Problem finding role for Api ID: " + apiId);
                } else {
                    return null;
                }
            }
        }
    }

    private String getServiceAccount(String clientId) throws IOException, OIDCException {
        String accessToken = getAccessToken();
        Request getServiceAccountRequest = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .url("http://localhost:8080/admin/realms/master/clients/" + clientId + "/service-account-user")
                .get()
                .build();
        try (Response getServiceAccountResponse = httpClient.newCall(getServiceAccountRequest).execute()) {
            if (getServiceAccountResponse.isSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(Objects.requireNonNull(getServiceAccountResponse.body()).string());
                return jsonResponse.get("id").asText();
            } else {
                throw new OIDCException("Problem getting service account for client ID: " + clientId);
            }
        }
    }

    public void assignServiceAccountRole(String apiId, String clientId) throws IOException, OIDCException {
        String apiIdRole = getApiIdRole(apiId, true);
        if(apiIdRole == null) {
            apiIdRole = createApiIdRole(apiId);
        }

        String serviceAccount = getServiceAccount(clientId);

        JsonArray assignServiceAccountJsonArray = new JsonArray();
        JsonObject assignServiceAccountJsonObject = new JsonObject();
        assignServiceAccountJsonObject.put(OIDCConstants.NAME_ATTRIBUTE, apiId);
        assignServiceAccountJsonObject.put(OIDCConstants.ID_ATTRIBUTE, apiIdRole);
        assignServiceAccountJsonArray.add(assignServiceAccountJsonObject);

        String accessToken = getAccessToken();
        Request assignServiceAccountRoleRequest = new Request.Builder()
                .addHeader(OIDCConstants.AUTHORIZATION_HEADER, OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oidcProviderHost + OIDCConstants.USERS_URI + serviceAccount + OIDCConstants.ROLE_MAPPING_URI)
                .post(RequestBody.create(assignServiceAccountJsonArray.toJson(), OIDCConstants.JSON_TYPE))
                .build();
        try (Response assignServiceAccountRoleResponse = httpClient.newCall(assignServiceAccountRoleRequest).execute()) {
            if (!assignServiceAccountRoleResponse.isSuccessful()) {
                throw new OIDCException("Problem subscribing to service, return code " + assignServiceAccountRoleResponse.code() + " for api id: " + apiId);
            }
        }
    }

    public JsonArray getCapiClients() throws IOException, OIDCException {
        String accessToken = getAccessToken();
        Request getAllCapiClientsRequest = new Request.Builder()
                .addHeader(OIDCConstants.AUTHORIZATION_HEADER, OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oidcProviderHost + "/admin/realms/master/clients/")
                .get()
                .build();
        try (Response getAllCapiClientsResponse = httpClient.newCall(getAllCapiClientsRequest).execute()) {
            if (!getAllCapiClientsResponse.isSuccessful()) {
                throw new OIDCException("Problem subscribing to service, return code " + getAllCapiClientsResponse.code());
            }
            assert getAllCapiClientsResponse.body() != null;
            return objectMapper.readValue(getAllCapiClientsResponse.body().string(), JsonArray.class);
        }
    }
}