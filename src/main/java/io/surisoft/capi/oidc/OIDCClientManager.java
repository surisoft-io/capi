package io.surisoft.capi.oidc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.schema.Group;
import io.surisoft.capi.schema.OIDCClient;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Value("${oidc.provider.realm}")
    private String oidcProviderRealm;

    private String getAccessToken() throws IOException, OIDCException {
        RequestBody requestBody = new FormBody.Builder()
                .add(OIDCConstants.CLIENT_ID, clientId)
                .add(OIDCConstants.CLIENT_SECRET, clientSecret)
                .add(OIDCConstants.GRANT_TYPE, OIDCConstants.CLIENT_CREDENTIALS_GRANT_TYPE)
                .build();
        Request accessTokenRequest = new Request.Builder()
                .url(oidcProviderHost + oidcProviderRealm + OIDCConstants.TOKEN_URI)
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
                    .url(oidcProviderHost + oidcProviderRealm + OIDCConstants.CLIENT_REGISTRATION_URI)
                    .post(okhttp3.RequestBody.create(clientRegistrationJson.toJson(), OIDCConstants.JSON_TYPE))
                    .build();
            try (Response clientRegistrationResponse = httpClient.newCall(clientRegistrationRequest).execute()) {
                if (clientRegistrationResponse.isSuccessful()) {
                    OIDCClient oidcClient = objectMapper.readValue(Objects.requireNonNull(clientRegistrationResponse.body()).string(), OIDCClient.class);
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.put(OIDCConstants.SERVICE_ACCOUNT_ENABLE, true);
                    Request enableServiceAccountRequest = new Request.Builder()
                            .addHeader(OIDCConstants.AUTHORIZATION_HEADER,OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                            .url(oidcProviderHost + "/admin" + oidcProviderRealm + OIDCConstants.CLIENTS_URI + "/" +oidcClient.getClientId())
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
                .url(oidcProviderHost + "/admin" + oidcProviderRealm + OIDCConstants.ROLES_URI)
                .post(RequestBody.create(apiRole.toJson(), OIDCConstants.JSON_TYPE))
                .build();
        try (Response createApiRoleResponse = httpClient.newCall(createApiRoleRequest).execute()) {
            if (createApiRoleResponse.isSuccessful() || createApiRoleResponse.code() == 409) {
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
                .url(oidcProviderHost + "/admin" + oidcProviderRealm + OIDCConstants.ROLES_URI + "/" + apiId)
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
                .url(oidcProviderHost + "/admin" + oidcProviderRealm + OIDCConstants.CLIENTS_URI + "/" + clientId + "/service-account-user")
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
                .url(oidcProviderHost + "/admin" + oidcProviderRealm + OIDCConstants.USERS_URI + serviceAccount + OIDCConstants.ROLE_MAPPING_URI)
                .post(RequestBody.create(assignServiceAccountJsonArray.toJson(), OIDCConstants.JSON_TYPE))
                .build();
        try (Response assignServiceAccountRoleResponse = httpClient.newCall(assignServiceAccountRoleRequest).execute()) {
            if (!assignServiceAccountRoleResponse.isSuccessful()) {
                throw new OIDCException("Problem subscribing to service, return code " + assignServiceAccountRoleResponse.code() + " for api id: " + apiId);
            }
        }
    }

    public List<OIDCClient> getCapiClients() throws IOException, OIDCException {
        List<OIDCClient> oidcClients = new ArrayList<>();
        String accessToken = getAccessToken();
        Request getAllCapiClientsRequest = new Request.Builder()
                .addHeader(OIDCConstants.AUTHORIZATION_HEADER, OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oidcProviderHost + "/admin" + oidcProviderRealm + "/clients/")
                .get()
                .build();
        try (Response getAllCapiClientsResponse = httpClient.newCall(getAllCapiClientsRequest).execute()) {
            if (!getAllCapiClientsResponse.isSuccessful()) {
                throw new OIDCException("Problem subscribing to service, return code " + getAllCapiClientsResponse.code());
            }
            assert getAllCapiClientsResponse.body() != null;
            JsonArray jsonArray = objectMapper.readValue(getAllCapiClientsResponse.body().string(), JsonArray.class);
            for(Object o : jsonArray) {
                if(o instanceof LinkedHashMap<?, ?> clientHashMap) {
                    if(clientHashMap.get("name").toString().startsWith("capi-")) {
                        OIDCClient oidcClient = new OIDCClient();
                        oidcClient.setClientId(clientHashMap.get("clientId").toString());
                        oidcClient.setName(clientHashMap.get("name").toString());
                        oidcClient.setServiceAccountsEnabled(true);
                        oidcClients.add(oidcClient);
                    }
                }
            }
            return oidcClients;
        }
    }

    public List<Group> getGroups() throws IOException, OIDCException {
        String accessToken = getAccessToken();
        Request getAllCapiClientsRequest = new Request.Builder()
                .addHeader(OIDCConstants.AUTHORIZATION_HEADER, OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oidcProviderHost + "/admin" + oidcProviderRealm + "/groups")
                .get()
                .build();
        try (Response getAllCapiClientsResponse = httpClient.newCall(getAllCapiClientsRequest).execute()) {
            if (!getAllCapiClientsResponse.isSuccessful()) {
                throw new OIDCException("Problem subscribing to service, return code " + getAllCapiClientsResponse.code());
            }
            assert getAllCapiClientsResponse.body() != null;
            return objectMapper.readValue(getAllCapiClientsResponse.body().string(), new TypeReference<>() {
            });
        }
    }

    public boolean addClientToGroup(String groupName, String clientId) throws IOException, OIDCException {
        JsonObject emptyBody = new JsonObject();
        List<Group> groupList = getGroups();
        if(groupList != null && !groupList.isEmpty()) {
            for(Group group : groupList) {
                if(group.getName().equals(groupName)) {
                    String serviceAccount = getServiceAccount(clientId);
                    String accessToken = getAccessToken();
                    Request addClientToGroupRequest = new Request.Builder()
                            .addHeader(OIDCConstants.AUTHORIZATION_HEADER, OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                            .url(oidcProviderHost + "/admin" + oidcProviderRealm + "/users/" + serviceAccount + "/groups/" + group.getId())
                            .put(RequestBody.create(emptyBody.toJson(), OIDCConstants.JSON_TYPE))
                            .build();
                    try (Response addClientToGroupResponse = httpClient.newCall(addClientToGroupRequest).execute()) {
                        if (!addClientToGroupResponse.isSuccessful()) {
                            throw new OIDCException("Problem adding client to group, return code " + addClientToGroupResponse.code());
                        }
                       return true;
                    }
                }
            }
        }
        return false;
    }

    private JsonArray getAllMembersOfGroup(String groupName) throws OIDCException, IOException {
        String accessToken = getAccessToken();
        List<Group> groupList = getGroups();
        if(groupList != null && !groupList.isEmpty()) {
            for(Group group : groupList) {
                if(group.getName().equals(groupName)) {
                    Request getMembersOfGroupRequest = new Request.Builder()
                            .addHeader(OIDCConstants.AUTHORIZATION_HEADER, OIDCConstants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                            .url(oidcProviderHost + "/admin" + oidcProviderRealm + "/groups/" + group.getId() + "/members")
                            .get()
                            .build();
                    try (Response getMembersOfGroupResponse = httpClient.newCall(getMembersOfGroupRequest).execute()) {
                        if (!getMembersOfGroupResponse.isSuccessful()) {
                            throw new OIDCException("Problem getting members of group, return code " + getMembersOfGroupResponse.code());
                        }
                        assert getMembersOfGroupResponse.body() != null;
                        return objectMapper.readValue(getMembersOfGroupResponse.body().string(), JsonArray.class);
                    }
                }
            }
        }
        return null;
    }

    public List<String> getAllClientsOfGroup(String groupName) throws OIDCException, IOException {
        List<String> clientList = new ArrayList<>();
        JsonArray members = getAllMembersOfGroup(groupName);
        assert members != null;
        members.forEach(o -> {
            LinkedHashMap<String, String> member = (LinkedHashMap<String, String>) o;
            clientList.add(member.get("username").replace("service-account-", ""));
        });
        return clientList;
    }
}