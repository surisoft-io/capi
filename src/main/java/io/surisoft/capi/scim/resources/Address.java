package io.surisoft.capi.scim.resources;

import io.surisoft.capi.scim.annotation.ScimAttribute;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.io.Serializable;

/**
 * Scim core schema, <a href="https://tools.ietf.org/html/rfc7643#section-4.1.2>section 4.1.2</a>
 *
 */
@XmlType(name = "address")
@XmlAccessorType(XmlAccessType.NONE)
public class Address implements Serializable, TypedAttribute {

  @XmlElement
  @ScimAttribute(canonicalValueList = { "work", "home", "other" },
          description="A label indicating the attribute's function; e.g., 'office', 'mobile' etc.")
  private String type;
  
  @XmlElement
  @ScimAttribute(description="A human readable name, primarily used for display purposes. READ-ONLY.")
  private String display;
  
  @XmlElement
  @ScimAttribute(description="A Boolean value indicating the 'primary' or preferred attribute value for this attribute, e.g. the preferred mailing address or primary e-mail address. The primary attribute value 'true' MUST appear no more than once.")
  private Boolean primary = false;
  
  @ScimAttribute(description="The two letter ISO 3166-1 alpha-2 country code")
  @XmlElement
  private String country;
  
  @ScimAttribute(description="The full mailing address, formatted for display or use with a mailing label. This attribute MAY contain newlines.")
  @XmlElement
  private String formatted;
  
  @ScimAttribute(description="The city or locality component.")
  @XmlElement
  private String locality;
  
  @ScimAttribute(description="The zipcode or postal code component.")
  @XmlElement
  private String postalCode;
  
  @ScimAttribute(description="The state or region component.")
  @XmlElement
  private String region;
  
  @ScimAttribute(description="The full street address component, which may include house number, street name, PO BOX, and multi-line extended street address information. This attribute MAY contain newlines.")
  @XmlElement
  private String streetAddress;

  @Override
  public String getType() {
    return type;
  }

  public String getDisplay() {
    return display;
  }

  public void setDisplay(String display) {
    this.display = display;
  }

  public Boolean getPrimary() {
    return primary;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getFormatted() {
    return formatted;
  }

  public void setFormatted(String formatted) {
    this.formatted = formatted;
  }

  public String getLocality() {
    return locality;
  }

  public void setLocality(String locality) {
    this.locality = locality;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getStreetAddress() {
    return streetAddress;
  }

  public void setStreetAddress(String streetAddress) {
    this.streetAddress = streetAddress;
  }
}