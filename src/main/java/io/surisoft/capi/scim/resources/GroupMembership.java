package io.surisoft.capi.scim.resources;

import io.surisoft.capi.scim.annotation.ScimAttribute;
import io.surisoft.capi.scim.annotation.ScimResourceIdReference;
import io.surisoft.capi.scim.schema.Schema;
import jakarta.xml.bind.annotation.*;

import java.io.Serializable;

@XmlType(propOrder = {"value","ref","display","type"})
@XmlAccessorType(XmlAccessType.NONE)
public class GroupMembership implements Serializable {

  @XmlEnum
  public enum Type {
    @XmlEnumValue("User") USER,
    @XmlEnumValue("Group") GROUP;
  }
  
  @ScimAttribute(description="Identifier of the member of this Group.",
    mutability = Schema.Attribute.Mutability.IMMUTABLE)
  @ScimResourceIdReference
  @XmlElement
  private String value;

  @ScimAttribute(name = "$ref", description="The URI corresponding to a SCIM resource that is a member of this Group.",
    referenceTypes={"User", "Group"},
    mutability = Schema.Attribute.Mutability.IMMUTABLE)
  @XmlElement(name = "$ref")
  private String ref;

  @ScimAttribute(description="A human readable name, primarily used for display purposes.",
    mutability = Schema.Attribute.Mutability.READ_ONLY)
  @XmlElement
  private String display;

  @ScimAttribute(description="A label indicating the type of resource, e.g., 'User' or 'Group'.",
    canonicalValueList={"User", "Group"},
    mutability = Schema.Attribute.Mutability.IMMUTABLE)
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
