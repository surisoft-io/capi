package io.surisoft.capi.scim.resources;

import io.surisoft.capi.scim.validator.Urn;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

/**
 * All the different variations of SCIM responses require that the object
 * contains a list of the schemas it conforms to.
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class BaseResource implements Serializable {

  @XmlElement(name="schemas")
  @Size(min = 1)
  Set<@Urn String> schemas;

  public BaseResource(@Urn String urn) {
    addSchema(urn);
  }

  public void addSchema(@Urn String urn) {
    if (schemas == null){
      schemas = new TreeSet<>();
    }
    schemas.add(urn);
  }

  public void setSchemas(@Urn Set<String> schemas) {
    if (schemas == null) {
      this.schemas.clear();
    } else {
      this.schemas = new TreeSet<>(schemas);
    }
  }

  public Set<String> getSchemas() {
    return this.schemas;
  }
}