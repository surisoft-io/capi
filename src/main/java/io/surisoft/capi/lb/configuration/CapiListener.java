package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.cache.ConsulDiscoveryCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class CapiListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(CapiListener.class);

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