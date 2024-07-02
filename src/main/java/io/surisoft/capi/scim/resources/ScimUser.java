package io.surisoft.capi.scim.resources;

import io.surisoft.capi.scim.annotation.ScimAttribute;
import io.surisoft.capi.scim.annotation.ScimResourceType;
import io.surisoft.capi.scim.schema.Meta;
import io.surisoft.capi.scim.schema.Schema;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.*;

@ScimResourceType(id = ScimUser.RESOURCE_NAME, name = ScimUser.RESOURCE_NAME, schema = ScimUser.SCHEMA_URI, description = "Top level ScimUser", endpoint = "/Users")
@XmlRootElement(name = ScimUser.RESOURCE_NAME)
@XmlAccessorType(XmlAccessType.NONE)
public class ScimUser extends ScimResource implements Serializable {
  public static final String RESOURCE_NAME = "User";
  public static final String SCHEMA_URI = "urn:ietf:params:scim:schemas:core:2.0:User";

  @XmlElement
  @ScimAttribute(description="A Boolean value indicating the User's administrative status.")
  private Boolean active = true;

  @XmlElement
  @ScimAttribute(description="A physical mailing address for this User, as described in (address Element). Canonical Type Values of work, home, and other. The value attribute is a complex type with the following sub-attributes.")
  private  List<Address> addresses;

  @XmlElement
  @ScimAttribute(description="The name of the User, suitable for display to end-users. The name SHOULD be the full name of the User being described if known")
  private String displayName;

  @XmlElement
  @ScimAttribute(description="E-mail addresses for the user. The value SHOULD be canonicalized by the Service Provider, e.g. bjensen@example.com instead of bjensen@EXAMPLE.COM. Canonical Type values of work, home, and other.")
  private List<Email> emails;

  @XmlElement
  @ScimAttribute(description="An entitlement may be an additional right to a thing, object, or service")
  private List<Entitlement> entitlements;

  @XmlElement
  @ScimAttribute(description="A list of groups that the user belongs to, either thorough direct membership, nested groups, or dynamically calculated")
  private List<UserGroup> groups;

  @XmlElement
  @ScimAttribute(description="Instant messaging address for the User.")
  private List<Im> ims;

  @XmlElement
  @ScimAttribute(description="Used to indicate the User's default location for purposes of localizing items such as currency, date time format, numerical representations, etc.")
  private String locale;

  @XmlElement
  @ScimAttribute(description="The components of the user's real name. Providers MAY return just the full name as a single string in the formatted sub-attribute, or they MAY return just the individual component attributes using the other sub-attributes, or they MAY return both. If both variants are returned, they SHOULD be describing the same name, with the formatted name indicating how the component attributes should be combined.")
  private Name name;

  @XmlElement
  @ScimAttribute(description="The casual way to address the user in real life, e.g.'Bob' or 'Bobby' instead of 'Robert'. This attribute SHOULD NOT be used to represent a User's username (e.g. bjensen or mpepperidge)")
  private String nickName;

  @XmlElement
  @ScimAttribute(returned = Schema.Attribute.Returned.NEVER, description="The User's clear text password.  This attribute is intended to be used as a means to specify an initial password when creating a new User or to reset an existing User's password.")
  private String password;

  @XmlElement
  @ScimAttribute(description="Phone numbers for the User.  The value SHOULD be canonicalized by the Service Provider according to format in RFC3966 e.g. 'tel:+1-201-555-0123'.  Canonical Type values of work, home, mobile, fax, pager and other.")
  private List<PhoneNumber> phoneNumbers;

  @XmlElement
  @ScimAttribute(description="URLs of photos of the User.")
  private List<Photo> photos;

  @XmlElement
  @ScimAttribute(description="A fully qualified URL to a page representing the User's online profile", referenceTypes={"external"})
  private String profileUrl;

  @XmlElement
  @ScimAttribute(description="Indicates the User's preferred written or spoken language.  Generally used for selecting a localized User interface. e.g., 'en_US' specifies the language English and country US.")
  private String preferredLanguage;

