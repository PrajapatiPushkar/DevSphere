package com.devsphere.user.idempotency;

import com.devsphere.user.entity.ProcessedEvent;
import com.devsphere.user.repository.ProcessedEventRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
class EventIdempotencyIntegrationTest {

    @Autowired
    private EventIdempotencyService idempotencyService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        processedEventRepository.deleteAll();
    }

    @Test
    void firstEvent_executesBusinessActionAndStoresProcessedEventMarker() {
        String eventId = UUID.randomUUID().toString();
        String consumerGroup = "test-group-1";
        AtomicInteger executionCounter = new AtomicInteger(0);

        EventProcessingResult<Integer> result = idempotencyService.executeIdempotent(
                eventId, "ResumeVersionPublished", consumerGroup, executionCounter::incrementAndGet
        );

        assertThat(result.isProcessed()).isTrue();
        assertThat(result.isDuplicate()).isFalse();
        assertThat(executionCounter.get()).isEqualTo(1);

        Optional<ProcessedEvent> opt = processedEventRepository.findByEventIdAndConsumerGroup(eventId, consumerGroup);
        assertThat(opt).isPresent();
        assertThat(opt.get().getEventType()).isEqualTo("ResumeVersionPublished");
    }

    @Test
    void duplicateEvent_skipsBusinessActionAndReturnsDuplicateResult() {
        String eventId = UUID.randomUUID().toString();
        String consumerGroup = "test-group-1";
        AtomicInteger executionCounter = new AtomicInteger(0);

        // First execution
        idempotencyService.executeIdempotent(eventId, "ResumeVersionPublished", consumerGroup, executionCounter::incrementAndGet);
        assertThat(executionCounter.get()).isEqualTo(1);

        // Duplicate execution
        EventProcessingResult<Integer> dupResult = idempotencyService.executeIdempotent(
                eventId, "ResumeVersionPublished", consumerGroup, executionCounter::incrementAndGet
        );

        assertThat(dupResult.isProcessed()).isFalse();
        assertThat(dupResult.isDuplicate()).isTrue();
        assertThat(executionCounter.get()).isEqualTo(1); // Business action skipped!
    }

    @Test
    void differentEventIds_processIndependently() {
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();
        String consumerGroup = "test-group-1";
        AtomicInteger executionCounter = new AtomicInteger(0);

        idempotencyService.executeIdempotent(eventId1, "ResumeVersionPublished", consumerGroup, executionCounter::incrementAndGet);
        idempotencyService.executeIdempotent(eventId2, "ResumeVersionPublished", consumerGroup, executionCounter::incrementAndGet);

        assertThat(executionCounter.get()).isEqualTo(2);
        assertThat(processedEventRepository.count()).isEqualTo(2);
    }

    @Test
    void failedBusinessOperation_rollsBackProcessedEventMarker() {
        String eventId = UUID.randomUUID().toString();
        String consumerGroup = "test-group-1";

        assertThatThrownBy(() -> idempotencyService.executeIdempotent(
                eventId, "ResumeVersionPublished", consumerGroup, () -> {
                    throw new RuntimeException("Business action failure");
                }
        )).isInstanceOf(RuntimeException.class);

        // Marker must NOT be saved when transaction rolls back
        Optional<ProcessedEvent> opt = processedEventRepository.findByEventIdAndConsumerGroup(eventId, consumerGroup);
        assertThat(opt).isEmpty();
    }

    @Test
    void concurrentDuplicateDelivery_executesBusinessActionOnlyOnce() throws Exception {
        String eventId = UUID.randomUUID().toString();
        String consumerGroup = "concurrent-test-group";
        AtomicInteger executionCounter = new AtomicInteger(0);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    idempotencyService.executeIdempotent(
                            eventId, "ResumeVersionPublished", consumerGroup, executionCounter::incrementAndGet
                    );
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Start concurrent processing
        doneLatch.await();
        executor.shutdown();

        assertThat(executionCounter.get()).isEqualTo(1);
        assertThat(processedEventRepository.findByEventIdAndConsumerGroup(eventId, consumerGroup)).isPresent();
    }

    @Test
    void consumerRestart_ignoresCommittedEventOnRedelivery() {
        String eventId = UUID.randomUUID().toString();
        String consumerGroup = "restart-test-group";
        AtomicInteger executionCounter = new AtomicInteger(0);

        // Simulate initial successful run
        idempotencyService.executeIdempotent(eventId, "UserRegistered", consumerGroup, executionCounter::incrementAndGet);
        assertThat(executionCounter.get()).isEqualTo(1);

        // Simulate consumer restart and redelivery
        boolean isAlreadyProcessed = idempotencyService.isProcessed(eventId, consumerGroup);
        assertThat(isAlreadyProcessed).isTrue();

        EventProcessingResult<Integer> redeliveryResult = idempotencyService.executeIdempotent(
                eventId, "UserRegistered", consumerGroup, executionCounter::incrementAndGet
        );
        assertThat(redeliveryResult.isDuplicate()).isTrue();
        assertThat(executionCounter.get()).isEqualTo(1);
    }

    @Test
    void consumerGroupIsolation_allowsSameEventIdForDifferentConsumerGroups() {
        String eventId = UUID.randomUUID().toString();
        String groupA = "analytics-group";
        String groupB = "notification-group";
        AtomicInteger counterA = new AtomicInteger(0);
        AtomicInteger counterB = new AtomicInteger(0);

        EventProcessingResult<Integer> resA = idempotencyService.executeIdempotent(
                eventId, "ResumeVersionPublished", groupA, counterA::incrementAndGet
        );
        EventProcessingResult<Integer> resB = idempotencyService.executeIdempotent(
                eventId, "ResumeVersionPublished", groupB, counterB::incrementAndGet
        );

        assertThat(resA.isProcessed()).isTrue();
        assertThat(resB.isProcessed()).isTrue();
        assertThat(counterA.get()).isEqualTo(1);
        assertThat(counterB.get()).isEqualTo(1);

        assertThat(processedEventRepository.findByEventIdAndConsumerGroup(eventId, groupA)).isPresent();
        assertThat(processedEventRepository.findByEventIdAndConsumerGroup(eventId, groupB)).isPresent();
    }
}
