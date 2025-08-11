package org.thingsboard.server.actors;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.actors.internal.queue.TwoLevelPriorityQueue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TwoLevelPriorityQueue.
 */
public class TwoLevelPriorityQueueTest {

    @Test
    public void testPriorityOrder() {
        TwoLevelPriorityQueue queue = new TwoLevelPriorityQueue();
        queue.offer(new IntTbActorMsg(2), false);
        queue.offer(new IntTbActorMsg(1), true);

        int first = ((IntTbActorMsg) queue.poll()).getValue();
        int second = ((IntTbActorMsg) queue.poll()).getValue();

        assertEquals(1, first);
        assertEquals(2, second);
        assertNull(queue.poll());
    }

    @Test
    public void testClearAndNotify() {
        TwoLevelPriorityQueue queue = new TwoLevelPriorityQueue();
        queue.offer(new IntTbActorMsg(5), false);
        queue.offer(new IntTbActorMsg(6), true);

        List<Integer> dropped = new ArrayList<>();
        queue.clearAndNotify(m -> dropped.add(((IntTbActorMsg) m).getValue()));

        assertEquals(2, dropped.size());
        // high priority was added second but drained first; clear simply iterates both queues.
        assertTrue(dropped.contains(5));
        assertTrue(dropped.contains(6));
        assertNull(queue.poll());
    }
}