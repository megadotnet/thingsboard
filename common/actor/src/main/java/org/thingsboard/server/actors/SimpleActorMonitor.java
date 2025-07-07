/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class SimpleActorMonitor implements TbActorMonitor {
    private final ConcurrentHashMap<TbActorId, ActorStats> stats = new ConcurrentHashMap<>();

    @Override
    public void onActorCreated(TbActorId actorId) {
        stats.putIfAbsent(actorId, new ActorStats());
        log.debug("Actor created: {}", actorId);
    }

    @Override
    public void onActorStopped(TbActorId actorId) {
        log.debug("Actor stopped: {}", actorId);
        stats.remove(actorId);
    }

    @Override
    public void onMessageProcessed(TbActorId actorId, TbActorMsg msg) {
        ActorStats actorStats = stats.get(actorId);
        if (actorStats != null) {
            actorStats.incrementProcessed();
        }
    }

    @Override
    public void onMessageFailed(TbActorId actorId, TbActorMsg msg, Throwable t) {
        ActorStats actorStats = stats.get(actorId);
        if (actorStats != null) {
            actorStats.incrementFailed();
        }
        log.warn("Message processing failed for actor {}: {}", actorId, msg, t);
    }

    private static class ActorStats {
        private final AtomicLong processed = new AtomicLong();
        private final AtomicLong failed = new AtomicLong();

        void incrementProcessed() {
            processed.incrementAndGet();
        }

        void incrementFailed() {
            failed.incrementAndGet();
        }
    }
}
