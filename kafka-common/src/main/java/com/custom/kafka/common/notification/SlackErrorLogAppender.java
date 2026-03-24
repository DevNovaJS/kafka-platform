package com.custom.kafka.common.notification;

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
import static ch.qos.logback.classic.Level.ERROR;

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
        if (!ERROR.equals(event.getLevel())) {
            return;
        }
        if (webhookUrl.isBlank()) {
            return;
        }

        try {
            String json = buildBlockKit(
                    event.getLoggerName(),
                    event.getThreadName(),
                    SLACK_TIME_FORMAT.format(Instant.ofEpochMilli(event.getTimeStamp())),
                    event.getFormattedMessage(),
                    extractStackTrace(event)
            );

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            addError("Slack 발송 실패: " + e.getMessage());
        }
    }

    private String buildBlockKit(String loggerName, String threadName,
                                 String timestamp, String message, String stackTrace) {
        var sb = new StringBuilder();

        sb.append("""
                {"blocks":[{"type":"header","text":{"type":"plain_text","text":":x: ERROR Log Alert"}},\
                {"type":"section","fields":[\
                {"type":"mrkdwn","text":"*Logger:*\\n%s"},\
                {"type":"mrkdwn","text":"*Thread:*\\n%s"},\
                {"type":"mrkdwn","text":"*Time:*\\n%s"}]}""".formatted(
                escape(loggerName), escape(threadName), escape(timestamp)));

        if (message != null && !message.isBlank()) {
            sb.append("""
                    ,{"type":"section","text":{"type":"mrkdwn","text":"*Message:*\\n```%s```"}}""".formatted(
                    escape(truncate(message))));
        }

        if (stackTrace != null && !stackTrace.isBlank()) {
            sb.append("""
                    ,{"type":"section","text":{"type":"mrkdwn","text":"*Stacktrace:*\\n```%s```"}}""".formatted(
                    escape(truncate(stackTrace))));
        }

        sb.append("]}");
        return sb.toString();
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

    private static String escape(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "    ");
    }

    private static String truncate(String text) {
        int maxLength = 2500;

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "…";
    }
}
