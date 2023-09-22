package io.surisoft.capi.oidc;

import okhttp3.MediaType;

public class Oauth2Constants {
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
    public static final String USERS_URI = "/users/";
    public static final String ROLE_MAPPING_URI = "/role-mappings/realm";
    public static final String ROLES_URI = "/roles";
    public static final String CLIENTS_URI = "/clients";
    public static final String CLIENT_REGISTRATION_URI = "/clients-registrations/openid-connect";
    public static final String TOKEN_URI = "/protocol/openid-connect/token";
    public static final String CERTS_URI = "/protocol/openid-connect/certs";
    public static final String REALMS_CLAIM = "realm_access";
    public static final String SUBSCRIPTIONS_CLAIM = "subscriptions";
    public static final String ROLES_CLAIM = "roles";
    public static final String CAMEL_SERVLET_CONTEXT_PATH = "CamelServletContextPath";
    public static final String AUTHORIZATION_QUERY = "access_token";
}
