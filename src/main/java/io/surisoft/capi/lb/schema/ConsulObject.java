package io.surisoft.capi.lb.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsulObject {

    @JsonProperty("ID")
    private String ID;

    @JsonProperty("ServiceName")
    private String serviceName;

    @JsonProperty("ServiceTags")
    private List<String> serviceTags;

    @JsonProperty("ServiceAddress")
    private String serviceAddress;

    @JsonProperty("ServicePort")
    private int servicePort;

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<String> getServiceTags() {
        return serviceTags;
    }

    public void setServiceTags(List<String> serviceTags) {
        this.serviceTags = serviceTags;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public int getServicePort() {
        return servicePort;
    }

    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }
}
