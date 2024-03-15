package io.surisoft.capi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Schema(hidden = true)
public class Service implements Serializable {
    private String id;
    private String name;
    private String context;
    private Set<Mapping> mappingList = new HashSet<>();
    private ServiceMeta serviceMeta;
    private boolean roundRobinEnabled;
    private boolean failOverEnabled;
    private boolean matchOnUriPrefix;
    private boolean forwardPrefix;
    private String registeredBy;
    private transient OpenAPI openAPI;
    private String serviceIdConsul;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Set<Mapping> getMappingList() {
        return mappingList;
    }

    public void setMappingList(Set<Mapping> mappingList) {
        this.mappingList = mappingList;
    }

    public ServiceMeta getServiceMeta() {
        return serviceMeta;
    }

    public void setServiceMeta(ServiceMeta serviceMeta) {
        this.serviceMeta = serviceMeta;
    }

    public boolean isRoundRobinEnabled() {
        return roundRobinEnabled;
    }

    public void setRoundRobinEnabled(boolean roundRobinEnabled) {
        this.roundRobinEnabled = roundRobinEnabled;
    }

    public boolean isMatchOnUriPrefix() {
        return matchOnUriPrefix;
    }

    public void setMatchOnUriPrefix(boolean matchOnUriPrefix) {
        this.matchOnUriPrefix = matchOnUriPrefix;
    }

    public boolean isForwardPrefix() {
        return forwardPrefix;
    }

    public void setForwardPrefix(boolean forwardPrefix) {
        this.forwardPrefix = forwardPrefix;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isFailOverEnabled() {
        return failOverEnabled;
    }

    public void setFailOverEnabled(boolean failOverEnabled) {
        this.failOverEnabled = failOverEnabled;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    public void setOpenAPI(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public String getServiceIdConsul() {
        return serviceIdConsul;
    }

    public void setServiceIdConsul(String serviceIdConsul) {
        this.serviceIdConsul = serviceIdConsul;
    }
}