  @XmlElement
  @ScimAttribute(description="A list of roles for the User that collectively represent who the User is; e.g., 'Student', 'Faculty'.")
  private List<Role> roles;

  @XmlElement
  @ScimAttribute(description="The User's time zone in the 'Olson' timezone database format; e.g.,'America/Los_Angeles'")
  private String timezone;

  @XmlElement
  @ScimAttribute(description="The user's title, such as \"Vice President.\"")
  private String title;

  @XmlElement
  @ScimAttribute(required=true, uniqueness= Schema.Attribute.Uniqueness.SERVER, description="Unique identifier for the User typically used by the user to directly authenticate to the service provider. Each User MUST include a non-empty userName value.  This identifier MUST be unique across the Service Consumer's entire set of Users.  REQUIRED")
  private String userName;

  @XmlElement
  @ScimAttribute(description="Used to identify the organization to user relationship. Typical values used might be 'Contractor', 'Employee', 'Intern', 'Temp', 'External', and 'Unknown' but any value may be used.")
  private String userType;

  @XmlElement
  @ScimAttribute(description="A list of certificates issued to the User.")
  private List<X509Certificate> x509Certificates;

  public ScimUser() {
    super(SCHEMA_URI, RESOURCE_NAME);
  }

  public Optional<Address> getPrimaryAddress() {
    if (addresses == null) {
      return Optional.empty();
    }

    return addresses.stream()
                    .filter(Address::getPrimary)
                    .findFirst();
  }

  public Optional<Email> getPrimaryEmailAddress() {
    if (emails == null) {
      return Optional.empty();
    }

    return emails.stream()
                 .filter(Email::getPrimary)
                 .findFirst();
  }

  public Optional<PhoneNumber> getPrimaryPhoneNumber() {
    if (phoneNumbers == null) {
      return Optional.empty();
    }

    return phoneNumbers.stream()
                       .filter(PhoneNumber::getPrimary)
                       .findFirst();
  }

  @Override
  public void setSchemas(Set<String> schemas) {
    super.setSchemas(schemas);
  }

  @Override
  public void setMeta(Meta meta) {
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
  public ScimUser addExtension(ScimExtension extension) {
    return (ScimUser) super.addExtension(extension);
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public List<Address> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<Address> addresses) {
    this.addresses = addresses;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public List<Email> getEmails() {
    return emails;
  }

  public void setEmails(List<Email> emails) {
    this.emails = emails;
  }

  public List<Entitlement> getEntitlements() {
    return entitlements;
  }

  public void setEntitlements(List<Entitlement> entitlements) {
    this.entitlements = entitlements;
  }

  public List<UserGroup> getGroups() {
    return groups;
  }

  public void setGroups(List<UserGroup> groups) {
    this.groups = groups;
  }

  public List<Im> getIms() {
    return ims;
  }

  public void setIms(List<Im> ims) {
    this.ims = ims;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public Name getName() {
    return name;
  }

  public void setName(Name name) {
    this.name = name;
  }

  public String getNickName() {
    return nickName;
  }

  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public List<PhoneNumber> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  public List<Photo> getPhotos() {
    return photos;
  }

  public void setPhotos(List<Photo> photos) {
    this.photos = photos;
  }

  public String getProfileUrl() {
    return profileUrl;
  }

  public void setProfileUrl(String profileUrl) {
    this.profileUrl = profileUrl;
  }

  public String getPreferredLanguage() {
    return preferredLanguage;
  }

  public void setPreferredLanguage(String preferredLanguage) {
    this.preferredLanguage = preferredLanguage;
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserType() {
    return userType;
  }

  public void setUserType(String userType) {
    this.userType = userType;
  }

  public List<X509Certificate> getX509Certificates() {
    return x509Certificates;
  }

  public void setX509Certificates(List<X509Certificate> x509Certificates) {
    this.x509Certificates = x509Certificates;
  }
}
