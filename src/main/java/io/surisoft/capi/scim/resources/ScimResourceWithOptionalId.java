package io.surisoft.capi.scim.resources;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * This class overrides the required id element in ScimResource for use as a
 * base class for some of the odd SCIM resources.
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class ScimResourceWithOptionalId extends ScimResource {
  @XmlElement
  String id;
  
  public ScimResourceWithOptionalId(String urn, String resourceType) {
    super(urn, resourceType);
  }

  @Override
  public String getId() {
    return id;
  }
}
