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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TbActorMailboxTest
 * @author: Qwen3-coder
 */
@Slf4j
public class TbActorMailboxTest {

    private static final String TEST_DISPATCHER = "test-dispatcher";

    private TbActorSystem actorSystem;
    private ExecutorService executor;
    private TbActorSystemSettings settings;

    @BeforeEach
    public void setUp() {
        settings = new TbActorSystemSettings(10, 1, 3);
        actorSystem = new DefaultTbActorSystem(settings);
        executor = ThingsBoardExecutors.newWorkStealingPool(2, getClass());
        actorSystem.createDispatcher(TEST_DISPATCHER, executor);
    }

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
    public void testMailboxInitialization() {
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        CountDownLatch initLatch = new CountDownLatch(1);
        AtomicBoolean initialized = new AtomicBoolean(false);

        TbActor actor = spy(new TestActor() {
            @Override
            public void init(TbActorCtx ctx) {
                initialized.set(true);
                initLatch.countDown();
            }
        });

        TbActorRef actorRef = actorSystem.createRootActor(TEST_DISPATCHER, new TestActorCreator(actorId, actor));
        assertThat(actorRef).isNotNull();

        try {
            initLatch.await(3, TimeUnit.SECONDS);
            assertThat(initialized.get()).isTrue();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMessageProcessing() throws InterruptedException {
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        CountDownLatch processLatch = new CountDownLatch(1);
        AtomicInteger processedValue = new AtomicInteger(0);

        TbActor actor = spy(new TestActor() {
            @Override
            public boolean process(TbActorMsg msg) {
                if (msg instanceof IntTbActorMsg) {
                    processedValue.set(((IntTbActorMsg) msg).getValue());
                    processLatch.countDown();
                }
                return true;
            }
        });

        TbActorRef actorRef = actorSystem.createRootActor(TEST_DISPATCHER, new TestActorCreator(actorId, actor));
        assertThat(actorRef).isNotNull();

        // 发送消息
        actorRef.tell(new IntTbActorMsg(42));

        // 等待消息处理
        boolean processed = processLatch.await(3, TimeUnit.SECONDS);
        assertThat(processed).isTrue();
        assertThat(processedValue.get()).isEqualTo(42);
    }

    @Test
    public void testHighPriorityMessageProcessing() throws InterruptedException {
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        CountDownLatch processLatch = new CountDownLatch(2);
        AtomicInteger firstProcessedValue = new AtomicInteger(0);
        AtomicInteger secondProcessedValue = new AtomicInteger(0);

        TbActor actor = spy(new TestActor() {
            @Override
            public boolean process(TbActorMsg msg) {
                if (msg instanceof IntTbActorMsg) {
                    if (firstProcessedValue.get() == 0) {
                        firstProcessedValue.set(((IntTbActorMsg) msg).getValue());
                    } else {
                        secondProcessedValue.set(((IntTbActorMsg) msg).getValue());
                    }
                    processLatch.countDown();
                }
                return true;
            }
        });

        TbActorRef actorRef = actorSystem.createRootActor(TEST_DISPATCHER, new TestActorCreator(actorId, actor));
        assertThat(actorRef).isNotNull();

        // 先发送普通优先级消息
        actorRef.tell(new IntTbActorMsg(1));
        // 再发送高优先级消息
        actorRef.tellWithHighPriority(new IntTbActorMsg(2));

        // 等待消息处理
        boolean processed = processLatch.await(3, TimeUnit.SECONDS);
        assertThat(processed).isTrue();
        // 高优先级消息应该先处理
        assertThat(firstProcessedValue.get()).isEqualTo(2);
        assertThat(secondProcessedValue.get()).isEqualTo(1);
    }

    @Test
    public void testActorDestroy() throws InterruptedException {
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        CountDownLatch destroyLatch = new CountDownLatch(1);
        AtomicReference<TbActorStopReason> stopReasonRef = new AtomicReference<>();

        TbActor actor = spy(new TestActor() {
            @Override
            public void destroy(TbActorStopReason stopReason, Throwable cause) {
                stopReasonRef.set(stopReason);
                destroyLatch.countDown();
            }
        });

        TbActorRef actorRef = actorSystem.createRootActor(TEST_DISPATCHER, new TestActorCreator(actorId, actor));
        assertThat(actorRef).isNotNull();

        // 停止actor
        actorSystem.stop(actorRef);

        // 等待销毁完成
        boolean destroyed = destroyLatch.await(3, TimeUnit.SECONDS);
        assertThat(destroyed).isTrue();
        assertThat(stopReasonRef.get()).isEqualTo(TbActorStopReason.STOPPED);
    }

    @Test
    public void testMessageOnStoppedActor() throws InterruptedException {
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        CountDownLatch stopLatch = new CountDownLatch(1);
        AtomicReference<TbActorStopReason> stopReasonRef = new AtomicReference<>();

        TbActor actor = spy(new TestActor() {
            @Override
            public void destroy(TbActorStopReason stopReason, Throwable cause) {
                stopReasonRef.set(stopReason);
                stopLatch.countDown();
            }
        });

        TbActorRef actorRef = actorSystem.createRootActor(TEST_DISPATCHER, new TestActorCreator(actorId, actor));
        assertThat(actorRef).isNotNull();

        // 停止actor
        actorSystem.stop(actorRef);

        // 等待销毁完成
        boolean stopped = stopLatch.await(3, TimeUnit.SECONDS);
        assertThat(stopped).isTrue();

        // 尝试向已停止的actor发送消息
        TbActorMsg msg = mock(TbActorMsg.class);
        when(msg.getMsgType()).thenReturn(MsgType.QUEUE_TO_RULE_ENGINE_MSG);
        actorRef.tell(msg);

        // 验证消息的onTbActorStopped方法被调用
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(msg, times(1)).onTbActorStopped(any(TbActorStopReason.class)));
    }

    static class TestActor implements TbActor {
        @Override
        public boolean process(TbActorMsg msg) {
            return true;
        }

        @Override
        public TbActorRef getActorRef() {
            return null;
        }
    }

    static class TestActorCreator implements TbActorCreator {
        private final TbActorId actorId;
        private final TbActor actor;

        public TestActorCreator(TbActorId actorId, TbActor actor) {
            this.actorId = actorId;
            this.actor = actor;
        }

        @Override
        public TbActorId createActorId() {
            return actorId;
        }

        @Override
        public TbActor createActor() {
            return actor;
        }
    }
}