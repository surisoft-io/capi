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
@ConditionalOnProperty(prefix = "oauth2.provider", name = "enabled", havingValue = "true")
public class Oauth2ClientManager {

    private static final Logger log = LoggerFactory.getLogger(Oauth2ClientManager.class);
    @Autowired
    private OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${oauth2.provider.clientId}")
    private String clientId;
    @Value("${oauth2.provider.clientSecret}")
    private String clientSecret;
    @Value("${oauth2.provider.host}")
    private String oauth2ProviderHost;

    @Value("${oauth2.provider.realm}")
    private String oauth2ProviderRealm;

    private String getAccessToken() throws IOException, Oauth2Exception {
        RequestBody requestBody = new FormBody.Builder()
                .add(Oauth2Constants.CLIENT_ID, clientId)
                .add(Oauth2Constants.CLIENT_SECRET, clientSecret)
                .add(Oauth2Constants.GRANT_TYPE, Oauth2Constants.CLIENT_CREDENTIALS_GRANT_TYPE)
                .build();
        Request accessTokenRequest = new Request.Builder()
                .url(oauth2ProviderHost + oauth2ProviderRealm + Oauth2Constants.TOKEN_URI)
                .post(requestBody)
                .build();
        try (Response clientRegistrationResponse = httpClient.newCall(accessTokenRequest).execute()) {
            if (clientRegistrationResponse.isSuccessful()) {
                JsonNode accessTokenResponse = objectMapper.readTree(Objects.requireNonNull(clientRegistrationResponse.body()).string());
                return accessTokenResponse.get(Oauth2Constants.ACCESS_TOKEN_ATTRIBUTE).asText();
            } else {
                throw new Oauth2Exception("Error getting access token from OIDC Provider");
            }
        }
    }

    public OIDCClient registerClient(String name) throws IOException, Oauth2Exception {
        log.info("Requesting client with name {}", name);
        String accessToken = getAccessToken();
        log.info(accessToken);
        try {
            JsonObject clientRegistrationJson = new JsonObject();
            clientRegistrationJson.put(Oauth2Constants.CLIENT_NAME, name);

            //Register the client to get ID and Secret
            Request clientRegistrationRequest = new Request.Builder()
                    .addHeader(Oauth2Constants.AUTHORIZATION_HEADER, Oauth2Constants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                    .url(oauth2ProviderHost + oauth2ProviderRealm + Oauth2Constants.CLIENT_REGISTRATION_URI)
                    .post(okhttp3.RequestBody.create(clientRegistrationJson.toJson(), Oauth2Constants.JSON_TYPE))
                    .build();
            try (Response clientRegistrationResponse = httpClient.newCall(clientRegistrationRequest).execute()) {
                if (clientRegistrationResponse.isSuccessful()) {
                    OIDCClient oidcClient = objectMapper.readValue(Objects.requireNonNull(clientRegistrationResponse.body()).string(), OIDCClient.class);
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.put(Oauth2Constants.SERVICE_ACCOUNT_ENABLE, true);
                    Request enableServiceAccountRequest = new Request.Builder()
                            .addHeader(Oauth2Constants.AUTHORIZATION_HEADER, Oauth2Constants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                            .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + Oauth2Constants.CLIENTS_URI + "/" +oidcClient.getClientId())
                            .put(RequestBody.create(jsonObject.toJson(), Oauth2Constants.JSON_TYPE))
                            .build();
                    try (Response enableServiceAccountResponse = httpClient.newCall(enableServiceAccountRequest).execute()) {
                        if (enableServiceAccountResponse.isSuccessful()) {
                            return oidcClient;
                        } else {
                            throw new Oauth2Exception("Error creating client for name: " + name);
                        }
                    }
                } else {
                    throw new Oauth2Exception("Error creating client for name: " + name);
                }
            }
        } catch (IOException e) {
            throw new Oauth2Exception(e.getMessage());
        }
    }

    private String createApiIdRole(String apiId) throws IOException, Oauth2Exception {
        String accessToken = getAccessToken();

        JsonObject apiRole = new JsonObject();
        apiRole.put(Oauth2Constants.NAME_ATTRIBUTE, apiId);

        Request createApiRoleRequest = new Request.Builder()
                .addHeader(Oauth2Constants.AUTHORIZATION_HEADER, Oauth2Constants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + Oauth2Constants.ROLES_URI)
                .post(RequestBody.create(apiRole.toJson(), Oauth2Constants.JSON_TYPE))
                .build();
        try (Response createApiRoleResponse = httpClient.newCall(createApiRoleRequest).execute()) {
            if (createApiRoleResponse.isSuccessful() || createApiRoleResponse.code() == 409) {
                return getApiIdRole(apiId, false);
            } else {
                throw new Oauth2Exception("Problem creating role for Service ID: " + apiId);
            }
        }
    }

    private String getApiIdRole(String apiId, boolean findOnly) throws IOException, Oauth2Exception {
        String accessToken = getAccessToken();
        Request getApiRoleRequest = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + Oauth2Constants.ROLES_URI + "/" + apiId)
                .get()
                .build();
        try (Response getApiRoleResponse = httpClient.newCall(getApiRoleRequest).execute()) {
            if (getApiRoleResponse.isSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(Objects.requireNonNull(getApiRoleResponse.body()).string());
                if (jsonNode.get("id") != null) {
                    return jsonNode.get("id").asText();
                } else {
                    throw new Oauth2Exception("Problem finding role for Service ID: " + apiId);
                }
            } else {
                if (!findOnly) {
                    throw new Oauth2Exception("Problem finding role for Service ID: " + apiId);
                } else {
                    return null;
                }
            }
        }
    }

