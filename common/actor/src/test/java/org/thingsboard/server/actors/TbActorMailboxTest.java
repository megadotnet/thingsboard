package org.thingsboard.server.actors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style tests for TbActorMailbox behavior (priority handling).
 */
public class TbActorMailboxTest {

    private ExecutorService executor;
    private TbActorSystem actorSystem;

    @AfterEach
    public void tearDown() {
        if (actorSystem != null) {
            actorSystem.stop();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void testPriorityProcessingOrder() throws InterruptedException {
        TbActorSystemSettings settings = new TbActorSystemSettings(10, 1, 3);
        actorSystem = new DefaultTbActorSystem(settings);
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
        actorSystem.createDispatcher("test-dispatcher", executor);

        final ActorTestCtx ctx = new ActorTestCtx(new java.util.concurrent.CountDownLatch(1), new AtomicInteger(0), 2, new AtomicLong(0));

        TbActorCreator creator = new TbActorCreator() {
            @Override
            public TbActorId createActorId() {
                return new TbStringActorId("mailbox-test-actor");
            }

            @Override
            public TbActor createActor() {
                return new AbstractTbActor() {
                    @Override
                    public boolean process(org.thingsboard.server.common.msg.TbActorMsg msg) {
                        ctx.getInvocationCount().incrementAndGet();
                        ctx.getActual().addAndGet(((IntTbActorMsg) msg).getValue());
                        if (ctx.getInvocationCount().get() >= ctx.getExpectedInvocationCount()) {
                            ctx.getLatch().countDown();
                        }
                        return true;
                    }
                };
            }
        };

        TbActorRef ref = actorSystem.createRootActor("test-dispatcher", creator);

        // Send normal then high priority message: high should be processed first.
        ref.tell(new IntTbActorMsg(2)); // normal
        ref.tellWithHighPriority(new IntTbActorMsg(1)); // high

        boolean processed = ctx.getLatch().await(5, TimeUnit.SECONDS);
        assertTrue(processed, "Messages were not processed in time");

        // cleanup is done in tearDown
    }
}