package org.thingsboard.server.actors.internal.queue;

import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Default mailbox queue implementation with two priority levels.
 * High priority messages are always drained before normal ones.
 */
public final class TwoLevelPriorityQueue implements MailboxQueue {

    private final Queue<TbActorMsg> high = new ConcurrentLinkedQueue<>();
    private final Queue<TbActorMsg> normal = new ConcurrentLinkedQueue<>();

    @Override
    public void offer(TbActorMsg msg, boolean highPriority) {
        if (highPriority) {
            high.offer(msg);
        } else {
            normal.offer(msg);
        }
    }

    @Override
    public TbActorMsg poll() {
        TbActorMsg msg = high.poll();
        return msg != null ? msg : normal.poll();
    }

    @Override
    public boolean isEmpty() {
        return high.isEmpty() && normal.isEmpty();
    }

    @Override
    public void clearAndNotify(Consumer<TbActorMsg> onDrop) {
        TbActorMsg m;
        while ((m = high.poll()) != null) {
            onDrop.accept(m);
        }
        while ((m = normal.poll()) != null) {
            onDrop.accept(m);
        }
    }
}