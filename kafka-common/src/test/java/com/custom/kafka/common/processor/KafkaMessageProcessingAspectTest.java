package com.custom.kafka.common.processor;

import com.custom.kafka.common.dlt.DltSender;
import com.custom.kafka.common.history.MessageHistory;
import com.custom.kafka.common.history.MessageHistoryRepository;
import com.custom.kafka.common.history.MessageHistoryStatus;
import com.custom.kafka.common.notification.SlackNotifier;
import com.custom.kafka.common.registry.MetadataRegistryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageProcessingAspectTest {

    @Mock
    private MessageHistoryRepository messageHistoryRepository;

    @Mock
    private DltSender dltSender;

    @Mock
    private SlackNotifier slackNotifier;

    @Mock
    private MetadataRegistryService metadataRegistryService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void duplicateMessage_savesSkippedAndReturnsNull() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper, metadataRegistryService
        );
        ConsumerRecord<String, String> record = createRecord("key-1", "id-1", 0,
                "{\"eventKey\":\"key-1\",\"eventId\":\"id-1\",\"payload\":{}}");

        when(messageHistoryRepository.existsByEventKeyAndEventIdAndFailCount("key-1", "id-1", 0)).thenReturn(true);

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isNull();
        verify(joinPoint, never()).proceed();

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(MessageHistoryStatus.SKIPPED);
        assertThat(captor.getValue().eventKey()).isEqualTo("key-1");
        assertThat(captor.getValue().eventId()).isEqualTo("id-1");

        verify(metadataRegistryService).registerIfNew("unknown", "unknown");
    }

    @Test
    void successfulProcessing_savesSuccessStatus() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper, metadataRegistryService
        );
        ConsumerRecord<String, String> record = createRecord("key-2", "id-2", 0,
                "{\"eventKey\":\"key-2\",\"eventId\":\"id-2\",\"payload\":{}}");

        when(messageHistoryRepository.existsByEventKeyAndEventIdAndFailCount("key-2", "id-2", 0)).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("ok");

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isEqualTo("ok");

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(MessageHistoryStatus.SUCCESS);
        assertThat(captor.getValue().eventKey()).isEqualTo("key-2");
        assertThat(captor.getValue().eventId()).isEqualTo("id-2");

        verify(metadataRegistryService).registerIfNew("unknown", "unknown");
    }

    @Test
    void successfulProcessing_withServiceNameAndDomain_registersMetadata() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper, metadataRegistryService
        );
        ConsumerRecord<String, String> record = createRecordWithMetadata("key-5", "id-5", 0,
                "{\"eventKey\":\"key-5\",\"eventId\":\"id-5\",\"payload\":{}}",
                "order-service", "commerce");

        when(messageHistoryRepository.existsByEventKeyAndEventIdAndFailCount("key-5", "id-5", 0)).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("ok");

        // when
        aspect.around(joinPoint, record);

        // then
        verify(metadataRegistryService).registerIfNew("order-service", "commerce");

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().serviceName()).isEqualTo("order-service");
        assertThat(captor.getValue().domain()).isEqualTo("commerce");
    }

    @Test
    void failedProcessing_savesFailedAndSendsDltAndNotifiesSlack() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper, metadataRegistryService
        );
        ConsumerRecord<String, String> record = createRecord("key-3", "id-3", 1,
                "{\"eventKey\":\"key-3\",\"eventId\":\"id-3\",\"payload\":{}}");
        RuntimeException error = new RuntimeException("processing error");

        when(messageHistoryRepository.existsByEventKeyAndEventIdAndFailCount("key-3", "id-3", 1)).thenReturn(false);
        when(joinPoint.proceed()).thenThrow(error);

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isNull();

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(MessageHistoryStatus.FAILED);
        assertThat(captor.getValue().errorMessage()).isEqualTo("processing error");

        verify(dltSender).send("key-3", "id-3", 1, record);
        verify(slackNotifier).sendError(eq("key-3"), eq("id-3"), eq(1), eq(record), eq(error));
        verify(metadataRegistryService).registerIfNew("unknown", "unknown");
    }

    @Test
    void invalidJson_skipsAllProcessing() throws Throwable {
        // given — invalid JSON payload, eventKey/eventId extraction fails
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper, metadataRegistryService
        );
        ConsumerRecord<String, String> record = createRecord(null, null, 0, "invalid-json");

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isNull();
        verifyNoInteractions(messageHistoryRepository);
        verify(dltSender, never()).send(anyString(), anyString(), anyInt(), any());
        verify(slackNotifier, never()).sendError(anyString(), anyString(), anyInt(), any(), any());
    }

    @Test
    void nullEventKey_skipsAllProcessing() throws Throwable {
        // given — JSON is valid but eventKey is null
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper, metadataRegistryService
        );
        ConsumerRecord<String, String> record = createRecord(null, null, 0,
                "{\"eventKey\":null,\"eventId\":null,\"payload\":{}}");

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isNull();
        verify(joinPoint, never()).proceed();
        verifyNoInteractions(messageHistoryRepository);
        verify(dltSender, never()).send(anyString(), anyString(), anyInt(), any());
        verify(slackNotifier, never()).sendError(anyString(), anyString(), anyInt(), any(), any());
    }

    @Test
    void withFailCount_extractsFromHeader() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper, metadataRegistryService
        );
        ConsumerRecord<String, String> record = createRecord("key-4", "id-4", 2,
                "{\"eventKey\":\"key-4\",\"eventId\":\"id-4\",\"payload\":{}}");

        when(messageHistoryRepository.existsByEventKeyAndEventIdAndFailCount("key-4", "id-4", 2)).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("ok");

        // when
        aspect.around(joinPoint, record);

        // then
        verify(messageHistoryRepository).existsByEventKeyAndEventIdAndFailCount("key-4", "id-4", 2);

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().failCount()).isEqualTo(2);
    }

    private ConsumerRecord<String, String> createRecord(String eventKey, String eventId, int failCount, String payload) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("test-topic", 0, 0L, "key", payload);
        if (eventKey != null) {
            record.headers().add(new RecordHeader("X-Event-Key", eventKey.getBytes(StandardCharsets.UTF_8)));
        }
        if (eventId != null) {
            record.headers().add(new RecordHeader("X-Event-Id", eventId.getBytes(StandardCharsets.UTF_8)));
        }
        if (failCount > 0) {
            record.headers().add(new RecordHeader("X-Fail-Count",
                    String.valueOf(failCount).getBytes(StandardCharsets.UTF_8)));
        }
        return record;
    }

    private ConsumerRecord<String, String> createRecordWithMetadata(
            String eventKey, String eventId, int failCount, String payload,
            String serviceName, String domain
    ) {
        ConsumerRecord<String, String> record = createRecord(eventKey, eventId, failCount, payload);
        record.headers().add(new RecordHeader("X-Service-Name", serviceName.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("X-Domain", domain.getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
