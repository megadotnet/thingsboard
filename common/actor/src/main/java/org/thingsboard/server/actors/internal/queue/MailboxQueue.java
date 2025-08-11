package org.thingsboard.server.actors.internal.queue;

import org.thingsboard.server.common.msg.TbActorMsg;
import java.util.function.Consumer;

/**
 * Mailbox queue abstraction to decouple TbActorMailbox from concrete queue implementation.
 * Default implementation uses two-level priority queues.
 */
public interface MailboxQueue {
    void offer(TbActorMsg msg, boolean highPriority);
    TbActorMsg poll();
    boolean isEmpty();
    void clearAndNotify(Consumer<TbActorMsg> onDrop);
}