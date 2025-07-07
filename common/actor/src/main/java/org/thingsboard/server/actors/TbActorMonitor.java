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

import org.thingsboard.server.common.msg.TbActorMsg;

public interface TbActorMonitor {
    /**
     * Called when a new actor is created
     * @param actorId the ID of the created actor
     */
    void onActorCreated(TbActorId actorId);

    /**
     * Called when an actor is stopped
     * @param actorId the ID of the stopped actor
     */
    void onActorStopped(TbActorId actorId);

    /**
     * Called when a message is processed
     * @param actorId the ID of the processing actor
     * @param msg the processed message
     */
    void onMessageProcessed(TbActorId actorId, TbActorMsg msg);

    /**
     * Called when message processing fails
     * @param actorId the ID of the processing actor
     * @param msg the failed message
     * @param t the exception that occurred
     */
    void onMessageFailed(TbActorId actorId, TbActorMsg msg, Throwable t);
}
