package io.surisoft.capi.scim.schema;

import java.io.Serializable;
import java.util.Set;

public interface AttributeContainer extends Serializable {

  String getUrn();

  Set<Schema.Attribute> getAttributes();

  Schema.Attribute getAttribute(String attributeName);
}