    private String getServiceAccount(String clientId) throws IOException, Oauth2Exception {
        String accessToken = getAccessToken();
        Request getServiceAccountRequest = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + Oauth2Constants.CLIENTS_URI + "/" + clientId + "/service-account-user")
                .get()
                .build();
        try (Response getServiceAccountResponse = httpClient.newCall(getServiceAccountRequest).execute()) {
            if (getServiceAccountResponse.isSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(Objects.requireNonNull(getServiceAccountResponse.body()).string());
                return jsonResponse.get("id").asText();
            } else {
                throw new Oauth2Exception("Problem getting service account for client ID: " + clientId);
            }
        }
    }

    public void assignServiceAccountRole(String apiId, String clientId) throws IOException, Oauth2Exception {
        String apiIdRole = getApiIdRole(apiId, true);
        if(apiIdRole == null) {
            apiIdRole = createApiIdRole(apiId);
        }

        String serviceAccount = getServiceAccount(clientId);

        JsonArray assignServiceAccountJsonArray = new JsonArray();
        JsonObject assignServiceAccountJsonObject = new JsonObject();
        assignServiceAccountJsonObject.put(Oauth2Constants.NAME_ATTRIBUTE, apiId);
        assignServiceAccountJsonObject.put(Oauth2Constants.ID_ATTRIBUTE, apiIdRole);
        assignServiceAccountJsonArray.add(assignServiceAccountJsonObject);

        String accessToken = getAccessToken();
        Request assignServiceAccountRoleRequest = new Request.Builder()
                .addHeader(Oauth2Constants.AUTHORIZATION_HEADER, Oauth2Constants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + Oauth2Constants.USERS_URI + serviceAccount + Oauth2Constants.ROLE_MAPPING_URI)
                .post(RequestBody.create(assignServiceAccountJsonArray.toJson(), Oauth2Constants.JSON_TYPE))
                .build();
        try (Response assignServiceAccountRoleResponse = httpClient.newCall(assignServiceAccountRoleRequest).execute()) {
            if (!assignServiceAccountRoleResponse.isSuccessful()) {
                throw new Oauth2Exception("Problem subscribing to service, return code " + assignServiceAccountRoleResponse.code() + " for service id: " + apiId);
            }
        }
    }

    public List<OIDCClient> getCapiClients() throws IOException, Oauth2Exception {
        List<OIDCClient> oidcClients = new ArrayList<>();
        String accessToken = getAccessToken();
        Request getAllCapiClientsRequest = new Request.Builder()
                .addHeader(Oauth2Constants.AUTHORIZATION_HEADER, Oauth2Constants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + "/clients/")
                .get()
                .build();
        try (Response getAllCapiClientsResponse = httpClient.newCall(getAllCapiClientsRequest).execute()) {
            if (!getAllCapiClientsResponse.isSuccessful()) {
                throw new Oauth2Exception("Problem subscribing to service, return code " + getAllCapiClientsResponse.code());
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

    public List<Group> getGroups() throws IOException, Oauth2Exception {
        String accessToken = getAccessToken();
        Request getAllCapiClientsRequest = new Request.Builder()
                .addHeader(Oauth2Constants.AUTHORIZATION_HEADER, Oauth2Constants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + "/groups")
                .get()
                .build();
        try (Response getAllCapiClientsResponse = httpClient.newCall(getAllCapiClientsRequest).execute()) {
            if (!getAllCapiClientsResponse.isSuccessful()) {
                throw new Oauth2Exception("Problem subscribing to service, return code " + getAllCapiClientsResponse.code());
            }
            assert getAllCapiClientsResponse.body() != null;
            return objectMapper.readValue(getAllCapiClientsResponse.body().string(), new TypeReference<>() {
            });
        }
    }

    public boolean addClientToGroup(String groupName, String clientId) throws IOException, Oauth2Exception {
        JsonObject emptyBody = new JsonObject();
        List<Group> groupList = getGroups();
        if(groupList != null && !groupList.isEmpty()) {
            for(Group group : groupList) {
                if(group.getName().equals(groupName)) {
                    String serviceAccount = getServiceAccount(clientId);
                    String accessToken = getAccessToken();
                    Request addClientToGroupRequest = new Request.Builder()
                            .addHeader(Oauth2Constants.AUTHORIZATION_HEADER, Oauth2Constants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                            .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + "/users/" + serviceAccount + "/groups/" + group.getId())
                            .put(RequestBody.create(emptyBody.toJson(), Oauth2Constants.JSON_TYPE))
                            .build();
                    try (Response addClientToGroupResponse = httpClient.newCall(addClientToGroupRequest).execute()) {
                        if (!addClientToGroupResponse.isSuccessful()) {
                            throw new Oauth2Exception("Problem adding client to group, return code " + addClientToGroupResponse.code());
                        }
                       return true;
                    }
                }
            }
        }
        return false;
    }

    private JsonArray getAllMembersOfGroup(String groupName) throws Oauth2Exception, IOException {
        String accessToken = getAccessToken();
        List<Group> groupList = getGroups();
        if(groupList != null && !groupList.isEmpty()) {
            for(Group group : groupList) {
                if(group.getName().equals(groupName)) {
                    Request getMembersOfGroupRequest = new Request.Builder()
                            .addHeader(Oauth2Constants.AUTHORIZATION_HEADER, Oauth2Constants.BEARER_AUTHORIZATION_ATTRIBUTE + accessToken)
                            .url(oauth2ProviderHost + "/admin" + oauth2ProviderRealm + "/groups/" + group.getId() + "/members")
                            .get()
                            .build();
                    try (Response getMembersOfGroupResponse = httpClient.newCall(getMembersOfGroupRequest).execute()) {
                        if (!getMembersOfGroupResponse.isSuccessful()) {
                            throw new Oauth2Exception("Problem getting members of group, return code " + getMembersOfGroupResponse.code());
                        }
                        assert getMembersOfGroupResponse.body() != null;
                        return objectMapper.readValue(getMembersOfGroupResponse.body().string(), JsonArray.class);
                    }
                }
            }
        }
        return null;
    }

    public List<String> getAllClientsOfGroup(String groupName) throws Oauth2Exception, IOException {
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