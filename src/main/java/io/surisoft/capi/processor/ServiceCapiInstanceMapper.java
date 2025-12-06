package io.surisoft.capi.processor;

import io.surisoft.capi.schema.ServiceCapiInstances;
import java.util.Map;

public class ServiceCapiInstanceMapper {

    public static final String SERVICE_CAPI_INSTANCE_PREFIX = "capi-instance-";

    public ServiceCapiInstances convert(Map<String, String> flat) {
        ServiceCapiInstances result = new ServiceCapiInstances();

        flat.forEach((key, value) -> {

            if (!key.startsWith(SERVICE_CAPI_INSTANCE_PREFIX)) return;

            String remainder = key.substring(SERVICE_CAPI_INSTANCE_PREFIX.length());

            String[] parts = remainder.split("-");
            if (parts.length < 2) return;

            String sectionName = parts[0];
            String propertyName = parts[1];

            ServiceCapiInstances.Instance instance =
                    result.getInstances().computeIfAbsent(sectionName, s -> new ServiceCapiInstances.Instance());

            applyProperty(instance, propertyName, value);
        });

        return result;
    }

    private void applyProperty(ServiceCapiInstances.Instance instance, String prop, String value) {
        switch (prop) {
            case "secured":
                instance.setSecured(Boolean.parseBoolean(value));
                break;
            case "open-api":
                instance.setOpenApi(value);
                break;
            case "route-group-first":
                instance.setRouteGroupFirst(Boolean.parseBoolean(value));
                break;
        }
    }
}
