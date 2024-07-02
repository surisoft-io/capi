package io.surisoft.capi.scim.resources;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import io.surisoft.capi.scim.annotation.ScimAttribute;
import io.surisoft.capi.scim.annotation.ScimExtensionType;
import io.surisoft.capi.scim.annotation.ScimResourceType;
import io.surisoft.capi.scim.exception.InvalidExtensionException;
import io.surisoft.capi.scim.schema.Meta;
import io.surisoft.capi.scim.schema.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class defines the attributes shared by all SCIM resources. It also
 * provides BVF annotations to allow validation of the POJO.
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class ScimResource extends BaseResource implements Serializable {

  @XmlElement
  @NotNull
  @ScimAttribute(returned = Schema.Attribute.Returned.ALWAYS)
  private Meta meta;

  @XmlElement
  @Size(min = 1)
  @ScimAttribute(required = true, returned = Schema.Attribute.Returned.ALWAYS, mutability = Schema.Attribute.Mutability.READ_ONLY, uniqueness = Schema.Attribute.Uniqueness.SERVER, description = "A unique identifier for a SCIM resource as defined by the service provider.")
  private String id;

  @XmlElement
  @ScimAttribute(caseExact = true, mutability = Schema.Attribute.Mutability.READ_WRITE)
  private String externalId;

  private Map<String, ScimExtension> extensions = new LinkedHashMap<>();

  private final String baseUrn;

  private final String resourceType;

  public ScimResource(String urn, String resourceType) {
    super(urn);
    this.baseUrn = urn;
    this.resourceType = resourceType;

    ScimResourceType resourceTypeAnnotation = getClass().getAnnotation(ScimResourceType.class);
    if (resourceTypeAnnotation != null) {
      this.meta = new Meta();
      this.meta.setResourceType(resourceTypeAnnotation.id());
    }
  }

  public ScimResource addExtension(ScimExtension extension) {
    ScimExtensionType[] se = extension.getClass().getAnnotationsByType(ScimExtensionType.class);

    if (se.length != 1) {
      throw new InvalidExtensionException("Registered extensions must have an ScimExtensionType annotation");
    }

    String extensionUrn = se[0].id();
    extensions.put(extensionUrn, extension);
    
    addSchema(extensionUrn);
    return this;
  }

  public ScimExtension getExtension(String urn) {
    return extensions.get(urn);
  }


  private <T> ScimExtensionType lookupScimExtensionType(Class<T> extensionClass) {
    ScimExtensionType[] se = extensionClass.getAnnotationsByType(ScimExtensionType.class);

    if (se.length != 1) {
      throw new InvalidExtensionException("Registered extensions must have an ScimExtensionType annotation");
    }

    return se[0];
  }

  public String getBaseUrn() {
    return baseUrn;
  }

  @JsonAnyGetter
  public Map<String, ScimExtension> getExtensions() {
    return extensions;
  }

  public ScimExtension removeExtension(String urn) {
    return extensions.remove(urn);
  }
  
  @SuppressWarnings("unchecked")
  public <T> T removeExtension(Class<T> extensionClass) {
    ScimExtensionType se = lookupScimExtensionType(extensionClass);
    
    return (T) extensions.remove(se.id());
  }

  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public void setExtensions(Map<String, ScimExtension> extensions) {
    this.extensions = extensions;
  }

  public String getResourceType() {
    return resourceType;
  }

}
