package io.surisoft.capi.scim.resources;

import io.surisoft.capi.scim.annotation.ScimAttribute;
import io.surisoft.capi.scim.annotation.ScimResourceType;
import io.surisoft.capi.scim.schema.Meta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ScimResourceType(id = ScimGroup.RESOURCE_NAME, name = ScimGroup.RESOURCE_NAME, schema = ScimGroup.SCHEMA_URI, description = "Top level ScimGroup", endpoint = "/Groups")
@XmlRootElement(name = ScimGroup.RESOURCE_NAME)
@XmlAccessorType(XmlAccessType.NONE)
public class ScimGroup extends ScimResource implements Serializable {
  public static final String RESOURCE_NAME = "Group";
  public static final String SCHEMA_URI = "urn:ietf:params:scim:schemas:core:2.0:Group";

  @XmlElement
  @ScimAttribute(description="A human-readable name for the Group.", required=true)
  String displayName;
  
  @XmlElement
  @ScimAttribute(description = "A list of members of the Group.")
  List<GroupMembership> members;

  public ScimGroup addMember(GroupMembership groupMembership) {
    if (members == null) {
      members = new ArrayList<>();
    }
    members.add(groupMembership);

    return this;
  }

  public ScimGroup() {
    super(SCHEMA_URI, RESOURCE_NAME);
  }

  @Override
  public void setSchemas(Set<String> schemas) {
    super.setSchemas(schemas);
  }

  @Override
  public void setMeta(@NotNull Meta meta) {
     super.setMeta(meta);
  }

  @Override
  public void setId(@Size(min = 1) String id) {
    super.setId(id);
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
  public void addSchema(String urn) {
      super.addSchema(urn);
  }

  @Override
  public ScimGroup addExtension(ScimExtension extension) {
    return (ScimGroup) super.addExtension(extension);
  }
}
