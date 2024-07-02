package io.surisoft.capi.scim.resources;

import io.surisoft.capi.scim.annotation.ScimAttribute;
import io.surisoft.capi.scim.annotation.ScimResourceIdReference;
import io.surisoft.capi.scim.schema.Schema;
import jakarta.xml.bind.annotation.*;
import java.io.Serializable;

@XmlType(propOrder = {"value","ref","display","type"})
@XmlAccessorType(XmlAccessType.NONE)
public class UserGroup implements Serializable {
  @XmlEnum
  public enum Type {
    @XmlEnumValue("direct") DIRECT,
    @XmlEnumValue("indirect") INDIRECT;
  }
  
  @ScimAttribute(description="The identifier of the User's group.",
    mutability = Schema.Attribute.Mutability.READ_ONLY)
  @ScimResourceIdReference
  @XmlElement
  private String value;

  @ScimAttribute(name = "$ref", description="The URI of the corresponding 'Group' resource to which the user belongs.",
    referenceTypes={"User", "Group"},
    mutability = Schema.Attribute.Mutability.READ_ONLY)
  @XmlElement(name = "$ref")
  private String ref;

  @ScimAttribute(description="A human-readable name, primarily used for display purposes.",
    mutability = Schema.Attribute.Mutability.READ_ONLY)
  @XmlElement
  private String display;

  @ScimAttribute(description="A label indicating the attribute's function, e.g., 'direct' or 'indirect'.",
    canonicalValueList={"direct", "indirect"},
    mutability = Schema.Attribute.Mutability.READ_ONLY)
  @XmlElement
  private Type type;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public String getDisplay() {
    return display;
  }

  public void setDisplay(String display) {
    this.display = display;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }
}
