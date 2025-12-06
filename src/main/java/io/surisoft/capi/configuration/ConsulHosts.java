package io.surisoft.capi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "capi.consul")
public class ConsulHosts {

    private List<HostConfig> hosts;

    public List<HostConfig> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostConfig> hosts) {
        this.hosts = hosts;
    }

    public static class HostConfig {
        private String endpoint;
        private String token;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
