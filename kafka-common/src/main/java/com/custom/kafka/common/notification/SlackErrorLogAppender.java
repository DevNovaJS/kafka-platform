package com.custom.kafka.common.notification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static com.custom.kafka.common.message.CommonConstants.SLACK_TIME_FORMAT;

@Component
@RequiredArgsConstructor
public class SlackErrorLogAppender extends AppenderBase<ILoggingEvent> {
    private final RestClient restClient;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;
    @Value("${slack.error-log.stacktrace-lines:5}")
    private int stacktraceLines;

    @PostConstruct
    void register() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        setName("SLACK_ERROR");
        setContext(context);
        start();

        rootLogger.addAppender(this);
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!Level.ERROR.equals(event.getLevel())) {
            return;
        }

        if (webhookUrl.isBlank()) {
            addError("Webhook URL is blank");
            return;
        }

        try {
            String body = SlackBlockKitBuilder.build(
                    ":x: ERROR Log Alert",
                    new String[][]{
                            {"Logger", event.getLoggerName()},
                            {"Thread", event.getThreadName()},
                            {"Time", SLACK_TIME_FORMAT.format(Instant.ofEpochMilli(event.getTimeStamp()))}
                    },
                    event.getFormattedMessage(),
                    extractStackTrace(event)
            );

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            addError("Slack 발송 실패: " + e.getMessage());
        }
    }


    private String extractStackTrace(ILoggingEvent event) {
        IThrowableProxy tp = event.getThrowableProxy();

        if (tp == null) {
            return null;
        }

        var sb = new StringBuilder();
        sb.append(tp.getClassName()).append(": ").append(tp.getMessage()).append('\n');

        StackTraceElementProxy[] frames = tp.getStackTraceElementProxyArray();
        int limit = Math.min(frames.length, stacktraceLines);

        for (int i = 0; i < limit; i++) {
            sb.append("    at ").append(frames[i].getSTEAsString()).append('\n');
        }

        if (frames.length > limit) {
            sb.append("    ... ").append(frames.length - limit).append(" more\n");
        }

        return sb.toString();
    }
}
