package io.surisoft.capi.scim.schema;

import java.util.Set;

public class ScimSpecSchema {

  private final static Set<String> schemaNames = Set.of(
    "urn:ietf:params:scim:schemas:core:2.0:Group",
    "urn:ietf:params:scim:schemas:core:2.0:ResourceType",
    "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig",
    "urn:ietf:params:scim:schemas:core:2.0:User",
    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
  );
}