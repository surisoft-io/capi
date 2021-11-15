package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.cache.ConsulDiscoveryCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CapiListener implements ApplicationListener<ApplicationEvent> {

    @Autowired
    private ConsulDiscoveryCacheManager consulDiscoveryCacheManager;

    @Value("${capi.consul.discovery.enabled}")
    private boolean consulEnabled;

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ContextClosedEvent) {
            log.debug("Removing this node from Master, before shutting down.");
            if(consulDiscoveryCacheManager.getLocalMemberID().equals(consulDiscoveryCacheManager.getConsulWorkerNode().getMember()) && consulEnabled) {
                consulDiscoveryCacheManager.removeMeFromMaster();
            }
        }
    }
}
