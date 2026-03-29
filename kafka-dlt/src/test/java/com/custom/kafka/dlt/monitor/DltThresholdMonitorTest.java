package com.custom.kafka.dlt.monitor;

import com.custom.kafka.common.notification.SlackNotifier;
import com.custom.kafka.dlt.config.DltThresholdProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DltThresholdMonitorTest {

    @Mock
    private DltCountService dltCountService;

    @Mock
    private SlackNotifier slackNotifier;

    @Mock
    private DltThresholdProperties properties;

    @InjectMocks
    private DltThresholdMonitor monitor;

    @BeforeEach
    void setUp() {
        when(properties.windowMinutes()).thenReturn(60L);
        when(properties.resolveMaxCount(anyString())).thenReturn(10L);
    }

    @Test
    void checkThreshold_belowMax_noAlert() {
        when(dltCountService.countByOriginalTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 5L));

        monitor.checkThreshold();

        verifyNoInteractions(slackNotifier);
    }

    @Test
    void checkThreshold_atThreshold_sendsAlert() {
        when(dltCountService.countByOriginalTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 10L));

        monitor.checkThreshold();

        verify(slackNotifier).sendDltThresholdAlert(eq("order"), eq(10L), eq(60L), eq(10L));
    }

    @Test
    void checkThreshold_aboveMax_sendsAlert() {
        when(dltCountService.countByOriginalTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 15L));

        monitor.checkThreshold();

        verify(slackNotifier).sendDltThresholdAlert(eq("order"), eq(15L), eq(60L), eq(10L));
    }

    @Test
    void checkThreshold_multipleTopics_alertsOnlyExceeding() {
        when(dltCountService.countByOriginalTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 15L, "payment", 3L));

        monitor.checkThreshold();

        verify(slackNotifier).sendDltThresholdAlert(eq("order"), eq(15L), eq(60L), eq(10L));
        verify(slackNotifier, never()).sendDltThresholdAlert(eq("payment"), anyLong(), anyLong(), anyLong());
    }

    @Test
    void checkThreshold_topicCustomMaxCount_usesTopicThreshold() {
        when(properties.resolveMaxCount("order")).thenReturn(3L);
        when(dltCountService.countByOriginalTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 5L));

        monitor.checkThreshold();

        verify(slackNotifier).sendDltThresholdAlert(eq("order"), eq(5L), eq(60L), eq(3L));
    }

    @Test
    void checkThreshold_topicCustomMaxCount_belowTopicThreshold_noAlert() {
        when(properties.resolveMaxCount("order")).thenReturn(20L);
        when(dltCountService.countByOriginalTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 15L));

        monitor.checkThreshold();

        verifyNoInteractions(slackNotifier);
    }

    @Test
    void checkThreshold_exception_doesNotPropagate() {
        when(dltCountService.countByOriginalTopicAfter(any(Instant.class)))
                .thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> monitor.checkThreshold()).doesNotThrowAnyException();
        verifyNoInteractions(slackNotifier);
    }
}
