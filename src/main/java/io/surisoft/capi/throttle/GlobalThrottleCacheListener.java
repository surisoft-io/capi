package io.surisoft.capi.throttle;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryExpiredListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import io.surisoft.capi.schema.ThrottleServiceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalThrottleCacheListener implements EntryUpdatedListener<String, ThrottleServiceObject>,
                                              EntryRemovedListener<String, ThrottleServiceObject>,
        EntryExpiredListener<String, ThrottleServiceObject> {

    private static final Logger log = LoggerFactory.getLogger(GlobalThrottleCacheListener.class);
    @Override
    public void entryUpdated(EntryEvent<String, ThrottleServiceObject> entryEvent) {
        log.info("Throttle cache entry updated");
    }

    @Override
    public void entryRemoved(EntryEvent<String, ThrottleServiceObject> entryEvent) {
        log.info("Throttle cache entry removed");
    }

    @Override
    public void entryExpired(EntryEvent<String, ThrottleServiceObject> entryEvent) {
        log.info("Throttle cache entry expired");
    }
}