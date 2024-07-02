package io.surisoft.capi.scim.extension;

import io.surisoft.capi.scim.annotation.ScimAttribute;
import io.surisoft.capi.scim.annotation.ScimExtensionType;
import io.surisoft.capi.scim.resources.ScimExtension;
import io.surisoft.capi.scim.schema.Schema;
import jakarta.xml.bind.annotation.*;

import java.io.Serializable;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@ScimExtensionType(required = false, name = "EnterpriseUser", id = EnterpriseExtension.URN, description = "Attributes commonly used in representing users that belong to, or act on behalf of, a business or enterprise.")
public class EnterpriseExtension implements ScimExtension {

  public static final String URN = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

  @XmlType
  @XmlAccessorType(XmlAccessType.NONE)
  public static class Manager implements Serializable {

    private static final long serialVersionUID = -7930518578899296192L;

    @ScimAttribute(description = "The \"id\" of the SCIM resource representing the user's manager.  RECOMMENDED.")
    @XmlElement
    private String value;

    @ScimAttribute(name="$ref", description = "The URI of the SCIM resource representing the User's manager.  RECOMMENDED.")
    @XmlElement(name="$ref")
    private String ref;

    @ScimAttribute(mutability = Schema.Attribute.Mutability.READ_ONLY, description = "he displayName of the user's manager.  This attribute is OPTIONAL.")
    @XmlElement
    private String displayName;
  }

  @ScimAttribute(description = "A string identifier, typically numeric or alphanumeric, assigned to a person, typically based on order of hire or association with an organization.")
  @XmlElement
  private String employeeNumber;

  @ScimAttribute(description = "Identifies the name of a cost center.")
  @XmlElement
  private String costCenter;

  @ScimAttribute(description = "Identifies the name of an organization.")
  @XmlElement
  private String organization;

  @ScimAttribute(description = "Identifies the name of a division.")
  @XmlElement
  private String division;

  @ScimAttribute(description = "Identifies the name of a department.")
  @XmlElement
  private String department;

  @ScimAttribute(description = "The user's manager.  A complex type that optionally allows service providers to represent organizational hierarchy by referencing the \"id\" attribute of another User.")
  @XmlElement
  private Manager manager;

  @Override
  public String getUrn() {
    return URN;
  }

  public String getEmployeeNumber() {
    return employeeNumber;
  }

  public void setEmployeeNumber(String employeeNumber) {
    this.employeeNumber = employeeNumber;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public void setCostCenter(String costCenter) {
    this.costCenter = costCenter;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getDivision() {
    return division;
  }

  public void setDivision(String division) {
    this.division = division;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public Manager getManager() {
    return manager;
  }

  public void setManager(Manager manager) {
    this.manager = manager;
  }
}
