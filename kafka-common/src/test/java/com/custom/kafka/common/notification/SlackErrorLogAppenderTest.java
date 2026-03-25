package com.custom.kafka.common.notification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackErrorLogAppenderTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private SlackErrorLogAppender appender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appender, "webhookUrl", "https://hooks.slack.com/test");
        ReflectionTestUtils.setField(appender, "stacktraceLines", 5);
        ReflectionTestUtils.setField(appender, "rateLimitSeconds", 60);
        appender.register();
    }

    @Test
    void firstError_sendsToSlack() {
        stubRestClientChain();

        appender.doAppend(mockEvent("com.example.Foo", new RuntimeException("boom")));

        verify(restClient, times(1)).post();
    }

    @Test
    void duplicateErrorWithinWindow_suppressed() {
        stubRestClientChain();

        appender.doAppend(mockEvent("com.example.Foo", new RuntimeException("boom1")));
        appender.doAppend(mockEvent("com.example.Foo", new RuntimeException("boom2")));

        verify(restClient, times(1)).post();
    }

    @Test
    void differentException_sendsSeparately() {
        stubRestClientChain();

        appender.doAppend(mockEvent("com.example.Foo", new RuntimeException("boom")));
        appender.doAppend(mockEvent("com.example.Foo", new IllegalArgumentException("bad")));

        verify(restClient, times(2)).post();
    }

    @Test
    void errorWithoutException_rateLimited() {
        stubRestClientChain();

        appender.doAppend(mockEvent("com.example.Bar", null));
        appender.doAppend(mockEvent("com.example.Bar", null));

        verify(restClient, times(1)).post();
    }

    @Test
    void blankWebhookUrl_skips() {
        ReflectionTestUtils.setField(appender, "webhookUrl", "");

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.ERROR);

        appender.doAppend(event);

        verifyNoInteractions(restClient);
    }

    @Test
    void differentLogger_sendsSeparately() {
        stubRestClientChain();

        appender.doAppend(mockEvent("com.example.Foo", new RuntimeException("boom")));
        appender.doAppend(mockEvent("com.example.Bar", new RuntimeException("boom")));

        verify(restClient, times(2)).post();
    }

    private ILoggingEvent mockEvent(String loggerName, Throwable throwable) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getLoggerName()).thenReturn(loggerName);
        when(event.getThreadName()).thenReturn("main");
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getFormattedMessage()).thenReturn("test error message");

        if (throwable != null) {
            IThrowableProxy tp = mock(IThrowableProxy.class);
            when(tp.getClassName()).thenReturn(throwable.getClass().getName());
            when(tp.getMessage()).thenReturn(throwable.getMessage());
            when(tp.getStackTraceElementProxyArray()).thenReturn(new StackTraceElementProxy[0]);
            when(event.getThrowableProxy()).thenReturn(tp);
        }

        return event;
    }

    private void stubRestClientChain() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);
    }
}
