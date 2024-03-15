package io.surisoft.capi.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsulObject {

    @JsonProperty("ID")
    private String ID;

    @JsonProperty("ServiceName")
    private String serviceName;

    @JsonProperty("ServiceMeta")
    private ServiceMeta serviceMeta;

    @JsonProperty("ServiceAddress")
    private String serviceAddress;

    @JsonProperty("ServicePort")
    private int servicePort;

    @JsonProperty("ServiceID")
    private String serviceId;

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

    public ServiceMeta getServiceMeta() {
        return serviceMeta;
    }

    public void setServiceMeta(ServiceMeta serviceMeta) {
        this.serviceMeta = serviceMeta;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
}