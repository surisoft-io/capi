package io.surisoft.capi.scim.resources;

import io.surisoft.capi.scim.annotation.ScimAttribute;
import io.surisoft.capi.scim.exception.PhoneNumberParseException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * Scim core schema, <a
 * href="https://tools.ietf.org/html/rfc7643#section-4.1.2>section 4.1.2</a>
 */

@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class PhoneNumber implements Serializable, TypedAttribute {
  public Boolean getPrimary() {
    return primary;
  }

  public String getDisplay() {
    return display;
  }

  public void setDisplay(String display) {
    this.display = display;
  }

  public void setType(String type) {
    this.type = type;
  }
  public boolean isGlobalNumber() {
    return isGlobalNumber;
  }

  public void setGlobalNumber(boolean globalNumber) {
    isGlobalNumber = globalNumber;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public String getSubAddress() {
    return subAddress;
  }

  public void setSubAddress(String subAddress) {
    this.subAddress = subAddress;
  }

  public String getPhoneContext() {
    return phoneContext;
  }

  public void setPhoneContext(String phoneContext) {
    this.phoneContext = phoneContext;
  }

  public boolean isDomainPhoneContext() {
    return isDomainPhoneContext;
  }

  public void setDomainPhoneContext(boolean domainPhoneContext) {
    isDomainPhoneContext = domainPhoneContext;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  private static final String VISUAL_SEPARATORS = "[\\(\\)\\-\\.]";

  private static final Logger log = LoggerFactory.getLogger(PhoneNumber.class);

  @ScimAttribute(description = "Phone number of the User")
  private String value;

  @XmlElement
  @ScimAttribute(description = "A human readable name, primarily used for display purposes. READ-ONLY.")
  private String display;

  @XmlElement
  @ScimAttribute(canonicalValueList = { "work", "home", "mobile", "fax", "pager", "other" }, description = "A label indicating the attribute's function; e.g., 'work' or 'home' or 'mobile' etc.")
  private String type;

  @XmlElement
  @ScimAttribute(description = "A Boolean value indicating the 'primary' or preferred attribute value for this attribute, e.g. the preferred phone number or primary phone number. The primary attribute value 'true' MUST appear no more than once.")
  private Boolean primary = false;

  private boolean isGlobalNumber = false;

  private String number;

  private String extension;

  private String subAddress;

  private String phoneContext;

  private boolean isDomainPhoneContext = false;

  private Map<String, String> params;

  @XmlElement
  public String getValue() {
    return value;
  }

  public void setValue(String value) throws PhoneNumberParseException {
    this.setValue(value, false);
  }

  public void setValue(String value, boolean strict) throws PhoneNumberParseException {
       this.value = value;
  }

  @Override
  public String getType() {
    return type;
  }

}