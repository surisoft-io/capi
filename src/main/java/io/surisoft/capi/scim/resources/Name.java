package io.surisoft.capi.scim.resources;

import io.surisoft.capi.scim.annotation.ScimAttribute;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.io.Serializable;

@XmlType(name = "name", propOrder = {
    "formatted",
    "familyName",
    "givenName",
    "middleName",
    "honorificPrefix",
    "honorificSuffix"    
})
@XmlAccessorType(XmlAccessType.NONE)
public class Name implements Serializable  {

  @XmlElement
  @ScimAttribute(description="The full name, including all middle names, titles, and suffixes as appropriate, formatted for display (e.g. Ms. Barbara J Jensen, III.).")
  private String formatted;

  @XmlElement
  @ScimAttribute(description="The family name of the User, or Last Name in most Western languages (e.g. Jensen given the full name Ms. Barbara J Jensen, III.).")
  private String familyName;

  @XmlElement
  @ScimAttribute(description="The given name of the User, or First Name in most Western languages (e.g. Barbara given the full name Ms. Barbara J Jensen, III.).")
  private String givenName;

  @XmlElement
  @ScimAttribute(description="The middle name(s) of the User (e.g. Robert given the full name Ms. Barbara J Jensen, III.).")
  private String middleName;

  @XmlElement
  @ScimAttribute(description="The honorific prefix(es) of the User, or Title in most Western languages (e.g. Ms. given the full name Ms. Barbara J Jensen, III.).")
  private String honorificPrefix;

  @XmlElement
  @ScimAttribute(description="The honorific suffix(es) of the User, or Suffix in most Western languages (e.g. III. given the full name Ms. Barbara J Jensen, III.).")
  private String honorificSuffix;

  public String getFormatted() {
    return formatted;
  }

  public void setFormatted(String formatted) {
    this.formatted = formatted;
  }

  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public void setMiddleName(String middleName) {
    this.middleName = middleName;
  }

  public String getHonorificPrefix() {
    return honorificPrefix;
  }

  public void setHonorificPrefix(String honorificPrefix) {
    this.honorificPrefix = honorificPrefix;
  }

  public String getHonorificSuffix() {
    return honorificSuffix;
  }

  public void setHonorificSuffix(String honorificSuffix) {
    this.honorificSuffix = honorificSuffix;
  }
}
