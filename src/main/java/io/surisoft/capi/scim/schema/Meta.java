package io.surisoft.capi.scim.schema;

import io.surisoft.capi.scim.adapter.LocalDateTimeAdapter;
import io.surisoft.capi.scim.annotation.ScimAttribute;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Defines the structure of the meta attribute for all SCIM resources as defined
 * by section 3.1 of the SCIM schema specification. See
 * https://tools.ietf.org/html/draft-ietf-scim-core-schema-17#section-3.1 for more
 * details.
 */
@XmlType(name = "meta")
@XmlAccessorType(XmlAccessType.NONE)
public class Meta implements Serializable {
  @XmlElement
  @Size(min = 1)
  @ScimAttribute(mutability = Schema.Attribute.Mutability.READ_ONLY, caseExact = true, description = "The name of the resource type of the resource.")
  private String resourceType;
  
  @XmlElement
  @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
  @ScimAttribute(mutability = Schema.Attribute.Mutability.READ_ONLY, description = "The DateTime that the resource was added to the service provider.")
  private LocalDateTime created;
  
  @XmlElement
  @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
  @ScimAttribute(mutability = Schema.Attribute.Mutability.READ_ONLY, description = "The most recent DateTime that the details of this resource were updated at the service provider.")
  private LocalDateTime lastModified;
  
  @XmlElement
  @ScimAttribute(mutability = Schema.Attribute.Mutability.READ_ONLY, description = "The URI of the resource being returned.")
  private String location;
  
  @XmlElement
  @ScimAttribute(mutability = Schema.Attribute.Mutability.READ_ONLY, description = "The version of the resource being returned.  This value must be the same as the entity-tag (ETag) HTTP response header")
  private String version;

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public LocalDateTime getCreated() {
    return created;
  }

  public void setCreated(LocalDateTime created) {
    this.created = created;
  }

  public LocalDateTime getLastModified() {
    return lastModified;
  }

  public void setLastModified(LocalDateTime lastModified) {
    this.lastModified = lastModified;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
