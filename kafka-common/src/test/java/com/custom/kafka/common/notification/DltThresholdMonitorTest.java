package com.custom.kafka.common.notification;

import com.custom.kafka.common.history.MessageHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private MessageHistoryService historyService;

    @Mock
    private SlackNotifier slackNotifier;

    @InjectMocks
    private DltThresholdMonitor monitor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(monitor, "windowMinutes", 60L);
        ReflectionTestUtils.setField(monitor, "maxCount", 10L);
    }

    @Test
    void checkThreshold_belowMax_noAlert() {
        when(historyService.countFailedByTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 5L));

        monitor.checkThreshold();

        verify(slackNotifier, never()).sendDltThresholdAlert(anyString(), anyLong(), anyLong());
    }

    @Test
    void checkThreshold_aboveMax_sendsAlert() {
        when(historyService.countFailedByTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 15L));

        monitor.checkThreshold();

        verify(slackNotifier).sendDltThresholdAlert(eq("order"), eq(15L), eq(60L));
    }

    @Test
    void checkThreshold_multipleTopics_alertsOnlyExceeding() {
        when(historyService.countFailedByTopicAfter(any(Instant.class)))
                .thenReturn(Map.of("order", 15L, "payment", 3L));

        monitor.checkThreshold();

        verify(slackNotifier).sendDltThresholdAlert(eq("order"), eq(15L), eq(60L));
        verify(slackNotifier, never()).sendDltThresholdAlert(eq("payment"), anyLong(), anyLong());
    }

    @Test
    void checkThreshold_exception_doesNotPropagate() {
        when(historyService.countFailedByTopicAfter(any(Instant.class)))
                .thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> monitor.checkThreshold()).doesNotThrowAnyException();
        verifyNoInteractions(slackNotifier);
    }
}
