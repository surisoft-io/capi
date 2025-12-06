package io.surisoft.capi.schema;

import java.util.HashMap;
import java.util.Map;

public class ServiceCapiInstances {

    private Map<String, Instance> instances = new HashMap<>();

    public static class Instance {
        private boolean secured;
        private String openApi;
        private boolean routeGroupFirst;

        public boolean isSecured() {
            return secured;
        }

        public void setSecured(boolean secured) {
            this.secured = secured;
        }

        public String getOpenApi() {
            return openApi;
        }

        public void setOpenApi(String openApi) {
            this.openApi = openApi;
        }

        public boolean isRouteGroupFirst() {
            return routeGroupFirst;
        }

        public void setRouteGroupFirst(boolean routeGroupFirst) {
            this.routeGroupFirst = routeGroupFirst;
        }


    }

    public Map<String, Instance> getInstances() {
        return instances;
    }

    public void setSections(Map<String, Instance> instances) {
        this.instances = instances;
    }
}
