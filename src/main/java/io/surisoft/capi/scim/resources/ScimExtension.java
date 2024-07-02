package io.surisoft.capi.scim.resources;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "ScimExtension")
public interface ScimExtension extends Serializable {
  String getUrn();
}
