package com.custom.kafka.common.notification;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlackBlockKitBuilder {
    private static final int MAX_TEXT_LENGTH = 2500;

    public static String build(String header, String[][] fields, String message, String stackTrace) {
        var sb = new StringBuilder();
        sb.append("""
                {"blocks":[{"type":"header","text":{"type":"plain_text","text":"%s"}},\
                {"type":"section","fields":[""".formatted(escape(header)));

        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }

            sb.append("""
                    {"type":"mrkdwn","text":"*%s:*\\n%s"}""".formatted(
                    escape(fields[i][0]), escape(fields[i][1])));
        }
        sb.append("]}");

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
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }

        return text.substring(0, MAX_TEXT_LENGTH) + "…";
    }
}
