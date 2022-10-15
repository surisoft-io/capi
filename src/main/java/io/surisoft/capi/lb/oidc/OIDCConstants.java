package io.surisoft.capi.lb.oidc;

import okhttp3.MediaType;

public class OIDCConstants {
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String CLIENT_NAME = "client_name";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
    public static final String ACCESS_TOKEN_ATTRIBUTE = "access_token";
    public static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_AUTHORIZATION_ATTRIBUTE = "Bearer ";
    public static final String SERVICE_ACCOUNT_ENABLE = "serviceAccountsEnabled";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String ID_ATTRIBUTE = "id";
    public static final String USERS_URI = "/admin/realms/master/users/";
    public static final String ROLE_MAPPING_URI = "/role-mappings/realm";
    public static final String ROLES_URI = "/admin/realms/master/roles";
    public static final String CLIENTS_URI = "/admin/realms/master/clients";
    public static final String CLIENT_REGISTRATION_URI = "/realms/master/clients-registrations/openid-connect";
    public static final String TOKEN_URI = "/realms/master/protocol/openid-connect/token";
}
