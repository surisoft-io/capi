package io.surisoft.capi.scim.schema;

import io.surisoft.capi.scim.annotation.ScimResourceType;
import io.surisoft.capi.scim.resources.ScimExtension;
import io.surisoft.capi.scim.resources.ScimResourceWithOptionalId;
import io.surisoft.capi.scim.validator.Urn;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * SCIM ResourceType
 * 
 * @see <a href="https://tools.ietf.org/html/rfc7643#section-6">ResourceType Schema</a>
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ResourceType extends ScimResourceWithOptionalId {
  
  public static final String RESOURCE_NAME = "ResourceType";
  public static final String SCHEMA_URI = "urn:ietf:params:scim:schemas:core:2.0:ResourceType";
  private static final long serialVersionUID = -696969911228870476L;

  public static class SchemaExtensionConfiguration implements Serializable {

    private static final long serialVersionUID = 7351651561572744255L;

    @XmlElement(name = "schema")
    @Urn
    @Size(min = 1)
    private String schemaUrn;

    @XmlElement
    private boolean required;

    public String getSchemaUrn() {
      return schemaUrn;
    }

    public void setSchemaUrn(String schemaUrn) {
      this.schemaUrn = schemaUrn;
    }

    public boolean isRequired() {
      return required;
    }

    public void setRequired(boolean required) {
      this.required = required;
    }
  }

  @XmlElement
  @Size(min = 1)
  private String name;

  @XmlElement
  private String description;

  @XmlElement
  @Size(min = 1)
  private String endpoint;

  @XmlElement(name = "schema")
  @Urn
  @Size(min = 1)
  private String schemaUrn;

  @XmlElement
  private List<SchemaExtensionConfiguration> schemaExtensions;
  
  public ResourceType() {
    super(SCHEMA_URI, RESOURCE_NAME);
  }
  
  public ResourceType(ScimResourceType annotation) {
    super(SCHEMA_URI, RESOURCE_NAME);
    this.name = annotation.name();
    this.description = annotation.description();
    this.schemaUrn = annotation.schema();
    this.endpoint = annotation.endpoint();
  }

  //@Override
  //public ResourceType setSchemas(Set<String> schemas) {
  //  return (ResourceType) super.setSchemas(schemas);
  //}

  @Override
  public void setMeta(Meta meta) {
     super.setMeta(meta);
  }

  @Override
  public void setExternalId(String externalId) {
     super.setExternalId(externalId);
  }

  @Override
  public void setExtensions(Map<String, ScimExtension> extensions) {
    super.setExtensions(extensions);
  }

  @Override
  public void setId(String id) {
    super.setId(id);
  }

  @Override
  public ResourceType addExtension(ScimExtension extension) {
    return (ResourceType) super.addExtension(extension);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getSchemaUrn() {
    return schemaUrn;
  }

  public void setSchemaUrn(String schemaUrn) {
    this.schemaUrn = schemaUrn;
  }

  public List<SchemaExtensionConfiguration> getSchemaExtensions() {
    return schemaExtensions;
  }

  public void setSchemaExtensions(List<SchemaExtensionConfiguration> schemaExtensions) {
    this.schemaExtensions = schemaExtensions;
  }
}
