package com.custom.kafka.common.notification;

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
class SlackNotifierTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private SlackNotifier slackNotifier;

    @Test
    void sendDltThresholdAlert_blankUrl_skips() {
        ReflectionTestUtils.setField(slackNotifier, "webhookUrl", "");

        slackNotifier.sendDltThresholdAlert("order", 15, 60);

        verifyNoInteractions(restClient);
    }

    @Test
    void sendDltThresholdAlert_withUrl_sendsPost() {
        ReflectionTestUtils.setField(slackNotifier, "webhookUrl", "https://hooks.slack.com/test");
        stubRestClientChain();

        slackNotifier.sendDltThresholdAlert("order", 15, 60);

        verify(restClient).post();
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
